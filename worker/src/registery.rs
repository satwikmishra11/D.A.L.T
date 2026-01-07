use crate::models::WorkerRegistration;
use reqwest::Client;

pub async fn register(controller_url: &str, worker_id: &str) {
    let client = Client::new();

    let payload = WorkerRegistration {
        worker_id: worker_id.to_string(),
        capabilities: vec!["http".into(), "https".into()],
        max_rps: 50_000,
    };

    client
        .post(format!("{}/workers/register", controller_url))
        .json(&payload)
        .send()
        .await
        .expect("worker registration failed");
}
