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
        Self::new_with_tls(config, true) // default to true for backward compatibility
    }

    pub fn new_with_tls(config: &HttpConfig, ignore_tls_errors: bool) -> Result<Self> {
        let client = ClientBuilder::new()
            .timeout(Duration::from_secs(config.timeout_seconds))
            .connect_timeout(Duration::from_secs(config.connect_timeout_seconds))
            .pool_max_idle_per_host(config.max_idle_connections)
            .pool_idle_timeout(Duration::from_secs(90))
            .tcp_nodelay(true)
            .danger_accept_invalid_certs(ignore_tls_errors)
            .build()?;

        Ok(Self { inner: client })
    }

    pub fn client(&self) -> &Client {
        &self.inner
    }
}
