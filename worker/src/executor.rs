use crate::{
    backoff::{self, BackoffConfig},
    circuit_breaker::{CircuitBreaker, CircuitBreakerConfig},
    http_client,
    metrics,
    models::WorkerTask,
    rate_limiter,
    config::Config,
};
use std::time::{Duration, Instant};

pub async fn execute(cfg: Config, task: WorkerTask) {
    // Note: Creating a new circuit breaker per request is anti-pattern if we want to track state across requests.
    // However, this executor seems to be a stateless function helper. 
    // Ideally, CircuitBreaker should be passed in or managed globally/per-host.
    // For now, adhering to existing structure but using new config.
    let mut breaker = CircuitBreaker::with_config(CircuitBreakerConfig {
        failure_threshold: 5,
        reset_timeout: Duration::from_secs(30),
        half_open_attempts: 1,
    });

    if !breaker.allow() {
        return;
    }

    let start = Instant::now();

    let backoff_config = BackoffConfig {
        initial_delay_ms: cfg.retry_delay_ms,
        max_delay_ms: 10000,
        multiplier: 2.0,
        jitter_factor: 0.2,
    };

    let result = backoff::retry_with_config(cfg.retry_max_attempts, backoff_config, || async {
        http_client::send(&cfg, &task).await.ok()
    })
    .await;

    let latency = start.elapsed().as_millis() as u64;

    match result {
        Some(status) => {
            rate_limiter::adjust(latency);
            // Updated to use the new emit function if record is not available
            metrics::emit(latency, status, task.org_id);
            breaker.record_success();
        }
        None => breaker.record_failure(),
    }
}
