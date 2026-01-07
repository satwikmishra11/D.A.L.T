from fastapi import FastAPI
from analyzer import detect_latency_anomaly

app = FastAPI()

@app.post("/analyze/latency")
def analyze(data: dict):
    return detect_latency_anomaly(data["latencies"])

if (scenario.isLocked()) {
    throw new RuntimeException("Scenario locked during execution");
}

