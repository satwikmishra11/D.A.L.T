mod clients;
mod config;
mod engine;
mod metrics;
mod models;
mod worker;

use anyhow::Result;
use tracing::info;
use crate::config::Settings;
use crate::worker::WorkerService;

#[tokio::main]
async fn main() -> Result<()> {
    // Load configuration
    let settings = Settings::new()?;

    // Initialize structured logging
    let subscriber = tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| format!("loadtest_worker={},info", settings.logging.level).into()),
        );

    if settings.logging.json {
        subscriber.json().init();
    } else {
        subscriber.init();
    }

    info!("Starting LoadTest Worker (ID: {})", settings.worker_id);
    info!("Configuration loaded: Mode={}", std::env::var("RUN_MODE").unwrap_or("development".into()));

    // Start Worker Service
    let mut service = WorkerService::new(settings).await?;
    service.run().await?;

    Ok(())
}
