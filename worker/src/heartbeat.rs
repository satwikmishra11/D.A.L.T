use crate::models::WorkerHeartbeat;
use reqwest::Client;
use std::time::Duration;

pub async fn start_heartbeat(controller: String, worker_id: String) {
    let client = Client::new();

    loop {
        let hb = WorkerHeartbeat {
            worker_id: worker_id.clone(),
            cpu: sysinfo::System::new_all().global_cpu_info().cpu_usage(),
            memory_mb: sysinfo::System::new_all().used_memory() / 1024,
            active_tasks: crate::worker::active_tasks(),
        };

        let _ = client
            .post(format!("{}/workers/heartbeat", controller))
            .json(&hb)
            .send()
            .await;

        tokio::time::sleep(Duration::from_secs(5)).await;
    }
}
