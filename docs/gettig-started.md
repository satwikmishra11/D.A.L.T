# Getting Started with Load Testing Platform

This tutorial will walk you through setting up and running your first load test in 10 minutes.

## Prerequisites Checklist

- [ ] Docker installed (20.10+)
- [ ] Docker Compose installed (2.0+)
- [ ] 4GB RAM available
- [ ] 10GB disk space
- [ ] Port 8080, 3000, 6379, 27017 available

## Part 1: Installation (5 minutes)

### Step 1: Create Project Directory

```bash
mkdir loadtest-platform
cd loadtest-platform
```

### Step 2: Create File Structure

```bash
mkdir -p controller/src/main/java/com/loadtest/{model,repository,service,controller}
mkdir -p controller/src/main/resources
mkdir -p worker/src
mkdir -p frontend/src
```

### Step 3: Copy Files

Copy all the provided code files into their respective directories:

**Controller Files:**
- `pom.xml` → `controller/`
- `application.yml` → `controller/src/main/resources/`
- Java files → `controller/src/main/java/com/loadtest/`

**Worker Files:**
- `Cargo.toml` → `worker/`
- Rust files → `worker/src/`

**Frontend Files:**
- `LoadTestDashboard.jsx` → `frontend/src/`
- `package.json` → `frontend/`

**Root Files:**
- `docker-compose.yml` → project root
- `test-platform.sh` → project root

### Step 4: Start Services

```bash
# Start infrastructure only
docker-compose up -d mongodb redis

# Wait 10 seconds for services to be ready
sleep 10

# Verify services
docker-compose ps
```

Expected output:
```
NAME                    STATUS    PORTS
loadtest-mongodb        Up        0.0.0.0:27017->27017/tcp
loadtest-redis          Up        0.0.0.0:6379->6379/tcp
```

## Part 2: Running Your First Test (5 minutes)

### Method A: Using Docker (Recommended)

```bash
# Start everything with 3 workers
docker-compose up -d --scale worker=3

# Check status
docker-compose ps

# View logs
docker-compose logs -f controller worker
```

### Method B: Running Locally

**Terminal 1 - Controller:**
```bash
cd controller
mvn spring-boot:run
```

**Terminal 2 - Worker 1:**
```bash
cd worker
cargo run
```

**Terminal 3 - Worker 2:**
```bash
cd worker
cargo run
```

**Terminal 4 - Frontend:**
```bash
cd frontend
npm install
npm start
```

### Step 5: Verify Setup

```bash
# Check workers
curl http://localhost:8080/api/v1/workers/status

# Expected: { "activeWorkerCount": 3, ... }
```

### Step 6: Create Your First Test

```bash
curl -X POST http://localhost:8080/api/v1/scenarios \
  -H "Content-Type: application/json" \
  -H "X-User-Id: tutorial-user" \
  -d '{
    "name": "My First Load Test",
    "targetUrl": "https://httpbin.org/get",
    "method": "GET",
    "durationSeconds": 30,
    "numWorkers": 2,
    "loadProfile": {
      "type": "CONSTANT",
      "initialRps": 50
    }
  }'
```

Save the returned `id` value.

### Step 7: Start the Test

```bash
# Replace {id} with your scenario ID
SCENARIO_ID="your-scenario-id"

curl -X POST http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/start
```

### Step 8: Watch It Run

**Option 1 - Dashboard:**
Open `http://localhost:3000` in your browser

**Option 2 - Command Line:**
```bash
# Real-time stats (updates every 2 seconds)
watch -n 2 "curl -s http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/stats/realtime | jq"
```

### Step 9: View Results

```bash
# Wait 30 seconds for test to complete, then:
curl -s http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/stats | jq
```

Example output:
```json
{
  "scenarioId": "abc-123",
  "totalRequests": 1500,
  "successfulRequests": 1485,
  "successRate": 99.0,
  "avgLatencyMs": 42.3,
  "p95LatencyMs": 85.0,
  "p99LatencyMs": 125.0
}
```

## Part 3: Understanding the Results

### Key Metrics Explained

1. **Total Requests**: How many HTTP requests were sent
   - Your test: 30 seconds × 50 RPS = ~1,500 requests

2. **Success Rate**: Percentage of successful requests (200-299 status)
   - Good: > 99%
   - Acceptable: 95-99%
   - Poor: < 95%

3. **Average Latency**: Mean response time
   - Excellent: < 50ms
   - Good: 50-200ms
   - Slow: > 200ms

4. **P95/P99 Latency**: 95th/99th percentile response times
   - Shows worst-case performance
   - Important for user experience

### Interpreting Results

**Scenario 1: Everything Good ✅**
```
Success Rate: 99.8%
Avg Latency: 45ms
P95: 85ms
P99: 125ms
```
→ Your API is healthy!

**Scenario 2: High Error Rate ⚠️**
```
Success Rate: 85%
Errors: 15% (500, 502, 503)
```
→ Server can't handle load, check logs

**Scenario 3: High Latency ⚠️**
```
Avg Latency: 250ms
P95: 800ms
P99: 2000ms
```
→ Bottleneck detected, investigate database/network

## Part 4: Advanced Tests

### Test 1: Ramp-Up Test (Gradual Increase)

```bash
curl -X POST http://localhost:8080/api/v1/scenarios \
  -H "Content-Type: application/json" \
  -H "X-User-Id: tutorial-user" \
  -d '{
    "name": "Ramp Test",
    "targetUrl": "https://httpbin.org/get",
    "method": "GET",
    "durationSeconds": 60,
    "numWorkers": 3,
    "loadProfile": {
      "type": "RAMP",
      "initialRps": 10,
      "targetRps": 200,
      "rampUpSeconds": 45
    }
  }'
```

This test simulates gradual traffic increase (like a product launch).

### Test 2: Burst Test (Traffic Spikes)

```bash
curl -X POST http://localhost:8080/api/v1/scenarios \
  -H "Content-Type: application/json" \
  -H "X-User-Id: tutorial-user" \
  -d '{
    "name": "Burst Test",
    "targetUrl": "https://httpbin.org/get",
    "method": "GET",
    "durationSeconds": 60,
    "numWorkers": 3,
    "loadProfile": {
      "type": "BURST",
      "initialRps": 50,
      "bursts": [
        {
          "startSecond": 20,
          "durationSeconds": 5,
          "rps": 500
        },
        {
          "startSecond": 40,
          "durationSeconds": 5,
          "rps": 500
        }
      ]
    }
  }'
```

This simulates sudden traffic spikes (like email campaigns).

### Test 3: Testing Your Own API

```bash
curl -X POST http://localhost:8080/api/v1/scenarios \
  -H "Content-Type: application/json" \
  -H "X-User-Id: tutorial-user" \
  -d '{
    "name": "My API Test",
    "targetUrl": "http://your-api.com/endpoint",
    "method": "POST",
    "headers": {
      "Authorization": "Bearer YOUR_TOKEN",
      "Content-Type": "application/json"
    },
    "body": "{\"key\": \"value\"}",
    "durationSeconds": 60,
    "numWorkers": 5,
    "loadProfile": {
      "type": "CONSTANT",
      "initialRps": 100
    }
  }'
```

## Part 5: Scaling Up

### Scale to 10 Workers (10K RPS)

```bash
docker-compose up -d --scale worker=10

# Verify
curl http://localhost:8080/api/v1/workers/status
```

### Scale to 20 Workers (20K RPS)

```bash
docker-compose up -d --scale worker=20
```

### Performance Tips

1. **For 50K+ RPS**: Use 10+ workers
2. **For low-latency APIs**: Increase worker count
3. **For high-latency APIs**: Fewer workers, higher RPS per worker

## Part 6: Troubleshooting

### Problem 1: Workers Not Starting

```bash
# Check Docker
docker-compose ps

# Check logs
docker-compose logs worker

# Common fix: Restart
docker-compose restart worker
```

### Problem 2: High Error Rates

```bash
# Check target API status
curl -I https://httpbin.org/get

# Reduce RPS
# Edit scenario with lower initialRps
```

### Problem 3: No Metrics Appearing

```bash
# Check MongoDB
docker exec loadtest-mongodb mongosh --eval "db.metrics.count()"

# Check Redis
docker exec loadtest-redis redis-cli LLEN loadtest:results

# Restart controller
docker-compose restart controller
```

### Problem 4: Dashboard Not Loading

```bash
# Check frontend logs
docker-compose logs frontend

# Rebuild frontend
docker-compose build frontend
docker-compose up -d frontend
```

## Part 7: Cleanup

### Stop Everything

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clears data)
docker-compose down -v
```

### Stop Specific Components

```bash
# Stop only workers
docker-compose stop worker

# Remove workers
docker-compose rm -f worker
```

## Next Steps

Now that you've completed the tutorial, try:

1. ✅ Test your own API endpoints
2. ✅ Try different load profiles (ramp, burst)
3. ✅ Scale to 10+ workers for high throughput
4. ✅ Set up monitoring with Grafana
5. ✅ Integrate with CI/CD pipeline
6. ✅ Run multi-hour endurance tests

## Quick Reference Commands

```bash
# Start platform
docker-compose up -d --scale worker=5

# Create test
curl -X POST http://localhost:8080/api/v1/scenarios -H "Content-Type: application/json" -H "X-User-Id: user" -d @scenario.json

# Start test
curl -X POST http://localhost:8080/api/v1/scenarios/{id}/start

# View results
curl http://localhost:8080/api/v1/scenarios/{id}/stats | jq

# Scale workers
docker-compose up -d --scale worker=10

# View logs
docker-compose logs -f

# Stop platform
docker-compose down
```

## Getting Help

- **Documentation**: See `README.md` and `API.md`
- **Test Script**: Run `./test-platform.sh` for automated testing
- **Logs**: `docker-compose logs -f [service]`
- **Issues**: Check GitHub issues

**Congratulations! You've successfully set up and run your first load test.** 🎉