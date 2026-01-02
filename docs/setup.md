# Distributed Load Testing Platform - Setup Guide

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for local development)
- Rust 1.70+ (for local development)
- Node.js 18+ (for local development)

### 1. Clone and Setup

```bash
# Create project structure
mkdir loadtest-platform && cd loadtest-platform
mkdir -p controller/src/main/java/com/loadtest
mkdir -p worker/src
mkdir -p frontend/src
```

### 2. Start Infrastructure

```bash
# Start MongoDB and Redis
docker-compose up -d mongodb redis

# Verify services
docker ps
```

### 3. Run Spring Boot Controller

```bash
cd controller

# Build with Maven
mvn clean install

# Run
mvn spring-boot:run

# Or using JAR
java -jar target/controller-1.0.0.jar
```

**Controller will be available at:** `http://localhost:8080`

### 4. Run Rust Workers

```bash
cd worker

# Build
cargo build --release

# Run multiple workers
./target/release/loadtest-worker &
./target/release/loadtest-worker &
./target/release/loadtest-worker &

# Or with Docker
docker-compose up -d --scale worker=5
```

### 5. Run React Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

**Frontend will be available at:** `http://localhost:3000`

---

## 📦 Docker Deployment

### Build and Run Everything

```bash
# Build all services
docker-compose build

# Start with 5 workers
docker-compose up -d --scale worker=5

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### Scale Workers Dynamically

```bash
# Scale to 10 workers
docker-compose up -d --scale worker=10

# Scale down to 2 workers
docker-compose up -d --scale worker=2
```

---

## 🔧 Configuration

### Environment Variables

Create `.env` file in project root:

```env
# MongoDB
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=your_secure_password

# Supabase Auth
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_JWT_SECRET=your-jwt-secret

# Redis (optional custom config)
REDIS_PASSWORD=your_redis_password
```

### Application Configuration

Edit `controller/src/main/resources/application.yml`:

```yaml
loadtest:
  worker:
    timeout: 30000
    heartbeat-interval: 5000
  redis:
    queue:
      tasks: "loadtest:tasks"
      results: "loadtest:results"
```

---

## 🧪 Testing the Platform

### 1. Check System Health

```bash
# Check workers
curl http://localhost:8080/api/v1/workers/status

# Expected response:
{
  "activeWorkerCount": 5,
  "taskQueueSize": 0,
  "resultQueueSize": 0
}
```

### 2. Create a Test Scenario

```bash
curl -X POST http://localhost:8080/api/v1/scenarios \
  -H "Content-Type: application/json" \
  -H "X-User-Id: demo-user" \
  -d '{
    "name": "API Test 1",
    "targetUrl": "https://httpbin.org/get",
    "method": "GET",
    "durationSeconds": 60,
    "numWorkers": 3,
    "loadProfile": {
      "type": "CONSTANT",
      "initialRps": 100,
      "targetRps": 100,
      "rampUpSeconds": 0
    }
  }'
```

### 3. Start the Test

```bash
# Get scenario ID from previous response
SCENARIO_ID="your-scenario-id"

curl -X POST http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/start
```

### 4. Monitor Results

```bash
# Get real-time stats
curl http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/stats/realtime

# Check for bottlenecks
curl http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/bottlenecks
```

---

## 📊 Performance Benchmarks

### Expected Performance

- **Single Worker:** 5,000 - 10,000 RPS
- **5 Workers:** 25,000 - 50,000 RPS
- **10 Workers:** 50,000 - 100,000 RPS
- **Latency Overhead:** < 5ms (worker processing)

### Scaling Guidelines

| Target RPS | Recommended Workers | CPU Cores | RAM |
|-----------|-------------------|-----------|-----|
| 10K       | 2-3               | 2         | 4GB |
| 50K       | 5-7               | 4         | 8GB |
| 100K      | 10-15             | 8         | 16GB |
| 500K+     | 50+               | 32+       | 64GB+ |

---

## 🛠️ Troubleshooting

### Workers Not Connecting

```bash
# Check Redis connectivity
redis-cli -h localhost ping
# Should return: PONG

# Check worker logs
docker-compose logs worker
```

### High Latency

```bash
# Check MongoDB performance
docker exec loadtest-mongodb mongo --eval "db.serverStatus()"

# Check Redis queue size
redis-cli LLEN loadtest:tasks
redis-cli LLEN loadtest:results
```

### Memory Issues

```bash
# Increase Docker memory limits
# Edit docker-compose.yml:

services:
  worker:
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 512M
```

---

## 🔐 Production Deployment

### 1. Enable Authentication

Configure Supabase in `application.yml`:

```yaml
loadtest:
  supabase:
    url: ${SUPABASE_URL}
    jwt-secret: ${SUPABASE_JWT_SECRET}
```

### 2. Secure MongoDB

```yaml
mongodb:
  environment:
    MONGO_INITDB_ROOT_USERNAME: admin
    MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD}
```

### 3. Enable Redis Auth

```yaml
redis:
  command: redis-server --requirepass ${REDIS_PASSWORD}
```

### 4. Use Reverse Proxy

```nginx
# nginx.conf
upstream backend {
    server controller:8080;
}

server {
    listen 80;
    server_name loadtest.yourdomain.com;

    location /api {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        proxy_pass http://frontend:80;
    }
}
```

---

## 📈 Monitoring

### View Metrics in Real-time

```bash
# Watch worker count
watch -n 1 'curl -s http://localhost:8080/api/v1/workers/status | jq'

# Monitor Redis queues
watch -n 1 'redis-cli LLEN loadtest:tasks && redis-cli LLEN loadtest:results'

# Monitor MongoDB
docker exec -it loadtest-mongodb mongo --eval "db.metrics.stats()"
```

### Export Metrics

```bash
# Export scenario results to JSON
curl http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/stats > results.json
```

---

## 🎯 Advanced Features

### Custom Load Profiles

**Ramp Load:**
```json
{
  "loadProfile": {
    "type": "RAMP",
    "initialRps": 100,
    "targetRps": 1000,
    "rampUpSeconds": 60
  }
}
```

**Burst Load:**
```json
{
  "loadProfile": {
    "type": "BURST",
    "initialRps": 100,
    "bursts": [
      {
        "startSecond": 30,
        "durationSeconds": 10,
        "rps": 5000
      }
    ]
  }
}
```

### SLA Monitoring

```bash
# Check SLA violations
curl -X GET 'http://localhost:8080/api/v1/scenarios/$SCENARIO_ID/bottlenecks'
```

---

## 🤝 Contributing

### Development Setup

```bash
# Run tests
cd controller && mvn test
cd worker && cargo test

# Format code
cd controller && mvn spotless:apply
cd worker && cargo fmt
```

---

## 📝 Resume Bullet Point

```
Implemented a distributed API load-testing platform with Rust/Go workers 
and Spring Boot control plane, generating 100K+ RPS and providing real-time 
observability dashboards for latency and error analysis.
```

---

## 🎓 Learning Outcomes

- ✅ Distributed systems architecture
- ✅ Message queue coordination (Redis)
- ✅ High-performance concurrent programming (Rust)
- ✅ Real-time metrics aggregation
- ✅ WebSocket streaming
- ✅ Microservices orchestration
- ✅ Docker containerization
- ✅ Performance optimization

---

**Need help?** Open an issue or contact support@loadtest.dev