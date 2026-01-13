use tokio::time::{sleep, Duration};
use std::future::Future;

#[derive(Clone, Copy, Debug)]
pub struct BackoffConfig {
    pub initial_delay_ms: u64,
    pub max_delay_ms: u64,
    pub multiplier: f64,
    pub jitter_factor: f64,
}

impl Default for BackoffConfig {
    fn default() -> Self {
        Self {
            initial_delay_ms: 100,
            max_delay_ms: 5000,
            multiplier: 2.0,
            jitter_factor: 0.1,
        }
    }
}

pub async fn retry<F, Fut, T>(mut attempts: u32, mut f: F) -> Option<T>
where
    F: FnMut() -> Fut,
    Fut: Future<Output = Option<T>>,
{
    retry_with_config(attempts, BackoffConfig::default(), f).await
}

pub async fn retry_with_config<F, Fut, T>(
    mut attempts: u32, 
    config: BackoffConfig, 
    mut f: F
) -> Option<T>
where
    F: FnMut() -> Fut,
    Fut: Future<Output = Option<T>>,
{
    let mut delay = config.initial_delay_ms as f64;

    while attempts > 0 {
        if let Some(val) = f().await {
            return Some(val);
        }

        attempts -= 1;
        if attempts == 0 {
            break;
        }

        // Apply jitter: delay * (1 ± jitter_factor)
        // Simple pseudo-randomness using time to avoid pulling in `rand` dependency if not present
        let jitter = (std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .subsec_nanos() as f64 / 1_000_000_000.0) * config.jitter_factor;
            
        let current_delay = delay * (1.0 + jitter);
        let capped_delay = current_delay.min(config.max_delay_ms as f64);
        
        sleep(Duration::from_millis(capped_delay as u64)).await;
        
        delay *= config.multiplier;
    }
    None
}
