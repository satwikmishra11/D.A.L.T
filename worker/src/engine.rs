use std::sync::Arc;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::RwLock;
use std::time::{Duration, Instant};
use anyhow::Result;
use tokio::sync::Mutex;
use tracing::{info, warn};
use governor::{Quota, RateLimiter};
use std::num::NonZeroU32;

use crate::models::{WorkerTask, WorkerResult, HttpMethod, ProfileType};
use crate::clients::http::HttpClient;
use crate::metrics::MetricsCollector;

pub struct TaskExecutor {
    http_client: HttpClient,
    worker_id: String,
}

impl TaskExecutor {
    pub fn new(http_client: HttpClient, worker_id: String) -> Self {
        Self { http_client, worker_id }
    }

    pub async fn execute(
        &self, 
        task: WorkerTask, 
        progress_callback: impl Fn(WorkerResult) + Send + Sync + 'static,
        is_cancelled: Arc<AtomicBool>
    ) -> Result<()> {
        let initial_target_rps = get_target_rps(&task, 0);
        info!(
            "Starting execution for task {} with initial target RPS: {}", 
            task.task_id, 
            initial_target_rps
        );

        let rps = NonZeroU32::new(initial_target_rps).unwrap_or(NonZeroU32::new(1).unwrap());
        let quota = Quota::per_second(rps);
        let initial_limiter = Arc::new(RateLimiter::direct(quota));
        
        let limiter_lock = Arc::new(RwLock::new(initial_limiter));
        
        // Shared metrics across threads
        let metrics = Arc::new(Mutex::new(MetricsCollector::new()));
        let start_time = Instant::now();
        let duration = Duration::from_secs(task.duration_seconds as u64);
        
        let client = self.http_client.clone();
        let task = Arc::new(task);

        // Reporting loop
        let metrics_clone = metrics.clone();
        let task_id_clone = task.task_id.clone();
        let worker_id_clone = self.worker_id.clone();
        let callback_clone = Arc::new(progress_callback);
        
        let reporter_handle = tokio::spawn(async move {
            let mut interval = tokio::time::interval(Duration::from_secs(1));
            loop {
                interval.tick().await;
                let mut m = metrics_clone.lock().await;
                let stats = m.get_stats();
                
                if stats.count > 0 {
                    let result = WorkerResult {
                        task_id: task_id_clone.clone(),
                        worker_id: worker_id_clone.clone(),
                        timestamp: chrono::Utc::now(),
                        success: true,
                        total_requests: stats.count,
                        success_count: stats.success,
                        error_count: stats.error,
                        avg_latency_ms: stats.mean / 1000.0, // us to ms
                        p50_latency_ms: stats.p50 as f64 / 1000.0,
                        p90_latency_ms: stats.p90 as f64 / 1000.0,
                        p95_latency_ms: stats.p95 as f64 / 1000.0,
                        p99_latency_ms: stats.p99 as f64 / 1000.0,
                        max_latency_ms: stats.max as f64 / 1000.0,
                        actual_rps: stats.count as f64, // Approximate per second since we reset
                        status_codes: stats.status_codes,
                        error_types: stats.error_types,
                        error_msg: None,
                    };
                    
                    callback_clone(result);
                    m.reset();
                }
            }
        });

        // Dynamic rate profile updater loop
        let limiter_lock_clone = limiter_lock.clone();
        let task_clone = task.clone();
        let start_time_clone = start_time.clone();
        let mut last_rps = initial_target_rps;

        let updater_handle = tokio::spawn(async move {
            let mut interval = tokio::time::interval(Duration::from_millis(200));
            loop {
                interval.tick().await;
                let elapsed = start_time_clone.elapsed().as_secs() as u32;
                let current_target_rps = get_target_rps(&task_clone, elapsed);
                if current_target_rps != last_rps {
                    info!("Updating target RPS from {} to {}", last_rps, current_target_rps);
                    let rps = NonZeroU32::new(current_target_rps).unwrap_or(NonZeroU32::new(1).unwrap());
                    let quota = Quota::per_second(rps);
                    let new_limiter = Arc::new(RateLimiter::direct(quota));
                    
                    if let Ok(mut guard) = limiter_lock_clone.write() {
                        *guard = new_limiter;
                    }
                    last_rps = current_target_rps;
                }
            }
        });

        // Load Generation Loop
        // We spawn distinct tasks up to RPS to ensure parallelism, but use the limiter to control rate.
        let concurrency = std::cmp::min(5000, std::cmp::max(50, task.rps / 10));
        let mut handles = Vec::new();
        
        info!("Calculated concurrency: {} for RPS: {}", concurrency, task.rps);

        for _ in 0..concurrency {
            let limiter_lock_ref = limiter_lock.clone();
            let client = client.clone();
            let task = task.clone();
            let metrics = metrics.clone();
            
            let is_cancelled_clone = is_cancelled.clone();
            handles.push(tokio::spawn(async move {
                while start_time.elapsed() < duration && !is_cancelled_clone.load(Ordering::Relaxed) {
                    // Wait for permission from the active limiter
                    let active_limiter = {
                        let guard = limiter_lock_ref.read().unwrap();
                        guard.clone()
                    };
                    active_limiter.until_ready().await;
                    
                    let req_start = Instant::now();
                    let response = Self::send_request(&client, &task).await;
                    let latency_us = req_start.elapsed().as_micros() as u64;
                    
                    let mut m = metrics.lock().await;
                    match response {
                        Ok(status) => {
                            if status >= 200 && status < 400 {
                                m.record_success(latency_us, status);
                            } else {
                                m.record_error(Some(status), None);
                            }
                        }
                        Err(e) => {
                            let err_type = if e.is_timeout() {
                                "Timeout".to_string()
                            } else if e.is_connect() {
                                "ConnectionError".to_string()
                            } else if e.is_body() || e.is_decode() {
                                "BodyError".to_string()
                            } else {
                                "UnknownError".to_string()
                            };
                            m.record_error(None, Some(err_type));
                        }
                    }
                }
            }));
        }

        // Wait for all workers
        for h in handles {
            let _ = h.await;
        }
        
        reporter_handle.abort();
        updater_handle.abort();
        
        info!("Execution finished for task {}", task.task_id);
        
        Ok(())
    }

    async fn send_request(client: &HttpClient, task: &WorkerTask) -> Result<u16, reqwest::Error> {
        let mut req_builder = match task.method {
            HttpMethod::GET => client.client().get(&task.target_url),
            HttpMethod::POST => client.client().post(&task.target_url),
            HttpMethod::PUT => client.client().put(&task.target_url),
            HttpMethod::DELETE => client.client().delete(&task.target_url),
            _ => client.client().get(&task.target_url),
        };

        if let Some(headers) = &task.headers {
            for (k, v) in headers {
                req_builder = req_builder.header(k, v);
            }
        }

        let req_builder = if let Some(body) = &task.body {
            req_builder.body(body.clone())
        } else {
            req_builder
        };
        
        let req = if let Some(t) = task.timeout_seconds {
            req_builder.timeout(Duration::from_secs(t as u64))
        } else {
            req_builder
        };
        
        match req.send().await {
            Ok(res) => Ok(res.status().as_u16()),
            Err(e) => Err(e),
        }
    }
}

/// Computes the target RPS at a given second of execution based on the task's load profile.
pub fn get_target_rps(task: &WorkerTask, elapsed_seconds: u32) -> u32 {
    let load_profile = match &task.load_profile {
        Some(lp) => lp,
        None => return task.rps,
    };

    match load_profile.profile_type {
        ProfileType::CONSTANT => load_profile.target_rps,
        ProfileType::RampUp => {
            if load_profile.ramp_up_seconds == 0 {
                return load_profile.target_rps;
            }
            if elapsed_seconds >= load_profile.ramp_up_seconds {
                load_profile.target_rps
            } else {
                let diff = load_profile.target_rps as i32 - load_profile.initial_rps as i32;
                let progress = elapsed_seconds as f64 / load_profile.ramp_up_seconds as f64;
                (load_profile.initial_rps as f64 + diff as f64 * progress) as u32
            }
        }
        ProfileType::SPIKE => {
            let duration = task.duration_seconds;
            if duration == 0 {
                return load_profile.initial_rps;
            }
            let midpoint = duration / 2;
            if midpoint == 0 {
                return load_profile.target_rps;
            }
            if elapsed_seconds <= midpoint {
                let diff = load_profile.target_rps as i32 - load_profile.initial_rps as i32;
                let progress = elapsed_seconds as f64 / midpoint as f64;
                (load_profile.initial_rps as f64 + diff as f64 * progress) as u32
            } else if elapsed_seconds >= duration {
                load_profile.initial_rps
            } else {
                let diff = load_profile.target_rps as i32 - load_profile.initial_rps as i32;
                let progress = (elapsed_seconds - midpoint) as f64 / (duration - midpoint) as f64;
                (load_profile.target_rps as f64 - diff as f64 * progress) as u32
            }
        }
        ProfileType::BURST => {
            if let Some(bursts) = &load_profile.bursts {
                for burst in bursts {
                    let end_second = burst.start_second + burst.duration_seconds;
                    if elapsed_seconds >= burst.start_second && elapsed_seconds < end_second {
                        return burst.rps;
                    }
                }
            }
            load_profile.initial_rps
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::{HttpMethod, LoadProfile, BurstConfig};
    use std::collections::HashMap;

    fn create_test_task(load_profile: Option<LoadProfile>) -> WorkerTask {
        WorkerTask {
            task_id: "test-task".to_string(),
            execution_id: "test-exec".to_string(),
            target_url: "http://example.com".to_string(),
            method: HttpMethod::GET,
            headers: None,
            body: None,
            rps: 100,
            duration_seconds: 60,
            org_id: "test-org".to_string(),
            timeout_seconds: None,
            ignore_tls_errors: None,
            load_profile,
        }
    }

    #[test]
    fn test_get_target_rps_constant() {
        let profile = LoadProfile {
            profile_type: ProfileType::CONSTANT,
            initial_rps: 10,
            target_rps: 50,
            ramp_up_seconds: 10,
            bursts: None,
        };
        let task = create_test_task(Some(profile));
        
        assert_eq!(get_target_rps(&task, 0), 50);
        assert_eq!(get_target_rps(&task, 30), 50);
        assert_eq!(get_target_rps(&task, 60), 50);
    }

    #[test]
    fn test_get_target_rps_ramp_up() {
        let profile = LoadProfile {
            profile_type: ProfileType::RampUp,
            initial_rps: 10,
            target_rps: 50,
            ramp_up_seconds: 20,
            bursts: None,
        };
        let task = create_test_task(Some(profile));
        
        assert_eq!(get_target_rps(&task, 0), 10);
        assert_eq!(get_target_rps(&task, 10), 30);
        assert_eq!(get_target_rps(&task, 20), 50);
        assert_eq!(get_target_rps(&task, 30), 50);
    }

    #[test]
    fn test_get_target_rps_spike() {
        let profile = LoadProfile {
            profile_type: ProfileType::SPIKE,
            initial_rps: 10,
            target_rps: 50,
            ramp_up_seconds: 0,
            bursts: None,
        };
        let task = create_test_task(Some(profile));
        
        assert_eq!(get_target_rps(&task, 0), 10);
        assert_eq!(get_target_rps(&task, 15), 30);
        assert_eq!(get_target_rps(&task, 30), 50);
        assert_eq!(get_target_rps(&task, 45), 30);
        assert_eq!(get_target_rps(&task, 60), 10);
    }

    #[test]
    fn test_get_target_rps_burst() {
        let bursts = vec![
            BurstConfig {
                start_second: 10,
                duration_seconds: 5,
                rps: 200,
            },
            BurstConfig {
                start_second: 40,
                duration_seconds: 10,
                rps: 300,
            },
        ];
        let profile = LoadProfile {
            profile_type: ProfileType::BURST,
            initial_rps: 20,
            target_rps: 0,
            ramp_up_seconds: 0,
            bursts: Some(bursts),
        };
        let task = create_test_task(Some(profile));
        
        assert_eq!(get_target_rps(&task, 0), 20);
        assert_eq!(get_target_rps(&task, 9), 20);
        assert_eq!(get_target_rps(&task, 10), 200);
        assert_eq!(get_target_rps(&task, 14), 200);
        assert_eq!(get_target_rps(&task, 15), 20);
        assert_eq!(get_target_rps(&task, 39), 20);
        assert_eq!(get_target_rps(&task, 40), 300);
        assert_eq!(get_target_rps(&task, 49), 300);
        assert_eq!(get_target_rps(&task, 50), 20);
    }
}
