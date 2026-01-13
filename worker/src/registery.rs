// ========== src/registry.rs (was registery.rs) ==========
use crate::{config::Config, models::WorkerRegistration};
use reqwest::{Client, StatusCode};
use tracing::{info, warn, error};
use std::time::Duration;

pub async fn register(cfg: &Config, worker_id: &str) -> anyhow::Result<()> {
    let client = Client::new();

    let payload = WorkerRegistration {
        worker_id: worker_id.to_string(),
        version: cfg.worker_version.clone(),
        capacity: cfg.max_concurrency,
        capabilities: vec!["http".to_string(), "https".to_string(), "http2".to_string()], // Default capabilities
        max_rps: cfg.max_concurrent_requests as u32,
    };

    let mut attempts = 0;
    const MAX_ATTEMPTS: u32 = 5;
    
    loop {
        attempts += 1;
        info!("Registering worker {} with controller at {} (Attempt {}/{})", 
            worker_id, cfg.controller_url, attempts, MAX_ATTEMPTS);
            
        let result = client
            .post(format!("{}/workers/register", cfg.controller_url))
            .header("Authorization", format!("Bearer {}", cfg.worker_token.clone().unwrap_or_default()))
            .json(&payload)
            .send()
            .await;
            
        match result {
            Ok(response) => {
                if response.status().is_success() {
                    info!("Worker registered successfully");
                    return Ok(());
                } else {
                    let status = response.status();
                    let text = response.text().await.unwrap_or_default();
                    warn!("Registration failed with status {}: {}", status, text);
                    
                    if status == StatusCode::UNAUTHORIZED || status == StatusCode::FORBIDDEN {
                        // Don't retry auth errors
                        return Err(anyhow::anyhow!("Registration unauthorized: {}", text));
                    }
                }
            }
            Err(e) => {
                warn!("Registration connection error: {}", e);
            }
        }
        
        if attempts >= MAX_ATTEMPTS {
            error!("Max registration attempts reached");
            return Err(anyhow::anyhow!("Failed to register worker after {} attempts", MAX_ATTEMPTS));
        }
        
        tokio::time::sleep(Duration::from_secs(2u64.pow(attempts))).await;
    }
}
