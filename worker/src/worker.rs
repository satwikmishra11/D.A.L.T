use anyhow::Result;
use std::sync::Arc;
use tokio::time::{sleep, Duration};
use tracing::{info, warn, error, instrument};
use sysinfo::{System, SystemExt, CpuExt};

use crate::config::Settings;
use crate::clients::redis::RedisClient;
use crate::clients::http::HttpClient;
use crate::engine::TaskExecutor;
use crate::models::{WorkerHeartbeat, WorkerStatus, WorkerTask};

pub struct WorkerService {
    config: Settings,
    redis: RedisClient,
    http: HttpClient,
    system: System,
}

impl WorkerService {
    pub async fn new(config: Settings) -> Result<Self> {
        let redis = RedisClient::new(config.redis.clone()).await?;
        let http = HttpClient::new(&config.http)?;
        
        Ok(Self {
            config,
            redis,
            http,
            system: System::new_all(),
        })
    }

    pub async fn run(&mut self) -> Result<()> {
        info!("Worker {} started. Waiting for tasks...", self.config.worker_id);

        let heartbeat_handle = self.start_heartbeat_loop();

        loop {
            match self.redis.pop_task().await {
                Ok(Some(task_json)) => {
                    match serde_json::from_str::<WorkerTask>(&task_json) {
                        Ok(task) => self.process_task(task).await?,
                        Err(e) => error!("Failed to deserialize task: {}", e),
                    }
                }
                Ok(None) => {
                    // Backoff strategy could be implemented here
                    sleep(Duration::from_millis(500)).await;
                }
                Err(e) => {
                    error!("Redis error: {}", e);
                    sleep(Duration::from_secs(5)).await;
                }
            }
        }
        
        // heartbeat_handle.abort();
    }

    fn start_heartbeat_loop(&self) -> tokio::task::JoinHandle<()> {
        let redis = self.redis.clone();
        let worker_id = self.config.worker_id.clone();
        let interval_secs = self.config.limits.heartbeat_interval_seconds;
        let heartbeat_key = self.config.redis.heartbeat_key.clone();
        
        // We need a fresh system object or shared state for metrics if we want real cpu usage
        // For simplicity, passing a clone, but note System::refresh is needed
        
        tokio::spawn(async move {
            let mut sys = System::new();
            loop {
                sys.refresh_cpu();
                sys.refresh_memory();
                
                let cpu_usage = sys.global_cpu_info().cpu_usage();
                let memory_usage = sys.used_memory(); // Bytes
                
                let heartbeat = WorkerHeartbeat {
                    worker_id: worker_id.clone(),
                    timestamp: chrono::Utc::now(),
                    status: WorkerStatus::Idle, // Should reflect real status
                    current_task_id: None,
                    requests_processed: 0, // Needs shared state
                    cpu_usage,
                    memory_usage_mb: memory_usage / 1024 / 1024,
                    active_threads: 1,
                };
                
                if let Ok(json) = serde_json::to_string(&heartbeat) {
                    let key = format!("{}:{}", heartbeat_key, worker_id);
                    if let Err(e) = redis.update_heartbeat(&key, &json, 60).await {
                        warn!("Failed to send heartbeat: {}", e);
                    }
                }
                
                sleep(Duration::from_secs(interval_secs)).await;
            }
        })
    }

    #[instrument(skip(self, task), fields(task_id = %task.task_id))]
    async fn process_task(&self, task: WorkerTask) -> Result<()> {
        info!("Processing task: {} ({} RPS)", task.task_id, task.rps);
        
        let executor = TaskExecutor::new(self.http.clone(), self.config.worker_id.clone());
        let redis = self.redis.clone();
        
        // Callback for streaming results
        let callback = move |result| {
             let redis = redis.clone();
             tokio::spawn(async move {
                 if let Ok(json) = serde_json::to_string(&result) {
                     if let Err(e) = redis.push_result(&json).await {
                         error!("Failed to push result: {}", e);
                     }
                 }
             });
        };

        executor.execute(task, callback).await?;
        
        info!("Task completed");
        Ok(())
    }
}