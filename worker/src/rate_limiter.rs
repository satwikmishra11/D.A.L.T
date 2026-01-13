// ========== src/rate_limiter.rs ==========
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::Mutex;
use std::sync::atomic::{AtomicU64, Ordering};
use std::collections::VecDeque;

static TARGET_RPS: AtomicU64 = AtomicU64::new(1000);

pub fn adjust(latency_ms: u64) {
    // Simple AIMD:
    // If latency > 1000ms, cut RPS by 10% (Multiplicative Decrease)
    // Else, increase by 10 RPS (Additive Increase)
    // We keep a floor of 10 RPS to prevent starvation.
    let current = TARGET_RPS.load(Ordering::Relaxed);
    if latency_ms > 1000 {
        let new_val = std::cmp::max(10, (current as f64 * 0.9) as u64);
        TARGET_RPS.store(new_val, Ordering::Relaxed);
    } else {
        TARGET_RPS.fetch_add(10, Ordering::Relaxed);
    }
}

pub fn current_rps() -> u64 {
    TARGET_RPS.load(Ordering::Relaxed)
}

// Token Bucket Implementation
#[derive(Clone)]
pub struct TokenBucketRateLimiter {
    capacity: u32,
    tokens: Arc<Mutex<f64>>,
    refill_rate: f64,
    last_refill: Arc<Mutex<Instant>>,
}

impl TokenBucketRateLimiter {
    pub fn new(rps: u32) -> Self {
        Self {
            capacity: rps,
            tokens: Arc::new(Mutex::new(rps as f64)),
            refill_rate: rps as f64,
            last_refill: Arc::new(Mutex::new(Instant::now())),
        }
    }
    
    pub async fn acquire(&self) -> bool {
        let mut tokens = self.tokens.lock().await;
        let mut last_refill = self.last_refill.lock().await;
        
        let now = Instant::now();
        let elapsed = now.duration_since(*last_refill).as_secs_f64();
        
        // Refill
        if elapsed > 0.0 {
            let new_tokens = elapsed * self.refill_rate;
            *tokens = (*tokens + new_tokens).min(self.capacity as f64);
            *last_refill = now;
        }
        
        if *tokens >= 1.0 {
            *tokens -= 1.0;
            true
        } else {
            false
        }
    }
}

// Sliding Window Implementation (Precise)
#[derive(Clone)]
pub struct SlidingWindowRateLimiter {
    window_size: Duration,
    max_requests: u32,
    requests: Arc<Mutex<VecDeque<Instant>>>,
}

impl SlidingWindowRateLimiter {
    pub fn new(rps: u32, window_seconds: u64) -> Self {
        Self {
            window_size: Duration::from_secs(window_seconds),
            max_requests: rps * window_seconds as u32,
            requests: Arc::new(Mutex::new(VecDeque::new())),
        }
    }
    
    pub async fn acquire(&self) -> bool {
        let now = Instant::now();
        let mut requests = self.requests.lock().await;
        
        // Remove expired timestamps
        while let Some(&time) = requests.front() {
            if now.duration_since(time) >= self.window_size {
                requests.pop_front();
            } else {
                break;
            }
        }
        
        if requests.len() < self.max_requests as usize {
            requests.push_back(now);
            true
        } else {
            false
        }
    }
}