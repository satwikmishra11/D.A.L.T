use anyhow::{Context, Result};
use redis::{Client, aio::ConnectionManager};
use crate::config::RedisConfig;
use tracing::info;

#[derive(Clone)]
pub struct RedisClient {
    client: Client,
    manager: ConnectionManager,
    config: RedisConfig,
}

impl RedisClient {
    pub async fn new(config: RedisConfig) -> Result<Self> {
        let client = Client::open(config.url.as_str())
            .context("Failed to parse Redis URL")?;
        
        // ConnectionManager handles reconnects automatically
        let manager = client.get_tokio_connection_manager().await
            .context("Failed to create Redis connection manager")?;

        info!("Connected to Redis at {}", config.url);

        Ok(Self {
            client,
            manager,
            config,
        })
    }

    pub async fn pop_task(&self) -> Result<Option<String>> {
        let mut conn = self.manager.clone();
        let result: Option<String> = redis::cmd("LPOP")
            .arg(&self.config.task_queue)
            .query_async(&mut conn)
            .await?;
        Ok(result)
    }

    pub async fn push_result(&self, json: &str) -> Result<()> {
        let mut conn = self.manager.clone();
        redis::cmd("RPUSH")
            .arg(&self.config.result_queue)
            .arg(json)
            .query_async(&mut conn)
            .await?;
        Ok(())
    }

    pub async fn update_heartbeat(&self, key: &str, json: &str, ttl_secs: usize) -> Result<()> {
        let mut conn = self.manager.clone();
        redis::pipe()
            .set(key, json)
            .expire(key, ttl_secs)
            .query_async(&mut conn)
            .await?;
        Ok(())
    }
}
