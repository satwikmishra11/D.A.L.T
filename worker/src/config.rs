// ========== src/config.rs ==========
use serde::{Deserialize, Serialize};
use std::env;
use anyhow::{Result, Context};

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct Config {
    pub redis_url: String,
    pub task_queue: String,
    pub result_queue: String,
    pub heartbeat_key: String,
    pub heartbeat_interval_secs: u64,
    pub batch_report_size: u32,
    pub max_concurrent_requests: usize,
    pub request_timeout_secs: u64,
    pub connection_timeout_secs: u64,
    pub pool_max_idle_per_host: usize,
    pub retry_enabled: bool,
    pub retry_max_attempts: u32,
    pub retry_delay_ms: u64,
    pub enable_compression: bool,
    pub enable_http2: bool,
}

impl Config {
    pub fn from_env() -> Result<Self> {
        Ok(Self {
            redis_url: env::var("REDIS_URL")
                .unwrap_or_else(|_| "redis://127.0.0.1:6379".to_string()),
            task_queue: env::var("TASK_QUEUE")
                .unwrap_or_else(|_| "loadtest:tasks".to_string()),
            result_queue: env::var("RESULT_QUEUE")
                .unwrap_or_else(|_| "loadtest:results".to_string()),
            heartbeat_key: env::var("HEARTBEAT_KEY")
                .unwrap_or_else(|_| "loadtest:heartbeat".to_string()),
            heartbeat_interval_secs: env::var("HEARTBEAT_INTERVAL_SECS")
                .unwrap_or_else(|_| "5".to_string())
                .parse()
                .context("Invalid heartbeat interval")?,
            batch_report_size: env::var("BATCH_REPORT_SIZE")
                .unwrap_or_else(|_| "100".to_string())
                .parse()
                .context("Invalid batch report size")?,
            max_concurrent_requests: env::var("MAX_CONCURRENT_REQUESTS")
                .unwrap_or_else(|_| "1000".to_string())
                .parse()
                .context("Invalid max concurrent requests")?,
            request_timeout_secs: env::var("REQUEST_TIMEOUT_SECS")
                .unwrap_or_else(|_| "30".to_string())
                .parse()
                .context("Invalid request timeout")?,
            connection_timeout_secs: env::var("CONNECTION_TIMEOUT_SECS")
                .unwrap_or_else(|_| "10".to_string())
                .parse()
                .context("Invalid connection timeout")?,
            pool_max_idle_per_host: env::var("POOL_MAX_IDLE_PER_HOST")
                .unwrap_or_else(|_| "100".to_string())
                .parse()
                .context("Invalid pool max idle")?,
            retry_enabled: env::var("RETRY_ENABLED")
                .unwrap_or_else(|_| "false".to_string())
                .parse()
                .unwrap_or(false),
            retry_max_attempts: env::var("RETRY_MAX_ATTEMPTS")
                .unwrap_or_else(|_| "3".to_string())
                .parse()
                .unwrap_or(3),
            retry_delay_ms: env::var("RETRY_DELAY_MS")
                .unwrap_or_else(|_| "1000".to_string())
                .parse()
                .unwrap_or(1000),
            enable_compression: env::var("ENABLE_COMPRESSION")
                .unwrap_or_else(|_| "true".to_string())
                .parse()
                .unwrap_or(true),
            enable_http2: env::var("ENABLE_HTTP2")
                .unwrap_or_else(|_| "true".to_string())
                .parse()
                .unwrap_or(true),
        })
    }
}