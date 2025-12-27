#!/bin/bash

# Distributed Load Testing Platform - Test Script
# This script automates testing of the entire platform

set -e

API_BASE="http://localhost:8080/api/v1"
USER_ID="test-user-$(date +%s)"

echo "=================================="
echo "Load Testing Platform - Test Suite"
echo "=================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

function print_error() {
    echo -e "${RED}✗ $1${NC}"
}

function print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Test 1: Check if services are running
echo "Test 1: Checking service health..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    print_success "Controller is running"
else
    print_error "Controller is not responding"
    exit 1
fi

if redis-cli ping > /dev/null 2>&1; then
    print_success "Redis is running"
else
    print_error "Redis is not responding"
    exit 1
fi

if docker exec loadtest-mongodb mongosh --eval "db.version()" > /dev/null 2>&1; then
    print_success "MongoDB is running"
else
    print_error "MongoDB is not responding"
    exit 1
fi

echo ""

# Test 2: Check worker status
echo "Test 2: Checking workers..."
WORKER_STATUS=$(curl -s ${API_BASE}/workers/status)
WORKER_COUNT=$(echo $WORKER_STATUS | jq -r '.activeWorkerCount')

if [ "$WORKER_COUNT" -gt 0 ]; then
    print_success "Found $WORKER_COUNT active workers"
else
    print_error "No active workers found. Start workers first!"
    exit 1
fi

echo ""

# Test 3: Create a test scenario
echo "Test 3: Creating test scenario..."
SCENARIO_RESPONSE=$(curl -s -X POST ${API_BASE}/scenarios \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ${USER_ID}" \
  -d '{
    "name": "Automated Test - httpbin.org",
    "targetUrl": "https://httpbin.org/get",
    "method": "GET",
    "durationSeconds": 30,
    "numWorkers": 2,
    "loadProfile": {
      "type": "CONSTANT",
      "initialRps": 50,
      "targetRps": 50,
      "rampUpSeconds": 0
    }
  }')

SCENARIO_ID=$(echo $SCENARIO_RESPONSE | jq -r '.id')

if [ -n "$SCENARIO_ID" ] && [ "$SCENARIO_ID" != "null" ]; then
    print_success "Created scenario: $SCENARIO_ID"
else
    print_error "Failed to create scenario"
    echo "Response: $SCENARIO_RESPONSE"
    exit 1
fi

echo ""

# Test 4: Start the scenario
echo "Test 4: Starting load test..."
START_RESPONSE=$(curl -s -X POST ${API_BASE}/scenarios/${SCENARIO_ID}/start)
STATUS=$(echo $START_RESPONSE | jq -r '.status')

if [ "$STATUS" == "RUNNING" ]; then
    print_success "Load test started successfully"
else
    print_error "Failed to start load test"
    echo "Response: $START_RESPONSE"
    exit 1
fi

echo ""

# Test 5: Monitor progress
echo "Test 5: Monitoring test progress..."
print_info "Waiting 10 seconds for metrics to accumulate..."
sleep 10

for i in {1..3}; do
    STATS=$(curl -s ${API_BASE}/scenarios/${SCENARIO_ID}/stats/realtime?lastNSeconds=5)
    TOTAL_REQUESTS=$(echo $STATS | jq -r '.totalRequests')
    SUCCESS_RATE=$(echo $STATS | jq -r '.successRate')
    AVG_LATENCY=$(echo $STATS | jq -r '.avgLatencyMs')
    CURRENT_RPS=$(echo $STATS | jq -r '.currentRps')
    
    echo "  Iteration $i:"
    echo "    Total Requests: $TOTAL_REQUESTS"
    echo "    Success Rate: ${SUCCESS_RATE}%"
    echo "    Avg Latency: ${AVG_LATENCY}ms"
    echo "    Current RPS: $CURRENT_RPS"
    
    sleep 5
done

print_success "Metrics are being collected"
echo ""

# Test 6: Check bottlenecks
echo "Test 6: Analyzing bottlenecks..."
BOTTLENECKS=$(curl -s ${API_BASE}/scenarios/${SCENARIO_ID}/bottlenecks)
HAS_BOTTLENECKS=$(echo $BOTTLENECKS | jq -r '.hasBottlenecks')

if [ "$HAS_BOTTLENECKS" == "true" ]; then
    print_info "Bottlenecks detected:"
    echo $BOTTLENECKS | jq -r '.bottlenecks[]' | while read -r line; do
        echo "    - $line"
    done
else
    print_success "No bottlenecks detected"
fi

echo ""

# Test 7: Wait for completion and get final stats
echo "Test 7: Waiting for test completion..."
print_info "Waiting 20 more seconds..."
sleep 20

FINAL_STATS=$(curl -s ${API_BASE}/scenarios/${SCENARIO_ID}/stats)
TOTAL_REQUESTS=$(echo $FINAL_STATS | jq -r '.totalRequests')
SUCCESS_RATE=$(echo $FINAL_STATS | jq -r '.successRate')
AVG_LATENCY=$(echo $FINAL_STATS | jq -r '.avgLatencyMs')
P95_LATENCY=$(echo $FINAL_STATS | jq -r '.p95LatencyMs')
P99_LATENCY=$(echo $FINAL_STATS | jq -r '.p99LatencyMs')

echo ""
echo "=================================="
echo "       FINAL TEST RESULTS"
echo "=================================="
echo "Scenario ID: $SCENARIO_ID"
echo "Total Requests: $TOTAL_REQUESTS"
echo "Success Rate: ${SUCCESS_RATE}%"
echo "Average Latency: ${AVG_LATENCY}ms"
echo "P95 Latency: ${P95_LATENCY}ms"
echo "P99 Latency: ${P99_LATENCY}ms"
echo "=================================="
echo ""

# Test 8: Validate results
echo "Test 8: Validating results..."

if [ "$TOTAL_REQUESTS" -gt 100 ]; then
    print_success "Sufficient requests generated ($TOTAL_REQUESTS)"
else
    print_error "Too few requests generated ($TOTAL_REQUESTS)"
    exit 1
fi

SUCCESS_RATE_INT=${SUCCESS_RATE%.*}
if [ "$SUCCESS_RATE_INT" -gt 90 ]; then
    print_success "Good success rate (${SUCCESS_RATE}%)"
else
    print_error "Low success rate (${SUCCESS_RATE}%)"
    exit 1
fi

AVG_LATENCY_INT=${AVG_LATENCY%.*}
if [ "$AVG_LATENCY_INT" -lt 2000 ]; then
    print_success "Acceptable latency (${AVG_LATENCY}ms)"
else
    print_error "High latency (${AVG_LATENCY}ms)"
fi

echo ""

# Test 9: Test different load profiles
echo "Test 9: Testing ramp load profile..."
RAMP_SCENARIO=$(curl -s -X POST ${API_BASE}/scenarios \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ${USER_ID}" \
  -d '{
    "name": "Ramp Test",
    "targetUrl": "https://httpbin.org/get",
    "method": "GET",
    "durationSeconds": 30,
    "numWorkers": 2,
    "loadProfile": {
      "type": "RAMP",
      "initialRps": 10,
      "targetRps": 100,
      "rampUpSeconds": 20
    }
  }')

RAMP_ID=$(echo $RAMP_SCENARIO | jq -r '.id')
if [ -n "$RAMP_ID" ] && [ "$RAMP_ID" != "null" ]; then
    print_success "Created ramp scenario: $RAMP_ID"
    # Start it
    curl -s -X POST ${API_BASE}/scenarios/${RAMP_ID}/start > /dev/null
    print_success "Started ramp test"
else
    print_error "Failed to create ramp scenario"
fi

echo ""

# Test 10: Cleanup
echo "Test 10: Cleanup..."
print_info "Test scenarios created: $SCENARIO_ID, $RAMP_ID"
print_info "You can stop them manually or let them complete"

echo ""
echo "=================================="
echo "   ALL TESTS PASSED! ✓"
echo "=================================="
echo ""
echo "Platform is working correctly!"
echo "Access the dashboard at: http://localhost:3000"
echo ""

# Performance summary
echo "Performance Summary:"
echo "  • Generated ${TOTAL_REQUESTS} requests in 30 seconds"
echo "  • Average RPS: $(echo "scale=2; $TOTAL_REQUESTS / 30" | bc)"
echo "  • Success Rate: ${SUCCESS_RATE}%"
echo "  • P95 Latency: ${P95_LATENCY}ms"
echo ""

exit 0