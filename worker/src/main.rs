mod models;

use models::*;
use anyhow::{Result, Context};
use redis::AsyncCommands;
use reqwest::Client;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::Semaphore;
use tokio::time::sleep;
use tracing::{info, warn, error, debug};
use uuid::Uuid;
use chrono::Utc;

const REDIS_URL: &str = "redis://127.0.0.1:6379";
const TASK_QUEUE: &str = "loadtest:tasks";
const RESULT_QUEUE: &str = "loadtest:results";
const HEARTBEAT_KEY: &str = "loadtest:heartbeat";
const HEARTBEAT_INTERVAL_SECS: u64 = 5;
const BATCH_REPORT_SIZE: u32 = 100;

struct Worker {
    worker_id: String,
    redis_client: redis::Client,
    http_client: Client,
    requests_processed: Arc<std::sync::atomic::AtomicU64>,
}

impl Worker {
    fn new() -> Result<Self> {
        let worker_id = Uuid::new_v4().to_string();
        let redis_client = redis::Client::open(REDIS_URL)?;
        
        let http_client = Client::builder()
            .timeout(Duration::from_secs(30))
            .pool_max_idle_per_host(100)
            .build()?;
        
        info!("Worker {} initialized", worker_id);
        
        Ok(Self {
            worker_id,
            redis_client,
            http_client,
            requests_processed: Arc::new(std::sync::atomic::AtomicU64::new(0)),
        })
    }
    
    async fn run(&self) -> Result<()> {
        info!("Worker {} starting", self.worker_id);
        
        // Start heartbeat task
        let heartbeat_handle = self.start_heartbeat();
        
        // Main work loop
        loop {
            match self.process_task().await {
                Ok(true) => {
                    debug!("Task processed successfully");
                }
                Ok(false) => {
                    // No task available, wait a bit
                    sleep(Duration::from_secs(1)).await;
                }
                Err(e) => {
                    error!("Error processing task: {}", e);
                    sleep(Duration::from_secs(5)).await;
                }
            }
        }
        
        // This is unreachable but required for type checking
        #[allow(unreachable_code)]
        {
            heartbeat_handle.abort();
            Ok(())
        }
    }
    
    fn start_heartbeat(&self) -> tokio::task::JoinHandle<()> {
        let worker_id = self.worker_id.clone();
        let redis_client = self.redis_client.clone();
        let requests_processed = Arc::clone(&self.requests_processed);
        
        tokio::spawn(async move {
            loop {
                match Self::send_heartbeat(
                    &worker_id,
                    &redis_client,
                    &requests_processed
                ).await {
                    Ok(_) => debug!("Heartbeat sent"),
                    Err(e) => error!("Failed to send heartbeat: {}", e),
                }
                
                sleep(Duration::from_secs(HEARTBEAT_INTERVAL_SECS)).await;
            }
        })
    }
    
    async fn send_heartbeat(
        worker_id: &str,
        redis_client: &redis::Client,
        requests_processed: &Arc<std::sync::atomic::AtomicU64>,
    ) -> Result<()> {
        let mut conn = redis_client.get_async_connection().await?;
        
        let heartbeat = WorkerHeartbeat {
            worker_id: worker_id.to_string(),
            timestamp: Utc::now(),
            status: WorkerStatus::Idle,
            current_task_id: None,
            requests_processed: requests_processed.load(std::sync::atomic::Ordering::Relaxed),
        };
        
        let key = format!("{}:{}", HEARTBEAT_KEY, worker_id);
        let json = serde_json::to_string(&heartbeat)?;
        
        conn.set_ex(&key, json, 60).await?;
        
        Ok(())
    }
    
    async fn process_task(&self) -> Result<bool> {
        let mut conn = self.redis_client.get_async_connection().await?;
        
        // Try to pop a task from the queue
        let task_json: Option<String> = conn.lpop(TASK_QUEUE, None).await?;
        
        if task_json.is_none() {
            return Ok(false);
        }
        
        let task: WorkerTask = serde_json::from_str(&task_json.unwrap())?;
        info!("Picked up task: {} (RPS: {}, Duration: {}s)", 
            task.task_id, task.rps, task.duration_seconds);
        
        // Execute the load test
        let result = self.execute_load_test(task).await?;
        
        // Send result back
        self.send_result(result).await?;
        
        Ok(true)
    }
    
    async fn execute_load_test(&self, task: WorkerTask) -> Result<Vec<WorkerResult>> {
        let duration = Duration::from_secs(task.duration_seconds as u64);
        let interval = Duration::from_secs_f64(1.0 / task.rps as f64);
        
        info!("Starting load test: {} RPS for {} seconds", task.rps, task.duration_seconds);
        
        let start_time = Instant::now();
        let mut results = Vec::new();
        let mut batch_stats = BatchStats::new();
        
        // Rate limiter
        let semaphore = Arc::new(Semaphore::new(task.rps as usize));
        
        while start_time.elapsed() < duration {
            let permit = semaphore.clone().acquire_owned().await?;
            
            let http_client = self.http_client.clone();
            let task_clone = task.clone();
            let worker_id = self.worker_id.clone();
            let requests_processed = Arc::clone(&self.requests_processed);
            
            tokio::spawn(async move {
                let metric = Self::execute_request(
                    &http_client,
                    &task_clone
                ).await;
                
                requests_processed.fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                drop(permit);
                
                metric
            });
            
            sleep(interval).await;
            
            // Collect and report stats periodically
            if batch_stats.total_requests >= BATCH_REPORT_SIZE {
                let batch_result = self.create_batch_result(&task, &batch_stats);
                results.push(batch_result);
                batch_stats = BatchStats::new();
            }
        }
        
        // Report remaining stats
        if batch_stats.total_requests > 0 {
            let batch_result = self.create_batch_result(&task, &batch_stats);
            results.push(batch_result);
        }
        
        info!("Completed load test: {} total requests", 
            results.iter().map(|r| r.total_requests).sum::<u32>());
        
        Ok(results)
    }
    
    async fn execute_request(
        http_client: &Client,
        task: &WorkerTask,
    ) -> RequestMetric {
        let start = Instant::now();
        
        let mut request = match task.method {
            HttpMethod::GET => http_client.get(&task.target_url),
            HttpMethod::POST => http_client.post(&task.target_url),
            HttpMethod::PUT => http_client.put(&task.target_url),
            HttpMethod::DELETE => http_client.delete(&task.target_url),
            HttpMethod::PATCH => http_client.patch(&task.target_url),
        };
        
        // Add headers
        if let Some(headers) = &task.headers {
            for (key, value) in headers {
                request = request.header(key, value);
            }
        }
        
        // Add body
        if let Some(body) = &task.body {
            request = request.body(body.clone());
        }
        
        // Execute request
        let result = request.send().await;
        let latency = start.elapsed().as_millis() as u64;
        
        match result {
            Ok(response) => {
                let status_code = response.status().as_u16();
                let success = response.status().is_success();
                
                RequestMetric {
                    timestamp: Utc::now(),
                    latency_ms: latency,
                    status_code,
                    success,
                    error: if !success {
                        Some(format!("HTTP {}", status_code))
                    } else {
                        None
                    },
                }
            }
            Err(e) => {
                RequestMetric {
                    timestamp: Utc::now(),
                    latency_ms: latency,
                    status_code: 0,
                    success: false,
                    error: Some(e.to_string()),
                }
            }
        }
    }
    
    fn create_batch_result(&self, task: &WorkerTask, stats: &BatchStats) -> WorkerResult {
        WorkerResult {
            task_id: task.task_id.clone(),
            worker_id: self.worker_id.clone(),
            timestamp: Utc::now(),
            latency_ms: None,
            status_code: None,
            success: stats.success_count > 0,
            error: None,
            total_requests: stats.total_requests,
            success_count: stats.success_count,
            error_count: stats.error_count,
            avg_latency_ms: stats.avg_latency(),
            p95_latency_ms: stats.percentile(95.0),
            p99_latency_ms: stats.percentile(99.0),
        }
    }
    
    async fn send_result(&self, results: Vec<WorkerResult>) -> Result<()> {
        let mut conn = self.redis_client.get_async_connection().await?;
        
        for result in results {
            let json = serde_json::to_string(&result)?;
            conn.rpush(RESULT_QUEUE, json).await?;
        }
        
        Ok(())
    }
}

#[tokio::main]
async fn main() -> Result<()> {
    // Initialize logging
    tracing_subscriber::fmt()
        .with_env_filter("loadtest_worker=debug,info")
        .init();
    
    info!("Load Test Worker starting...");
    
    // Create and run worker
    let worker = Worker::new()?;
    worker.run().await?;
    
    Ok(())
}