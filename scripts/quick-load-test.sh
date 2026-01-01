# ========== scripts/quick-load-test.sh ==========
#!/bin/bash

API_BASE="http://localhost:8080/api/v1"
USER_ID="demo-user"

echo "Creating quick load test..."

# Create scenario
SCENARIO=$(curl -s -X POST ${API_BASE}/scenarios \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ${USER_ID}" \
  -d '{
    "name": "Quick Test",
    "targetUrl": "https://httpbin.org/get",
    "method": "GET",
    "durationSeconds": 30,
    "numWorkers": 2,
    "loadProfile": {
      "type": "CONSTANT",
      "initialRps": 50
    }
  }')

SCENARIO_ID=$(echo $SCENARIO | jq -r '.id')
echo "Scenario ID: $SCENARIO_ID"

# Start test
echo "Starting test..."
curl -s -X POST ${API_BASE}/scenarios/${SCENARIO_ID}/start > /dev/null

# Monitor
echo "Monitoring (30 seconds)..."
for i in {1..15}; do
  STATS=$(curl -s ${API_BASE}/scenarios/${SCENARIO_ID}/stats/realtime?lastNSeconds=5)
  
  REQUESTS=$(echo $STATS | jq -r '.totalRequests')
  SUCCESS=$(echo $STATS | jq -r '.successRate')
  LATENCY=$(echo $STATS | jq -r '.avgLatencyMs')
  
  echo "[$i] Requests: $REQUESTS | Success: ${SUCCESS}% | Latency: ${LATENCY}ms"
  
  sleep 2
done

echo "✓ Test complete"