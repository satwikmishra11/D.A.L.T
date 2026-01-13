// ========== src/metrics.rs ==========
use crate::models::RequestMetric;
use std::sync::{Arc, Mutex};
use serde::Serialize;
use uuid::Uuid;
use hdrhistogram::Histogram;

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
    histogram: Arc<Mutex<Histogram<u64>>>,
    success_count: Arc<Mutex<u32>>,
    error_count: Arc<Mutex<u32>>,
    total_count: Arc<Mutex<u32>>,
}

impl MetricsCollector {
    pub fn new() -> Self {
        Self {
            // Track latencies from 1ms to 1 hour (3,600,000ms) with 3 significant figures
            histogram: Arc::new(Mutex::new(Histogram::<u64>::new_with_bounds(1, 60 * 60 * 1000, 3).unwrap())),
            success_count: Arc::new(Mutex::new(0)),
            error_count: Arc::new(Mutex::new(0)),
            total_count: Arc::new(Mutex::new(0)),
        }
    }
    
    pub fn record(&self, metric: &RequestMetric) {
        let mut hist = self.histogram.lock().unwrap();
        // Record latency, clamped to min 1ms for histogram
        let _ = hist.record(metric.latency_ms.max(1));
        
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
        
        let hist = self.histogram.lock().unwrap();
        
        let avg = hist.mean();
        let p95 = hist.value_at_quantile(0.95) as f64;
        let p99 = hist.value_at_quantile(0.99) as f64;
        
        (total, success, errors, avg, p95, p99)
    }
    
    pub fn reset(&self) {
        let mut hist = self.histogram.lock().unwrap();
        hist.reset();
        
        *self.success_count.lock().unwrap() = 0;
        *self.error_count.lock().unwrap() = 0;
        *self.total_count.lock().unwrap() = 0;
    }

    pub fn total_count(&self) -> u32 {
        *self.total_count.lock().unwrap()
    }
}

// Deprecated: Internal percentile function no longer needed with HdrHistogram
#[allow(dead_code)]
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

// Helper to emit metrics (compatibility wrapper)
pub fn emit(latency: u64, status: u16, org: String) {
    // This function seems to be used as a fire-and-forget logger/emitter in some contexts.
    // For now, we'll just log it or it could be hooked up to an external system.
    tracing::debug!("Metric emitted: latency={}ms status={} org={}", latency, status, org);
}