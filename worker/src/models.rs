// ========== Enhanced models.rs additions ==========
use serde::{Deserialize, Serialize};
use serde::{Serialize, Deserialize};

#[derive(Serialize)]
pub struct WorkerRegistration {
    pub worker_id: String,
    pub capabilities: Vec<String>,
    pub max_rps: u32,
}

#[derive(Serialize)]
pub struct WorkerHeartbeat {
    pub worker_id: String,
    pub cpu: f32,
    pub memory_mb: u64,
    pub active_tasks: usize,
}


impl RequestMetric {
    pub fn new_error(error_message: String, error_type: String) -> Self {
        Self {
            timestamp: chrono::Utc::now(),
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
pub struct RequestMetric {
    pub timestamp: chrono::DateTime<chrono::Utc>,
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

use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Clone)]
pub struct WorkerTask {
    pub task_id: String,
    pub execution_id: String,
    pub target_url: String,
    pub method: String,
    pub org_id: String,
}

#[derive(Serialize)]
pub struct WorkerRegistration {
    pub worker_id: String,
    pub version: String,
    pub capacity: usize,
}

#[derive(Serialize)]
pub struct WorkerHeartbeat {
    pub worker_id: String,
    pub cpu: f32,
    pub memory_mb: u64,
    pub active_tasks: usize,
}
