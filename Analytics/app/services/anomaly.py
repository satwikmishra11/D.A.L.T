import numpy as np
from sklearn.ensemble import IsolationForest
from app.core.config import settings
from app.models.schemas import AnomalyResult

def detect_anomalies(data: list[float]) -> AnomalyResult:
    if len(data) < 10:
        # Fallback to simple mean if not enough data
        return AnomalyResult(
            is_anomalous=False, 
            anomaly_count=0, 
            anomalies=[], 
            model_used="none",
            anomaly_indices=[]
        )
        
    arr = np.array(data).reshape(-1, 1)
    
    # Advanced Machine Learning Anomaly Detection
    # Contamination defines the expected proportion of outliers (e.g., 5%)
    model = IsolationForest(n_estimators=100, contamination=0.05, random_state=42)
    predictions = model.fit_predict(arr)
    
    # -1 indicates an anomaly, 1 indicates normal
    anomaly_mask = predictions == -1
    anomalous_values = arr[anomaly_mask].flatten().tolist()
    anomaly_indices = np.where(anomaly_mask)[0].tolist()
    
    return AnomalyResult(
        is_anomalous=len(anomalous_values) > 0,
        anomaly_count=len(anomalous_values),
        anomalies=anomalous_values,
        threshold_upper=None, # Isolation Forest doesn't use a simple threshold
        model_used="isolation_forest",
        anomaly_indices=anomaly_indices
    )
