import numpy as np

def detect_latency_anomaly(latencies):
    mean = np.mean(latencies)
    std = np.std(latencies)

    anomalies = [
        l for l in latencies
        if abs(l - mean) > 3 * std
    ]

    return {
        "mean": mean,
        "std": std,
        "anomalies": anomalies,
        "anomaly_count": len(anomalies)
    }
