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
    
    # Simple Heuristic Health Score Calculation
    # Start with 100
    # Deduct 20 if p99 > 1000ms (SLA breach example)
    # Deduct 20 if Anomalous
    # Deduct 20 if Degrading
    score = 100.0
    if stats.p99 > 2000: score -= 30
    elif stats.p99 > 1000: score -= 15
    
    if anomalies.is_anomalous:
        score -= min(20, anomalies.anomaly_count * 2)
        
    if trend.is_degrading:
        score -= 20
        
    return AnalysisResult(
        statistics=stats,
        anomalies=anomalies,
        trend=trend,
        health_score=max(0.0, score)
    )

@router.get("/health")
async def health_check():
    return {"status": "ok", "service": "analytics"}
