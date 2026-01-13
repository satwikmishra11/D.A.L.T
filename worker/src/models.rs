// ========== src/models.rs ==========
use serde::{Deserialize, Serialize};
use chrono::{DateTime, Utc};

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
    pub headers: Option<std::collections::HashMap<String, String>>,
    pub body: Option<String>,
    pub rps: u32,
    pub duration_seconds: u32,
    pub org_id: String,
    // Alias for compatibility if needed
    #[serde(skip)]
    pub org: String, 
}

// Custom deserializer or constructor logic might be needed for 'org' vs 'org_id' alias
// But for now, we'll assume org_id is primary.

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WorkerResult {
    pub task_id: String,
    pub worker_id: String,
    pub timestamp: DateTime<Utc>,
    pub latency_ms: Option<u64>,
    pub status_code: Option<u16>,
    pub success: bool,
    pub error: Option<String>,
    
    // Batch stats
    pub total_requests: u32,
    pub success_count: u32,
    pub error_count: u32,
    pub avg_latency_ms: f64,
    pub p95_latency_ms: f64,
    pub p99_latency_ms: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RequestMetric {
    pub timestamp: DateTime<Utc>,
    pub latency_ms: u64,
    pub dns_lookup_ms: u64,
    pub tcp_connection_ms: u64,
    pub tls_handshake_ms: u64,
    pub ttfb_ms: u64,
    pub status_code: u16,
    pub success: bool,
    pub error: Option<String>,
    pub error_type: Option<String>,
}

impl RequestMetric {
    pub fn new_error(error_message: String, error_type: String) -> Self {
        Self {
            timestamp: Utc::now(),
            latency_ms: 0,
            dns_lookup_ms: 0,
            tcp_connection_ms: 0,
            tls_handshake_ms: 0,
            ttfb_ms: 0,
            status_code: 0,
            success: false,
            error: Some(error_message),
            error_type: Some(error_type),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum WorkerStatus {
    Idle,
    Busy,
    Draining,
    Offline,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct WorkerRegistration {
    pub worker_id: String,
    pub version: String,
    pub capacity: usize,
    pub capabilities: Vec<String>,
    pub max_rps: u32,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct WorkerHeartbeat {
    pub worker_id: String,
    pub timestamp: DateTime<Utc>,
    pub status: WorkerStatus,
    pub current_task_id: Option<String>,
    pub requests_processed: u64,
    pub cpu: f32,
    pub memory_mb: u64,
    pub active_tasks: usize,
}
