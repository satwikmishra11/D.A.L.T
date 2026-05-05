from pydantic import BaseModel, Field
from typing import List, Dict, Optional, Any

class LatencyInput(BaseModel):
    latencies: List[float] = Field(..., min_items=1, description="List of latency measurements in milliseconds")
    timestamps: Optional[List[float]] = Field(None, description="Optional timestamps corresponding to latencies")

class StatisticalSummary(BaseModel):
    mean: float
    median: float
    std_dev: float
    p95: float
    p99: float
    min: float
    max: float
    sample_size: int
    apdex_score: float = Field(0.0, description="Application Performance Index (Apdex)")

class AnomalyResult(BaseModel):
    is_anomalous: bool
    anomaly_count: int
    anomalies: List[float]
    threshold_upper: Optional[float] = None
    model_used: str = "isolation_forest"
    anomaly_indices: List[int] = Field(default_factory=list, description="Indices of anomalous points")

class TrendAnalysis(BaseModel):
    slope: float
    intercept: float
    is_degrading: bool
    description: str
    model_used: str = "holt_winters"
    forecast_next_values: List[float] = Field(default_factory=list, description="Forecast for the next steps")

class AnalysisResult(BaseModel):
    statistics: StatisticalSummary
    anomalies: AnomalyResult
    trend: Optional[TrendAnalysis] = None
    health_score: float = Field(..., ge=0, le=100, description="0 to 100 health score")
