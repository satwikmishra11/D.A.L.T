from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "Load Test Analytics Service"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # Thresholds
    ANOMALY_STD_DEV_THRESHOLD: float = 3.0
    LATENCY_DEGRADATION_THRESHOLD_MS: float = 100.0

    class Config:
        case_sensitive = True

settings = Settings()
