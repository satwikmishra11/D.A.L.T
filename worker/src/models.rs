use serde::{Deserialize, Serialize};
use chrono::{DateTime, Utc};
use std::collections::HashMap;

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WorkerTask {
    pub task_id: String,
    pub execution_id: String,
    pub target_url: String,
    pub method: HttpMethod,
    pub headers: Option<HashMap<String, String>>,
    pub body: Option<String>,
    pub rps: u32,
    pub duration_seconds: u32,
    pub org_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WorkerResult {
    pub task_id: String,
    pub worker_id: String,
    pub timestamp: DateTime<Utc>,
    
    // Core stats
    pub success: bool,
    pub total_requests: u64,
    pub success_count: u64,
    pub error_count: u64,
    
    // Latency stats (ms)
    pub avg_latency_ms: f64,
    pub p50_latency_ms: f64,
    pub p90_latency_ms: f64,
    pub p95_latency_ms: f64,
    pub p99_latency_ms: f64,
    pub max_latency_ms: f64,
    
    // Throughput
    pub actual_rps: f64,
    
    pub error_msg: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum WorkerStatus {
    Idle,
    Busy,
    Draining,
    Offline,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct WorkerHeartbeat {
    pub worker_id: String,
    pub timestamp: DateTime<Utc>,
    pub status: WorkerStatus,
    pub current_task_id: Option<String>,
    pub requests_processed: u64,
    pub cpu_usage: f32,
    pub memory_usage_mb: u64,
    pub active_threads: usize,
}