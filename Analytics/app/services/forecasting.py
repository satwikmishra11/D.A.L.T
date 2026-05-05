import numpy as np
from scipy import stats
from statsmodels.tsa.holtwinters import ExponentialSmoothing
from app.models.schemas import TrendAnalysis

def analyze_trend(latencies: list[float], timestamps: list[float] = None) -> TrendAnalysis:
    n = len(latencies)
    if n < 10:
        return TrendAnalysis(
            slope=0, 
            intercept=0, 
            is_degrading=False, 
            description="Insufficient data for forecasting",
            model_used="none",
            forecast_next_values=[]
        )
        
    y = np.array(latencies)
    x = np.array(timestamps) if timestamps else np.arange(n)
    
    # Calculate simple slope for description using linear regression
    slope, intercept, _, _, _ = stats.linregress(x, y)
    
    # Use Holt-Winters for actual forecasting
    try:
        # Simple Exponential Smoothing (if data lacks strong seasonality, we use trend)
        model = ExponentialSmoothing(y, trend='add', seasonal=None, initialization_method="estimated")
        fit_model = model.fit()
        forecast = fit_model.forecast(5).tolist() # Forecast next 5 data points
    except Exception:
        # Fallback if model fails to converge
        forecast = [y[-1] + slope * i for i in range(1, 6)]
    
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
        is_degrading=bool(is_degrading),
        description=description,
        model_used="holt_winters_exponential_smoothing",
        forecast_next_values=[float(f) for f in forecast]
    )
