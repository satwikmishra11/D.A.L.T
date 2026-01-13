// ========== src/worker.rs ==========
use crate::config::Config;
use crate::http_client::HttpClientManager;
use crate::metrics::MetricsCollector;
use crate::models::{WorkerTask, WorkerResult, WorkerHeartbeat, WorkerStatus};
use anyhow::Result;
use redis::AsyncCommands;
use std::sync::{Arc, atomic::{AtomicBool, Ordering}};
use std::time::Duration;
use tokio::sync::Semaphore;
use tokio::time::sleep;
use tracing::{info, warn, error, debug};
use uuid::Uuid;
use chrono::Utc;

use crate::circuit_breaker::CircuitBreaker;
use crate::rate_limiter;
use crate::tenant;

static SHUTDOWN_FLAG: AtomicBool = AtomicBool::new(false);

pub fn stop_accepting() {
    SHUTDOWN_FLAG.store(true, Ordering::SeqCst);
    info!("Worker received shutdown signal, stopping acceptance...");
}

pub fn is_shutting_down() -> bool {
    SHUTDOWN_FLAG.load(Ordering::SeqCst)
}

pub struct Worker {
    worker_id: String,
    config: Config,
    redis_client: redis::Client,
    http_client: Arc<HttpClientManager>,
    metrics_collector: Arc<MetricsCollector>,
    requests_processed: Arc<std::sync::atomic::AtomicU64>,
    active_tasks: Arc<std::sync::atomic::AtomicUsize>,
}

impl Worker {
    pub async fn new(config: Config) -> Result<Self> {
        let worker_id = Uuid::new_v4().to_string();
        let redis_client = redis::Client::open(config.redis_url.as_str())?;
        let http_client = Arc::new(HttpClientManager::new(config.clone())?);
        let metrics_collector = Arc::new(MetricsCollector::new());
        let requests_processed = Arc::new(std::sync::atomic::AtomicU64::new(0));
        let active_tasks = Arc::new(std::sync::atomic::AtomicUsize::new(0));
        
        info!("Worker {} initialized", worker_id);
        
        Ok(Self {
            worker_id,
            config,
            redis_client,
            http_client,
            metrics_collector,
            requests_processed,
            active_tasks,
        })
    }
    
    pub fn worker_id(&self) -> &str {
        &self.worker_id
    }
    
    pub async fn run(&self) -> Result<()> {
        info!("Worker {} starting main loop", self.worker_id);
        
        // Start heartbeat task
        let heartbeat_handle = self.start_heartbeat();
        
        // Main work loop
        while !is_shutting_down() {
            match self.process_task().await {
                Ok(true) => {
                    debug!("Task processed successfully");
                }
                Ok(false) => {
                    // No task available, wait a bit
                    debug!("No tasks available, waiting...");
                    sleep(Duration::from_secs(1)).await;
                }
                Err(e) => {
                    error!("Error processing task: {}", e);
                    sleep(Duration::from_secs(5)).await;
                }
            }
        }
        
        info!("Worker {} shutdown complete", self.worker_id);
        // Abort heartbeat on exit
        heartbeat_handle.abort();
        Ok(())
    }
    
    fn start_heartbeat(&self) -> tokio::task::JoinHandle<()> {
        let worker_id = self.worker_id.clone();
        let redis_client = self.redis_client.clone();
        let requests_processed = Arc::clone(&self.requests_processed);
        let active_tasks = Arc::clone(&self.active_tasks);
        let heartbeat_interval = self.config.heartbeat_interval_secs;
        let heartbeat_key = self.config.heartbeat_key.clone();
        
        tokio::spawn(async move {
            loop {
                match Self::send_heartbeat_static(
                    &worker_id,
                    &redis_client,
                    &requests_processed,
                    &active_tasks,
                    &heartbeat_key,
                ).await {
                    Ok(_) => debug!("Heartbeat sent"),
                    Err(e) => error!("Failed to send heartbeat: {}", e),
                }
                
                sleep(Duration::from_secs(heartbeat_interval)).await;
            }
        })
    }
    
    async fn send_heartbeat_static(
        worker_id: &str,
        redis_client: &redis::Client,
        requests_processed: &Arc<std::sync::atomic::AtomicU64>,
        active_tasks: &Arc<std::sync::atomic::AtomicUsize>,
        heartbeat_key: &str,
    ) -> Result<()> {
        let mut conn = redis_client.get_async_connection().await?;
        
        let heartbeat = WorkerHeartbeat {
            worker_id: worker_id.to_string(),
            timestamp: Utc::now(),
            status: if is_shutting_down() { WorkerStatus::Draining } else { WorkerStatus::Idle },
            current_task_id: None,
            requests_processed: requests_processed.load(Ordering::Relaxed),
            cpu: 0.0, // TODO: Implement actual CPU monitoring
            memory_mb: 0, // TODO: Implement actual memory monitoring
            active_tasks: active_tasks.load(Ordering::Relaxed),
        };
        
        let key = format!("{}:{}", heartbeat_key, worker_id);
        let json = serde_json::to_string(&heartbeat)?;
        
        conn.set_ex(&key, json, 60).await?;
        
        Ok(())
    }
    
    async fn process_task(&self) -> Result<bool> {
        let mut conn = self.redis_client.get_async_connection().await?;
        
        // Try to pop a task from the queue
        let task_json: Option<String> = conn.lpop(&self.config.task_queue, None).await?;
        
        if task_json.is_none() {
            return Ok(false);
        }
        
        let task: WorkerTask = serde_json::from_str(&task_json.unwrap())?;
        info!("Picked up task: {} (RPS: {}, Duration: {}s)", 
            task.task_id, task.rps, task.duration_seconds);
            
        // Set tenant context
        tenant::set_org(task.org.clone());
        
        self.active_tasks.fetch_add(1, Ordering::SeqCst);
        
        // Execute the load test
        let results = self.execute_load_test(task).await?;
        
        self.active_tasks.fetch_sub(1, Ordering::SeqCst);
        
        // Send results back
        self.send_results(results).await?;
        
        Ok(true)
    }
    
    async fn execute_load_test(&self, task: WorkerTask) -> Result<Vec<WorkerResult>> {
        let duration = Duration::from_secs(task.duration_seconds as u64);
        let interval = Duration::from_secs_f64(1.0 / task.rps as f64);
        
        info!("Starting load test: {} RPS for {} seconds", task.rps, task.duration_seconds);
        
        let start_time = std::time::Instant::now();
        let mut results = Vec::new();
        
        // Rate limiter
        let semaphore = Arc::new(Semaphore::new(task.rps as usize));
        
        // Metrics collector for this task
        self.metrics_collector.reset();
        
        let mut tick_interval = tokio::time::interval(interval);
        
        while start_time.elapsed() < duration {
            if is_shutting_down() {
                break;
            }
            
            tick_interval.tick().await;
            
            let permit = match semaphore.clone().try_acquire_owned() {
                Ok(p) => p,
                Err(_) => continue, // Skip if we're at the rate limit
            };
            
            let http_client = Arc::clone(&self.http_client);
            let task_clone = task.clone();
            let metrics_collector = Arc::clone(&self.metrics_collector);
            let requests_processed = Arc::clone(&self.requests_processed);
            
            tokio::spawn(async move {
                let metric = http_client.execute_request(&task_clone).await;
                
                metrics_collector.record(&metric);
                requests_processed.fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                
                // Adjust rate limiter
                rate_limiter::adjust(metric.latency_ms);
                
                drop(permit);
            });
            
            // Collect and report stats periodically
            if self.metrics_collector.total_count() >= self.config.batch_report_size {
                let batch_result = self.create_batch_result(&task);
                results.push(batch_result);
                self.metrics_collector.reset();
            }
        }
        
        // Wait for remaining requests to complete (simple delay)
        sleep(Duration::from_secs(2)).await;
        
        // Report remaining stats
        if self.metrics_collector.total_count() > 0 {
            let batch_result = self.create_batch_result(&task);
            results.push(batch_result);
        }
        
        let total_requests: u32 = results.iter().map(|r| r.total_requests).sum();
        info!("Completed load test: {} total requests", total_requests);
        
        Ok(results)
    }
    
    fn create_batch_result(&self, task: &WorkerTask) -> WorkerResult {
        let (total, success, errors, avg, p95, p99) = self.metrics_collector.get_stats();
        
        WorkerResult {
            task_id: task.task_id.clone(),
            worker_id: self.worker_id.clone(),
            timestamp: Utc::now(),
            latency_ms: None,
            status_code: None,
            success: success > 0,
            error: None,
            total_requests: total,
            success_count: success,
            error_count: errors,
            avg_latency_ms: avg,
            p95_latency_ms: p95,
            p99_latency_ms: p99,
        }
    }
    
    async fn send_results(&self, results: Vec<WorkerResult>) -> Result<()> {
        let mut conn = self.redis_client.get_async_connection().await?;
        
        for result in results {
            let json = serde_json::to_string(&result)?;
            conn.rpush(&self.config.result_queue, json).await?;
        }
        
        Ok(())
    }
}
