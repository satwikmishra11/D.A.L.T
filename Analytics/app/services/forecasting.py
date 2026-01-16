import numpy as np
from scipy import stats
from app.models.schemas import TrendAnalysis

def analyze_trend(latencies: list[float], timestamps: list[float] = None) -> TrendAnalysis:
    n = len(latencies)
    if n < 2:
        return TrendAnalysis(slope=0, intercept=0, is_degrading=False, description="Insufficient data")
        
    # If no timestamps, assume uniform intervals (index as time)
    x = np.array(timestamps) if timestamps else np.arange(n)
    y = np.array(latencies)
    
    slope, intercept, r_value, p_value, std_err = stats.linregress(x, y)
    
    # Interpretation
    is_degrading = slope > 0.1 # Threshold: growing by 0.1ms per unit time
    description = "Stable"
    
    if slope > 0.5:
        description = "Rapidly Degrading"
    elif slope > 0:
        description = "Slightly Degrading"
    elif slope < 0:
        description = "Improving"
        
    return TrendAnalysis(
        slope=float(slope),
        intercept=float(intercept),
        is_degrading=is_degrading,
        description=description
    )
