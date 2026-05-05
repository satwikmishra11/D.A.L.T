import numpy as np
from app.models.schemas import StatisticalSummary

def calculate_statistics(data: list[float], apdex_t: float = 500.0) -> StatisticalSummary:
    arr = np.array(data)
    
    # Calculate Apdex score
    # T is the target response time (default 500ms)
    # Satisfied: < T, Tolerating: T to 4T, Frustrated: > 4T
    satisfied = np.sum(arr <= apdex_t)
    tolerating = np.sum((arr > apdex_t) & (arr <= 4 * apdex_t))
    total = len(arr)
    
    apdex_score = (satisfied + (tolerating / 2.0)) / total if total > 0 else 0.0
    
    return StatisticalSummary(
        mean=float(np.mean(arr)),
        median=float(np.median(arr)),
        std_dev=float(np.std(arr)),
        p95=float(np.percentile(arr, 95)),
        p99=float(np.percentile(arr, 99)),
        min=float(np.min(arr)),
        max=float(np.max(arr)),
        sample_size=total,
        apdex_score=float(apdex_score)
    )
