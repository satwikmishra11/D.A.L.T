use config::{Config, ConfigError, Environment, File};
use serde::Deserialize;
use std::env;

#[derive(Debug, Deserialize, Clone)]
pub struct Settings {
    pub worker_id: String,
    pub redis: RedisConfig,
    pub http: HttpConfig,
    pub logging: LoggingConfig,
    pub limits: LimitsConfig,
}

#[derive(Debug, Deserialize, Clone)]
pub struct RedisConfig {
    pub url: String,
    pub task_queue: String,
    pub result_queue: String,
    pub heartbeat_key: String,
}

#[derive(Debug, Deserialize, Clone)]
pub struct HttpConfig {
    pub timeout_seconds: u64,
    pub max_idle_connections: usize,
    pub connect_timeout_seconds: u64,
}

#[derive(Debug, Deserialize, Clone)]
pub struct LoggingConfig {
    pub level: String,
    pub json: bool,
}

#[derive(Debug, Deserialize, Clone)]
pub struct LimitsConfig {
    pub max_concurrent_tasks: usize,
    pub max_virtual_users: usize,
    pub heartbeat_interval_seconds: u64,
}

impl Settings {
    pub fn new() -> Result<Self, ConfigError> {
        let run_mode = env::var("RUN_MODE").unwrap_or_else(|_| "development".into());

        let s = Config::builder()
            // Start with default settings
            .set_default("worker_id", uuid::Uuid::new_v4().to_string())?
            .set_default("redis.url", "redis://127.0.0.1:6379")?
            .set_default("redis.task_queue", "loadtest:tasks")?
            .set_default("redis.result_queue", "loadtest:results")?
            .set_default("redis.heartbeat_key", "loadtest:heartbeat")?
            
            .set_default("http.timeout_seconds", 30)?
            .set_default("http.max_idle_connections", 100)?
            .set_default("http.connect_timeout_seconds", 5)?
            
            .set_default("logging.level", "info")?
            .set_default("logging.json", false)?
            
            .set_default("limits.max_concurrent_tasks", 1)?
            .set_default("limits.max_virtual_users", 5000)?
            .set_default("limits.heartbeat_interval_seconds", 5)?

            // Add environment variables (overrides)
            // e.g. APP_REDIS__URL=...
            .add_source(Environment::with_prefix("APP").separator("__"))
            .build()?;

        s.try_deserialize()
    }
}
