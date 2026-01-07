// ========== src/rate_limiter.rs ==========
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::Mutex;
use std::sync::atomic::{AtomicU64, Ordering};

static TARGET_RPS: AtomicU64 = AtomicU64::new(1000);

pub fn adjust(latency_ms: u64) {
    if latency_ms > 1000 {
        TARGET_RPS.fetch_sub(50, Ordering::Relaxed);
    } else {
        TARGET_RPS.fetch_add(10, Ordering::Relaxed);
    }
}

pub fn current_rps() -> u64 {
    TARGET_RPS.load(Ordering::Relaxed)
}

pub struct TokenBucketRateLimiter {
    capacity: u32,
    tokens: Arc<Mutex<u32>>,
    refill_rate: f64,
    last_refill: Arc<Mutex<Instant>>,
}

impl TokenBucketRateLimiter {
    pub fn new(rps: u32) -> Self {
        Self {
            capacity: rps,
            tokens: Arc::new(Mutex::new(rps)),
            refill_rate: rps as f64,
            last_refill: Arc::new(Mutex::new(Instant::now())),
        }
    }
    
    pub async fn acquire(&self) -> bool {
        self.refill_tokens().await;
        
        let mut tokens = self.tokens.lock().await;
        if *tokens > 0 {
            *tokens -= 1;
            true
        } else {
            false
        }
    }
    
    async fn refill_tokens(&self) {
        let now = Instant::now();
        let mut last_refill = self.last_refill.lock().await;
        let elapsed = now.duration_since(*last_refill).as_secs_f64();
        
        if elapsed > 0.0 {
            let mut tokens = self.tokens.lock().await;
            let new_tokens = (elapsed * self.refill_rate) as u32;
            *tokens = (*tokens + new_tokens).min(self.capacity);
            *last_refill = now;
        }
    }
}

pub struct SlidingWindowRateLimiter {
    window_size: Duration,
    max_requests: u32,
    requests: Arc<Mutex<Vec<Instant>>>,
}

impl SlidingWindowRateLimiter {
    pub fn new(rps: u32, window_seconds: u64) -> Self {
        Self {
            window_size: Duration::from_secs(window_seconds),
            max_requests: rps * window_seconds as u32,
            requests: Arc::new(Mutex::new(Vec::new())),
        }
    }
    
    pub async fn acquire(&self) -> bool {
        let now = Instant::now();
        let mut requests = self.requests.lock().await;
        
        // Remove old requests outside the window
        requests.retain(|&time| now.duration_since(time) < self.window_size);
        
        if requests.len() < self.max_requests as usize {
            requests.push(now);
            true
        } else {
            false
        }
    }
}