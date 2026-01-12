use crate::{
    backoff,
    circuit_breaker::CircuitBreaker,
    http_client,
    metrics,
    models::WorkerTask,
    rate_limiter,
    config::Config,
};
use std::time::Instant;

pub async fn execute(cfg: Config, task: WorkerTask) {
    let mut breaker = CircuitBreaker::new();

    if !breaker.allow() {
        return;
    }

    let start = Instant::now();

    let result = backoff::retry(3, || async {
        http_client::send(&cfg, &task).await.ok()
    })
    .await;

    match result {
        Some(status) => {
            let latency = start.elapsed().as_millis() as u64;
            rate_limiter::adjust(latency);
            metrics::record(latency, status, &task.org_id);
            breaker.success();
        }
        None => breaker.failure(),
    }
}
