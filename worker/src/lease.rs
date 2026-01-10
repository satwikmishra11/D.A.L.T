use crate::models::WorkerTask;
use reqwest::Client;

pub async fn lease_task(
    controller_url: &str,
    worker_id: &str,
) -> Option<WorkerTask> {
    let client = Client::new();

    client
        .post(format!("{}/workers/lease", controller_url))
        .json(&serde_json::json!({ "worker_id": worker_id }))
        .send()
        .await
        .ok()?
        .json::<WorkerTask>()
        .await
        .ok()
}
