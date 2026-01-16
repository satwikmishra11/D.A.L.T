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

class AnomalyResult(BaseModel):
    is_anomalous: bool
    anomaly_count: int
    anomalies: List[float]
    threshold_upper: float

class TrendAnalysis(BaseModel):
    slope: float
    intercept: float
    is_degrading: bool
    description: str

class AnalysisResult(BaseModel):
    statistics: StatisticalSummary
    anomalies: AnomalyResult
    trend: Optional[TrendAnalysis] = None
    health_score: float = Field(..., ge=0, le=100, description="0 to 100 health score")
