use hdrhistogram::Histogram;
use std::time::Instant;

pub struct MetricsCollector {
    histogram: Histogram<u64>,
    success_count: u64,
    error_count: u64,
    start_time: Instant,
}

impl MetricsCollector {
    pub fn new() -> Self {
        Self {
            // Track latency from 1us to 1hr with 3 significant figures
            histogram: Histogram::<u64>::new_with_bounds(1, 60 * 60 * 1000 * 1000, 3).unwrap(),
            success_count: 0,
            error_count: 0,
            start_time: Instant::now(),
        }
    }

    pub fn record_success(&mut self, latency_us: u64) {
        self.success_count += 1;
        // Saturating add to avoid panic if latency exceeds bounds (unlikely but safe)
        let _ = self.histogram.record(latency_us);
    }

    pub fn record_error(&mut self) {
        self.error_count += 1;
    }

    pub fn total_requests(&self) -> u64 {
        self.success_count + self.error_count
    }

    pub fn reset(&mut self) {
        self.histogram.reset();
        self.success_count = 0;
        self.error_count = 0;
        self.start_time = Instant::now();
    }

    pub fn get_stats(&self) -> MetricsSnapshot {
        MetricsSnapshot {
            count: self.total_requests(),
            success: self.success_count,
            error: self.error_count,
            min: self.histogram.min(),
            max: self.histogram.max(),
            mean: self.histogram.mean(),
            p50: self.histogram.value_at_percentile(50.0),
            p90: self.histogram.value_at_percentile(90.0),
            p95: self.histogram.value_at_percentile(95.0),
            p99: self.histogram.value_at_percentile(99.0),
        }
    }
}

pub struct MetricsSnapshot {
    pub count: u64,
    pub success: u64,
    pub error: u64,
    pub min: u64,
    pub max: u64,
    pub mean: f64,
    pub p50: u64,
    pub p90: u64,
    pub p95: u64,
    pub p99: u64,
}
