use tokio::time::{sleep, Duration};

pub async fn retry<F, Fut, T>(mut attempts: u8, mut f: F) -> Option<T>
where
    F: FnMut() -> Fut,
    Fut: std::future::Future<Output = Option<T>>,
{
    let mut delay = 100;

    while attempts > 0 {
        if let Some(val) = f().await {
            return Some(val);
        }

        sleep(Duration::from_millis(delay)).await;
        delay *= 2;
        attempts -= 1;
    }
    None
}
