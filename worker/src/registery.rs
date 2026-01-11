use crate::{config::Config, models::WorkerRegistration};
use reqwest::Client;

pub async fn register(cfg: &Config, worker_id: &str) {
    let client = Client::new();

    let payload = WorkerRegistration {
        worker_id: worker_id.to_string(),
        version: cfg.worker_version.clone(),
        capacity: cfg.max_concurrency,
    };

    client
        .post(format!("{}/workers/register", cfg.controller_url))
        .bearer_auth(&cfg.worker_token)
        .json(&payload)
        .send()
        .await
        .expect("worker registration failed");
}
