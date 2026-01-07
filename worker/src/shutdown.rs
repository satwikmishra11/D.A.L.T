use tokio::signal;

pub async fn listen() {
    signal::ctrl_c().await.expect("shutdown signal failed");
    println!("Worker draining tasks...");
    crate::worker::stop_accepting();
}
