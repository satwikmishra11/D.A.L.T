import numpy as np
from app.models.schemas import StatisticalSummary

def calculate_statistics(data: list[float]) -> StatisticalSummary:
    arr = np.array(data)
    
    return StatisticalSummary(
        mean=float(np.mean(arr)),
        median=float(np.median(arr)),
        std_dev=float(np.std(arr)),
        p95=float(np.percentile(arr, 95)),
        p99=float(np.percentile(arr, 99)),
        min=float(np.min(arr)),
        max=float(np.max(arr)),
        sample_size=len(arr)
    )
