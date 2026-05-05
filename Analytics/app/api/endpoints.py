from fastapi import APIRouter, HTTPException
from app.models.schemas import LatencyInput, AnalysisResult
from app.services.statistics import calculate_statistics
from app.services.anomaly import detect_anomalies
from app.services.forecasting import analyze_trend

router = APIRouter()

@router.post("/analyze", response_model=AnalysisResult)
async def analyze_latencies(payload: LatencyInput):
    if not payload.latencies:
        raise HTTPException(status_code=400, detail="Latencies list cannot be empty")

    stats = calculate_statistics(payload.latencies)
    anomalies = detect_anomalies(payload.latencies)
    trend = analyze_trend(payload.latencies, payload.timestamps)
    
    # Professional Heuristic Health Score Calculation
    # Base score on Apdex
    score = stats.apdex_score * 100.0
    
    # Deduct points for anomalies found by Isolation Forest
    if anomalies.is_anomalous:
        score -= min(30.0, anomalies.anomaly_count * 2.5)
        
    # Deduct points if Holt-Winters forecasts a degrading trend
    if trend.is_degrading:
        score -= 15.0
        
    return AnalysisResult(
        statistics=stats,
        anomalies=anomalies,
        trend=trend,
        health_score=max(0.0, min(100.0, score))
    )

@router.get("/health")
async def health_check():
    return {"status": "ok", "service": "analytics"}
