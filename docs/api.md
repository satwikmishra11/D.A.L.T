# API Documentation

Base URL: `http://localhost:8080/api/v1`

## Authentication

All requests require `X-User-Id` header:
```
X-User-Id: your-user-id
```

## Endpoints

### Scenarios

#### Create Scenario
```http
POST /scenarios
Content-Type: application/json
X-User-Id: user123

{
  "name": "My Load Test",
  "targetUrl": "https://api.example.com/endpoint",
  "method": "GET|POST|PUT|DELETE|PATCH",
  "headers": {
    "Authorization": "Bearer token"
  },
  "body": "{\"key\": \"value\"}",
  "durationSeconds": 60,
  "numWorkers": 5,
  "loadProfile": {
    "type": "CONSTANT|RAMP|BURST|SPIKE",
    "initialRps": 100,
    "targetRps": 1000,
    "rampUpSeconds": 30,
    "bursts": [
      {
        "startSecond": 30,
        "durationSeconds": 10,
        "rps": 5000
      }
    ]
  }
}

Response: 200 OK
{
  "id": "scenario-uuid",
  "status": "DRAFT",
  "createdAt": "2025-01-15T10:00:00Z",
  ...
}
```

#### List Scenarios
```http
GET /scenarios
X-User-Id: user123

Response: 200 OK
[
  {
    "id": "scenario-1",
    "name": "Test 1",
    "status": "COMPLETED",
    ...
  }
]
```

#### Start Scenario
```http
POST /scenarios/{id}/start

Response: 200 OK
{
  "id": "scenario-uuid",
  "status": "RUNNING",
  "startedAt": "2025-01-15T10:05:00Z"
}
```

#### Stop Scenario
```http
POST /scenarios/{id}/stop

Response: 200 OK
```

#### Get Scenario Stats
```http
GET /scenarios/{id}/stats

Response: 200 OK
{
  "scenarioId": "scenario-uuid",
  "totalRequests": 150000,
  "successfulRequests": 148500,
  "failedRequests": 1500,
  "successRate": 99.0,
  "avgLatencyMs": 45.2,
  "minLatencyMs": 12.0,
  "maxLatencyMs": 1250.0,
  "p50LatencyMs": 38.0,
  "p95LatencyMs": 125.0,
  "p99LatencyMs": 280.0,
  "statusCodeDistribution": {
    "200": 148500,
    "500": 1200,
    "502": 300
  },
  "currentRps": 2500.0,
  "lastUpdated": "2025-01-15T10:15:00Z"
}
```

#### Get Real-time Stats
```http
GET /scenarios/{id}/stats/realtime?lastNSeconds=30

Response: Same as above, but for last N seconds
```

#### Detect Bottlenecks
```http
GET /scenarios/{id}/bottlenecks

Response: 200 OK
{
  "hasBottlenecks": true,
  "bottlenecks": [
    "High error rate: 5.23%",
    "High P95 latency: 1250ms",
    "Inconsistent latency (P99 >> P95)"
  ],
  "stats": { ... }
}
```

### Workers

#### Get Worker Status
```http
GET /workers/status

Response: 200 OK
{
  "activeWorkerCount": 10,
  "activeWorkerIds": [
    "worker-1",
    "worker-2",
    ...
  ],
  "taskQueueSize": 0,
  "resultQueueSize": 5
}
```

#### Get Worker Heartbeat
```http
GET /workers/{workerId}/heartbeat

Response: 200 OK
{
  "workerId": "worker-1",
  "timestamp": "2025-01-15T10:00:00Z",
  "status": "BUSY",
  "currentTaskId": "task-123",
  "requestsProcessed": 15420
}
```

#### Submit Heartbeat (Called by workers)
```http
POST /workers/heartbeat
Content-Type: application/json

{
  "workerId": "worker-1",
  "timestamp": "2025-01-15T10:00:00Z",
  "status": "IDLE|BUSY|ERROR|OFFLINE",
  "currentTaskId": "task-123",
  "requestsProcessed": 15420
}

Response: 200 OK
```

### Metrics

#### Submit Batch Metrics (Called by workers)
```http
POST /metrics/batch
Content-Type: application/json

[
  {
    "taskId": "task-123",
    "workerId": "worker-1",
    "timestamp": "2025-01-15T10:00:00Z",
    "totalRequests": 100,
    "successCount": 98,
    "errorCount": 2,
    "avgLatencyMs": 45.2,
    "p95LatencyMs": 125.0,
    "p99LatencyMs": 280.0
  }
]

Response: 200 OK
```

## WebSocket

### Connect to Real-time Stream

```javascript
const socket = new WebSocket('ws://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Subscribe to scenario metrics
  stompClient.subscribe('/topic/metrics/{scenarioId}', (message) => {
    const stats = JSON.parse(message.body);
    console.log('Real-time stats:', stats);
  });
  
  // Subscribe to worker status
  stompClient.subscribe('/topic/workers/status', (message) => {
    const status = JSON.parse(message.body);
    console.log('Worker status:', status);
  });
});
```

## Status Codes

| Code | Status | Description |
|------|--------|-------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created |
| 400 | Bad Request | Invalid parameters |
| 404 | Not Found | Resource not found |
| 500 | Internal Server Error | Server error |

## Load Profile Types

### CONSTANT
Fixed RPS throughout the test.
```json
{
  "type": "CONSTANT",
  "initialRps": 1000
}
```

### RAMP
Gradually increase from initial to target RPS.
```json
{
  "type": "RAMP",
  "initialRps": 100,
  "targetRps": 1000,
  "rampUpSeconds": 60
}
```

### BURST
Base load with periodic traffic spikes.
```json
{
  "type": "BURST",
  "initialRps": 500,
  "bursts": [
    {
      "startSecond": 30,
      "durationSeconds": 10,
      "rps": 5000
    }
  ]
}
```

### SPIKE
Single large traffic spike.
```json
{
  "type": "SPIKE",
  "initialRps": 100,
  "bursts": [
    {
      "startSecond": 30,
      "durationSeconds": 5,
      "rps": 10000
    }
  ]
}
```

## Examples

### Example 1: Run Simple Test

```bash
# 1. Create scenario
SCENARIO=$(curl -s -X POST http://localhost:8080/api/v1/scenarios \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user123" \
  -d '{
    "name": "Quick Test",
    "targetUrl": "https://httpbin.org/get",
    "method": "GET",
    "durationSeconds": 30,
    "numWorkers": 2,
    "loadProfile": {
      "type": "CONSTANT",
      "initialRps": 100
    }
  }')

SCENARIO_ID=$(echo $SCENARIO | jq -r '.id')

# 2. Start test
curl -X POST http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/start

# 3. Monitor (every 5 seconds)
watch -n 5 "curl -s http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/stats/realtime | jq"
```

### Example 2: POST Request with Authentication

```bash
curl -X POST http://localhost:8080/api/v1/scenarios \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user123" \
  -d '{
    "name": "API Test",
    "targetUrl": "https://api.example.com/users",
    "method": "POST",
    "headers": {
      "Authorization": "Bearer YOUR_TOKEN",
      "Content-Type": "application/json"
    },
    "body": "{\"name\":\"Test User\",\"email\":\"test@example.com\"}",
    "durationSeconds": 60,
    "numWorkers": 5,
    "loadProfile": {
      "type": "RAMP",
      "initialRps": 50,
      "targetRps": 500,
      "rampUpSeconds": 30
    }
  }'
```

### Example 3: Check for SLA Violations

```bash
# Get bottlenecks
BOTTLENECKS=$(curl -s http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/bottlenecks)

# Check if any violations
HAS_ISSUES=$(echo $BOTTLENECKS | jq -r '.hasBottlenecks')

if [ "$HAS_ISSUES" == "true" ]; then
  echo "⚠️ Performance issues detected:"
  echo $BOTTLENECKS | jq -r '.bottlenecks[]'
else
  echo "✅ All metrics within acceptable range"
fi
```

## Rate Limits

| Endpoint | Limit | Window |
|----------|-------|--------|
| Create Scenario | 10 | 1 minute |
| Start Scenario | 5 | 1 minute |
| Get Stats | 100 | 1 minute |
| Worker Heartbeat | Unlimited | - |

## Error Codes

### 4xx Client Errors

```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid RPS value: must be positive integer",
  "timestamp": "2025-01-15T10:00:00Z"
}
```

Common errors:
- `INVALID_PARAMETERS`: Request validation failed
- `RESOURCE_NOT_FOUND`: Scenario/worker not found
- `INSUFFICIENT_WORKERS`: Not enough workers available
- `SCENARIO_ALREADY_RUNNING`: Cannot start running scenario

### 5xx Server Errors

```json
{
  "error": "INTERNAL_SERVER_ERROR",
  "message": "Failed to connect to Redis",
  "timestamp": "2025-01-15T10:00:00Z"
}
```