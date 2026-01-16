import numpy as np
from app.core.config import settings
from app.models.schemas import AnomalyResult

def detect_anomalies(data: list[float]) -> AnomalyResult:
    arr = np.array(data)
    mean = np.mean(arr)
    std = np.std(arr)
    
    # Z-Score method
    threshold = settings.ANOMALY_STD_DEV_THRESHOLD
    upper_limit = mean + (threshold * std)
    
    # We generally care about high latency anomalies in load testing
    anomalies = arr[arr > upper_limit].tolist()
    
    return AnomalyResult(
        is_anomalous=len(anomalies) > 0,
        anomaly_count=len(anomalies),
        anomalies=anomalies,
        threshold_upper=float(upper_limit)
    )
