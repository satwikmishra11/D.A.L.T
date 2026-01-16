from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health_check():
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "analytics"}

def test_analyze_latencies():
    # Generate some synthetic data: mostly 50ms, with one 500ms outlier
    latencies = [50.0] * 50 + [52.0] * 40 + [500.0]
    
    response = client.post(
        "/api/v1/analyze",
        json={"latencies": latencies}
    )
    
    assert response.status_code == 200
    data = response.json()
    
    # Check Statistics
    assert data["statistics"]["min"] == 50.0
    assert data["statistics"]["max"] == 500.0
    assert data["statistics"]["sample_size"] == 91
    
    # Check Anomalies (500.0 should be detected)
    assert data["anomalies"]["is_anomalous"] is True
    assert 500.0 in data["anomalies"]["anomalies"]
    
    # Check Health Score
    assert data["health_score"] < 100
