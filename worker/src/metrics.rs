// ========== src/metrics.rs ==========
use crate::models::RequestMetric;
use std::sync::{Arc, Mutex};
use serde::Serialize;
use uuid::Uuid;

#[derive(Serialize)]
pub struct MetricEvent {
    pub trace_id: String,
    pub latency_ms: u64,
    pub status: u16,
    pub org: String,
}

pub fn new_metric(latency: u64, status: u16, org: String) -> MetricEvent {
    MetricEvent {
        trace_id: Uuid::new_v4().to_string(),
        latency_ms: latency,
        status,
        org,
    }
}

pub struct MetricsCollector {
    latencies: Arc<Mutex<Vec<u64>>>,
    success_count: Arc<Mutex<u32>>,
    error_count: Arc<Mutex<u32>>,
    total_count: Arc<Mutex<u32>>,
}

impl MetricsCollector {
    pub fn new() -> Self {
        Self {
            latencies: Arc::new(Mutex::new(Vec::new())),
            success_count: Arc::new(Mutex::new(0)),
            error_count: Arc::new(Mutex::new(0)),
            total_count: Arc::new(Mutex::new(0)),
        }
    }
    
    pub fn record(&self, metric: &RequestMetric) {
        let mut latencies = self.latencies.lock().unwrap();
        latencies.push(metric.latency_ms);
        
        let mut total = self.total_count.lock().unwrap();
        *total += 1;
        
        if metric.success {
            let mut success = self.success_count.lock().unwrap();
            *success += 1;
        } else {
            let mut errors = self.error_count.lock().unwrap();
            *errors += 1;
        }
    }
    
    pub fn get_stats(&self) -> (u32, u32, u32, f64, f64, f64) {
        let total = *self.total_count.lock().unwrap();
        let success = *self.success_count.lock().unwrap();
        let errors = *self.error_count.lock().unwrap();
        
        let latencies = self.latencies.lock().unwrap();
        let avg = if !latencies.is_empty() {
            latencies.iter().sum::<u64>() as f64 / latencies.len() as f64
        } else {
            0.0
        };
        
        let p95 = percentile(&latencies, 95.0);
        let p99 = percentile(&latencies, 99.0);
        
        (total, success, errors, avg, p95, p99)
    }
    
    pub fn reset(&self) {
        self.latencies.lock().unwrap().clear();
        *self.success_count.lock().unwrap() = 0;
        *self.error_count.lock().unwrap() = 0;
        *self.total_count.lock().unwrap() = 0;
    }
}

fn percentile(values: &[u64], p: f64) -> f64 {
    if values.is_empty() {
        return 0.0;
    }
    
    let mut sorted = values.to_vec();
    sorted.sort_unstable();
    
    let index = ((p / 100.0) * sorted.len() as f64).ceil() as usize - 1;
    let index = index.min(sorted.len() - 1);
    
    sorted[index] as f64
}