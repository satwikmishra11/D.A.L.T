use std::time::{Duration, Instant};

pub struct CircuitBreaker {
    failures: u32,
    opened_at: Option<Instant>,
}

impl CircuitBreaker {
    pub fn new() -> Self {
        Self { failures: 0, opened_at: None }
    }

    pub fn allow(&self) -> bool {
        match self.opened_at {
            Some(t) => t.elapsed() > Duration::from_secs(30),
            None => true,
        }
    }

    pub fn record_failure(&mut self) {
        self.failures += 1;
        if self.failures > 5 {
            self.opened_at = Some(Instant::now());
        }
    }

    pub fn record_success(&mut self) {
        self.failures = 0;
        self.opened_at = None;
    }
}
