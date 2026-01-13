use std::time::{Duration, Instant};
use std::sync::{Arc, Mutex};
use std::sync::atomic::{AtomicU32, Ordering};

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum State {
    Closed,
    Open,
    HalfOpen,
}

#[derive(Clone)]
pub struct CircuitBreakerConfig {
    pub failure_threshold: u32,
    pub reset_timeout: Duration,
    pub half_open_attempts: u32,
}

impl Default for CircuitBreakerConfig {
    fn default() -> Self {
        Self {
            failure_threshold: 5,
            reset_timeout: Duration::from_secs(30),
            half_open_attempts: 1,
        }
    }
}

#[derive(Clone)]
pub struct CircuitBreaker {
    state: Arc<Mutex<State>>,
    failures: Arc<AtomicU32>,
    last_failure: Arc<Mutex<Option<Instant>>>,
    config: CircuitBreakerConfig,
}

impl CircuitBreaker {
    pub fn new() -> Self {
        Self::with_config(CircuitBreakerConfig::default())
    }

    pub fn with_config(config: CircuitBreakerConfig) -> Self {
        Self {
            state: Arc::new(Mutex::new(State::Closed)),
            failures: Arc::new(AtomicU32::new(0)),
            last_failure: Arc::new(Mutex::new(None)),
            config,
        }
    }

    pub fn allow(&self) -> bool {
        let mut state = self.state.lock().unwrap();
        match *state {
            State::Closed => true,
            State::Open => {
                let last_fail = self.last_failure.lock().unwrap();
                if let Some(t) = *last_fail {
                    if t.elapsed() >= self.config.reset_timeout {
                        *state = State::HalfOpen;
                        return true;
                    }
                }
                false
            }
            State::HalfOpen => {
                // In a real implementation, we might limit concurrent half-open requests
                // For now, we allow it to test connectivity
                true
            }
        }
    }

    pub fn record_success(&self) {
        let mut state = self.state.lock().unwrap();
        if *state == State::HalfOpen {
            *state = State::Closed;
            self.failures.store(0, Ordering::Relaxed);
        } else if *state == State::Closed {
             self.failures.store(0, Ordering::Relaxed);
        }
    }

    pub fn record_failure(&self) {
        let mut state = self.state.lock().unwrap();
        match *state {
            State::Closed => {
                let fails = self.failures.fetch_add(1, Ordering::Relaxed) + 1;
                if fails >= self.config.failure_threshold {
                    *state = State::Open;
                    *self.last_failure.lock().unwrap() = Some(Instant::now());
                }
            }
            State::HalfOpen => {
                *state = State::Open;
                *self.last_failure.lock().unwrap() = Some(Instant::now());
            }
            State::Open => {
                // Already open, update timer? usually no.
            }
        }
    }
    
    // Legacy support wrappers if needed, but `executor.rs` calls these.
    // I need to ensure method names match existing usage or I fix usage later.
    // Existing usage: `breaker.success()` and `breaker.failure()` in executor.rs (WAIT, prompt content shows `breaker.success()` in executor.rs but `record_success` in file content of `circuit_breaker.rs`. Let me check `executor.rs` again.)
    // `executor.rs`: `breaker.success()` / `breaker.failure()`.
    // `worker.rs`: `breaker.record_success()` / `breaker.record_failure()`.
    // It seems `executor.rs` assumes methods that don't exist in the provided `circuit_breaker.rs` content?
    // Provided `circuit_breaker.rs` content has `record_failure` and `record_success`.
    // I will stick to `record_success` and `record_failure` and fix `executor.rs` later.
    
    pub fn success(&self) {
        self.record_success();
    }
    
    pub fn failure(&self) {
        self.record_failure();
    }
}
