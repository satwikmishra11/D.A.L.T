// ========== src/http_client.rs ==========
use crate::config::Config;
use crate::models::{HttpMethod, RequestMetric, WorkerTask};
use anyhow::Result;
use reqwest::{Client, ClientBuilder};
use std::time::{Duration, Instant};
use chrono::Utc;
use tracing::{debug, warn};

pub struct HttpClientManager {
    client: Client,
    config: Config,
}

impl HttpClientManager {
    pub fn new(config: Config) -> Result<Self> {
        let client = ClientBuilder::new()
            .timeout(Duration::from_secs(config.request_timeout_secs))
            .connect_timeout(Duration::from_secs(config.connection_timeout_secs))
            .pool_max_idle_per_host(config.pool_max_idle_per_host)
            .gzip(config.enable_compression)
            .brotli(config.enable_compression)
            .http2_prior_knowledge()
            .build()?;
        
        Ok(Self { client, config })
    }
    
    pub async fn execute_request(&self, task: &WorkerTask) -> RequestMetric {
        let mut attempt = 0;
        let max_attempts = if self.config.retry_enabled {
            self.config.retry_max_attempts
        } else {
            1
        };
        
        loop {
            attempt += 1;
            
            let start = Instant::now();
            let result = self.execute_single_request(task).await;
            let latency = start.elapsed().as_millis() as u64;
            
            match result {
                Ok((status_code, success)) => {
                    return RequestMetric {
                        timestamp: Utc::now(),
                        latency_ms: latency,
                        dns_lookup_ms: 0, // Would need custom DNS resolver
                        tcp_connection_ms: 0,
                        tls_handshake_ms: 0,
                        ttfb_ms: latency, // Simplified
                        status_code,
                        success,
                        error: if !success {
                            Some(format!("HTTP {}", status_code))
                        } else {
                            None
                        },
                        error_type: if !success {
                            Some(classify_error_type(status_code))
                        } else {
                            None
                        },
                    };
                }
                Err(e) => {
                    if attempt < max_attempts {
                        warn!(
                            "Request failed (attempt {}/{}): {}. Retrying...",
                            attempt, max_attempts, e
                        );
                        tokio::time::sleep(Duration::from_millis(
                            self.config.retry_delay_ms
                        )).await;
                        continue;
                    }
                    
                    return RequestMetric {
                        timestamp: Utc::now(),
                        latency_ms: latency,
                        dns_lookup_ms: 0,
                        tcp_connection_ms: 0,
                        tls_handshake_ms: 0,
                        ttfb_ms: 0,
                        status_code: 0,
                        success: false,
                        error: Some(e.to_string()),
                        error_type: Some(classify_error_from_message(&e.to_string())),
                    };
                }
            }
        }
    }
    
    async fn execute_single_request(
        &self,
        task: &WorkerTask,
    ) -> Result<(u16, bool)> {
        let mut request = match task.method {
            HttpMethod::GET => self.client.get(&task.target_url),
            HttpMethod::POST => self.client.post(&task.target_url),
            HttpMethod::PUT => self.client.put(&task.target_url),
            HttpMethod::DELETE => self.client.delete(&task.target_url),
            HttpMethod::PATCH => self.client.patch(&task.target_url),
            HttpMethod::HEAD => self.client.head(&task.target_url),
            HttpMethod::OPTIONS => {
                self.client.request(reqwest::Method::OPTIONS, &task.target_url)
            }
        };
        
        // Add headers
        if let Some(headers) = &task.headers {
            for (key, value) in headers {
                request = request.header(key, value);
            }
        }
        
        // Add body
        if let Some(body) = &task.body {
            request = request.body(body.clone());
        }
        
        // Execute request
        let response = request.send().await?;
        let status_code = response.status().as_u16();
        let success = response.status().is_success();
        
        // Consume response body to ensure connection reuse
        let _ = response.bytes().await;
        
        Ok((status_code, success))
    }
}

fn classify_error_type(status_code: u16) -> String {
    match status_code {
        400 => "BAD_REQUEST".to_string(),
        401 => "UNAUTHORIZED".to_string(),
        403 => "FORBIDDEN".to_string(),
        404 => "NOT_FOUND".to_string(),
        408 => "TIMEOUT".to_string(),
        429 => "RATE_LIMITED".to_string(),
        500 => "INTERNAL_SERVER_ERROR".to_string(),
        502 => "BAD_GATEWAY".to_string(),
        503 => "SERVICE_UNAVAILABLE".to_string(),
        504 => "GATEWAY_TIMEOUT".to_string(),
        _ if status_code >= 400 && status_code < 500 => "CLIENT_ERROR".to_string(),
        _ if status_code >= 500 => "SERVER_ERROR".to_string(),
        _ => "UNKNOWN".to_string(),
    }
}

fn classify_error_from_message(message: &str) -> String {
    if message.contains("timeout") {
        "TIMEOUT".to_string()
    } else if message.contains("connection") {
        "CONNECTION_ERROR".to_string()
    } else if message.contains("DNS") || message.contains("dns") {
        "DNS_ERROR".to_string()
    } else if message.contains("TLS") || message.contains("SSL") {
        "TLS_ERROR".to_string()
    } else {
        "NETWORK_ERROR".to_string()
    }
}