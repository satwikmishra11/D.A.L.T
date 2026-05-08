use anyhow::Result;
use reqwest::{Client, ClientBuilder};
use std::time::Duration;
use crate::config::HttpConfig;

#[derive(Clone)]
pub struct HttpClient {
    inner: Client,
}

impl HttpClient {
    pub fn new(config: &HttpConfig) -> Result<Self> {
        let client = ClientBuilder::new()
            .timeout(Duration::from_secs(config.timeout_seconds))
            .connect_timeout(Duration::from_secs(config.connect_timeout_seconds))
            .pool_max_idle_per_host(config.max_idle_connections)
            .pool_idle_timeout(Duration::from_secs(90))
            .tcp_nodelay(true) // Important for latency testing
            .danger_accept_invalid_certs(true) // Crucial for internal/mock load test targets
            .build()?;

        Ok(Self { inner: client })
    }

    pub fn client(&self) -> &Client {
        &self.inner
    }
}
