 ========== scripts/load-generator.py ==========
#!/usr/bin/env python3
"""
Quick load test generator for testing the platform
"""

import requests
import time
import json
from datetime import datetime

API_BASE = "http://localhost:8080/api/v1"
USER_ID = "test-user"

def create_scenario(name, url, rps, duration):
    """Create a test scenario"""
    payload = {
        "name": name,
        "targetUrl": url,
        "method": "GET",
        "durationSeconds": duration,
        "numWorkers": 3,
        "loadProfile": {
            "type": "CONSTANT",
            "initialRps": rps
        }
    }
    
    response = requests.post(
        f"{API_BASE}/scenarios",
        headers={"X-User-Id": USER_ID},
        json=payload
    )
    
    return response.json()["id"]

def start_scenario(scenario_id):
    """Start a test scenario"""
    response = requests.post(f"{API_BASE}/scenarios/{scenario_id}/start")
    return response.json()

def get_stats(scenario_id):
    """Get scenario statistics"""
    response = requests.get(f"{API_BASE}/scenarios/{scenario_id}/stats/realtime?lastNSeconds=10")
    return response.json()

def main():
    print("=== Load Test Generator ===")
    print()
    
    # Create scenario
    print("Creating scenario...")
    scenario_id = create_scenario(
        name=f"Auto Test {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        url="https://httpbin.org/get",
        rps=100,
        duration=60
    )
    print(f"✓ Scenario created: {scenario_id}")
    
    # Start scenario
    print("\nStarting test...")
    start_scenario(scenario_id)
    print("✓ Test started")
    
    # Monitor progress
    print("\nMonitoring (press Ctrl+C to stop):")
    print("-" * 80)
    
    try:
        while True:
            stats = get_stats(scenario_id)
            
            print(f"\rRequests: {stats['totalRequests']:,} | "
                  f"Success Rate: {stats['successRate']:.2f}% | "
                  f"Avg Latency: {stats['avgLatencyMs']:.0f}ms | "
                  f"P95: {stats['p95LatencyMs']:.0f}ms | "
                  f"Current RPS: {stats['currentRps']:.0f}",
                  end='', flush=True)
            
            time.sleep(2)
            
    except KeyboardInterrupt:
        print("\n\nTest monitoring stopped")

if __name__ == "__main__":
    main()