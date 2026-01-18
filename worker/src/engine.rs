use std::sync::Arc;
use std::time::{Duration, Instant};
use anyhow::Result;
use tokio::sync::Mutex;
use tracing::{info, debug, error};
use governor::{Quota, RateLimiter, Jitter};
use governor::state::{InMemoryState, NotKeyed};
use governor::clock::QuantaClock;
use std::num::NonZeroU32;

use crate::models::{WorkerTask, WorkerResult, HttpMethod};
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
        progress_callback: impl Fn(WorkerResult) + Send + Sync + 'static
    ) -> Result<WorkerResult> {
        info!("Starting execution for task {} with {} RPS", task.task_id, task.rps);

        let rps = NonZeroU32::new(task.rps).unwrap_or(NonZeroU32::new(1).unwrap());
        let quota = Quota::per_second(rps);
        let limiter = Arc::new(RateLimiter::direct(quota));
        
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
                        error_msg: None,
                    };
                    
                    callback_clone(result);
                    m.reset();
                }
            }
        });

        // Load Generation Loop
        // We spawn distinct tasks up to RPS to ensure parallelism, but use the limiter to control rate.
        // Actually, spawning a task per request at high RPS (e.g. 10k) is bad.
        // Better: Spawn N workers (where N = max_concurrent_users or sensible number) and have them loop against the limiter.
        
        let concurrency = 50; // Use a reasonable default or calc from RPS/Latency
        let mut handles = Vec::new();

        for _ in 0..concurrency {
            let limiter = limiter.clone();
            let client = client.clone();
            let task = task.clone();
            let metrics = metrics.clone();
            
            handles.push(tokio::spawn(async move {
                while start_time.elapsed() < duration {
                    // Wait for permission
                    limiter.until_ready().await;
                    
                    let req_start = Instant::now();
                    let response = Self::send_request(&client, &task).await;
                    let latency_us = req_start.elapsed().as_micros() as u64;
                    
                    let mut m = metrics.lock().await;
                    match response {
                        Ok(success) => {
                            if success {
                                m.record_success(latency_us);
                            } else {
                                m.record_error();
                            }
                        }
                        Err(_) => m.record_error(),
                    }
                }
            }));
        }

        // Wait for all workers
        for h in handles {
            let _ = h.await;
        }
        
        reporter_handle.abort();
        
        info!("Execution finished for task {}", task.task_id);
        
        // Final aggregate (placeholder, in reality we stream results)
        Ok(WorkerResult {
             task_id: task.task_id.clone(),
             worker_id: self.worker_id.clone(),
             timestamp: chrono::Utc::now(),
             success: true,
             total_requests: 0, 
             success_count: 0,
             error_count: 0,
             avg_latency_ms: 0.0,
             p50_latency_ms: 0.0,
             p90_latency_ms: 0.0,
             p95_latency_ms: 0.0,
             p99_latency_ms: 0.0,
             max_latency_ms: 0.0,
             actual_rps: 0.0,
             error_msg: None,
        })
    }

    async fn send_request(client: &HttpClient, task: &WorkerTask) -> Result<bool> {
        let req_builder = match task.method {
            HttpMethod::GET => client.client().get(&task.target_url),
            HttpMethod::POST => client.client().post(&task.target_url),
            HttpMethod::PUT => client.client().put(&task.target_url),
            HttpMethod::DELETE => client.client().delete(&task.target_url),
            _ => client.client().get(&task.target_url),
        };

        let req = if let Some(body) = &task.body {
            req_builder.body(body.clone())
        } else {
            req_builder
        };
        
        // Headers would be added here

        match req.send().await {
            Ok(res) => Ok(res.status().is_success()),
            Err(_) => Ok(false),
        }
    }
}
