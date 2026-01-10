use crate::{
    circuit_breaker::CircuitBreaker,
    http_client,
    metrics,
    models::WorkerTask,
    rate_limiter,
};
use std::time::Instant;

pub async fn execute(task: WorkerTask) {
    let mut breaker = CircuitBreaker::new();

    if !breaker.allow() {
        return;
    }

    let start = Instant::now();
    match http_client::send(&task).await {
        Ok(status) => {
            let latency = start.elapsed().as_millis() as u64;
            rate_limiter::adjust(latency);
            metrics::record(latency, status, &task.org_id);
            breaker.success();
        }
        Err(_) => breaker.failure(),
    }
}
