# System Architecture Documentation

## Overview

LoadTest Platform is a distributed, cloud-native load testing system designed for enterprise-scale performance testing. The architecture follows microservices principles with clear separation of concerns and horizontal scalability.

## Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                      External Users / CI/CD                     │
└────────────┬───────────────────────────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────────────────────┐
│                    Ingress / Load Balancer                      │
│                    (nginx / AWS ALB)                            │
└────────┬──────────────────────────────────┬────────────────────┘
         │                                  │
         │ HTTP/REST                        │ WebSocket
         ▼                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend (React SPA)                          │
│  • Dashboard UI        • Real-time Charts    • Test Management  │
│  • Authentication      • Export/Reports      • WebSocket Client │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼ REST API + WebSocket
┌─────────────────────────────────────────────────────────────────┐
│              Spring Boot Control Plane (8080)                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Controllers                                               │  │
│  │  • ScenarioController  • MetricsController               │  │
│  │  • WorkerController    • ExportController                │  │
│  │  • AuthController      • DashboardController             │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Services                                                  │  │
│  │  • LoadTestOrchestrationService                          │  │
│  │  • MetricsAggregationService                             │  │
│  │  • RedisQueueService                                     │  │
│  │  • AlertService                                          │  │
│  │  • ExportService                                         │  │
│  │  • WebSocketMetricsStreamer                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Security Layer (JWT + Spring Security)                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└────┬────────────────────────────┬──────────────────────────┬───┘
     │                            │                          │
     │                            │                          │
     ▼                            ▼                          ▼
┌─────────────┐           ┌──────────────┐         ┌──────────────┐
│   MongoDB   │           │    Redis     │         │  Prometheus  │
│   (27017)   │           │   (6379)     │         │   (9090)     │
│             │           │              │         │              │
│ • Scenarios │           │ • Task Queue │         │ • Metrics    │
│ • Metrics   │           │ • Results Q  │         │ • Monitoring │
│ • Users     │           │ • Heartbeats │         │              │
│ • Alerts    │           │ • Pub/Sub    │         │              │
└─────────────┘           └──────┬───────┘         └──────────────┘
                                 │
                                 │ Redis Pub/Sub
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
         ▼                       ▼                       ▼
    ┌─────────┐            ┌─────────┐            ┌─────────┐
    │Worker 1 │            │Worker 2 │            │Worker N │
    │ (Rust)  │            │ (Rust)  │            │ (Rust)  │
    └────┬────┘            └────┬────┘            └────┬────┘
         │                      │                      │
         └──────────────────────┴──────────────────────┘
                                │
                                │ HTTP Load
                                ▼
                    ┌──────────────────────┐
                    │   Target APIs        │
                    │   (Systems Under     │
                    │    Test)             │
                    └──────────────────────┘
```

## Component Details

### 1. Frontend Layer

**Technology**: React 18 + Tailwind CSS

**Responsibilities**:
- User interface for test management
- Real-time metrics visualization
- WebSocket client for live updates
- Authentication and session management
- Export and reporting interface

**Key Features**:
- Responsive design (mobile/desktop)
- Real-time charts (Recharts library)
- WebSocket auto-reconnection
- Local state management with hooks
- Component-based architecture

### 2. Control Plane (Spring Boot)

**Technology**: Java 17 + Spring Boot 3.2

**Responsibilities**:
- Test orchestration and coordination
- Worker task distribution
- Metrics aggregation and analysis
- User authentication and authorization
- Alert generation and notification
- Data persistence coordination
- WebSocket message broadcasting

**Key Components**:

#### Controllers (REST API)
- `/api/v1/scenarios` - Test scenario CRUD
- `/api/v1/workers` - Worker management
- `/api/v1/metrics` - Metrics collection
- `/api/v1/export` - Report generation
- `/api/v1/auth` - Authentication
- `/api/v1/dashboard` - Summary data

#### Services
- **LoadTestOrchestrationService**: Main orchestrator
- **MetricsAggregationService**: Real-time aggregation
- **RedisQueueService**: Message queue operations
- **AlertService**: Multi-channel notifications
- **ExportService**: Report generation (JSON/CSV/HTML/PDF)

#### Security
- JWT-based authentication
- Role-based access control (RBAC)
- Supabase integration (optional)
- Request validation
- CORS configuration

### 3. Workers (Rust)

**Technology**: Rust + Tokio (async runtime)

**Responsibilities**:
- HTTP request execution
- Rate limiting and pacing
- Metrics collection
- Result reporting
- Heartbeat transmission

**Key Features**:
- Asynchronous I/O (10K+ concurrent connections)
- Efficient memory usage (~50MB per worker)
- Built-in retry logic with exponential backoff
- Connection pooling (100 connections per worker)
- Token bucket rate limiting
- P50/P95/P99 latency calculation

**Performance Characteristics**:
- 5,000-10,000 RPS per worker
- < 5ms processing overhead
- Sub-second startup time
- Graceful shutdown

### 4. Data Layer

#### MongoDB (Primary Storage)

**Collections**:
- `scenarios` - Test configurations
- `metrics` - Time-series performance data
- `users` - User accounts and quotas
- `alerts` - Alert history
- `scheduled_tests` - Cron-based tests
- `reports` - Generated reports

**Indexes**:
```javascript
scenarios: { userId: 1, status: 1, createdAt: -1 }
metrics: { scenarioId: 1, timestamp: -1 }
alerts: { userId: 1, acknowledged: 1, createdAt: -1 }
```

**Data Retention**:
- Scenarios: Indefinite
- Metrics: 30 days (configurable)
- Alerts: 90 days
- Reports: 365 days

#### Redis (Message Broker & Cache)

**Queues**:
- `loadtest:tasks` - Task distribution (List)
- `loadtest:results` - Result collection (List)
- `loadtest:heartbeat:{workerId}` - Worker status (String + TTL)

**Pub/Sub Channels**:
- `metrics:{scenarioId}` - Real-time metrics
- `alerts` - Alert broadcasts
- `worker:status` - Worker state changes

**Cache**:
- Scenario data (TTL: 5 minutes)
- User sessions (TTL: 24 hours)
- Worker registry (TTL: 60 seconds)

### 5. Monitoring & Observability

#### Prometheus
- Controller metrics (`/actuator/prometheus`)
- Worker metrics (future: Prometheus exporter)
- Custom metrics:
  - `loadtest_requests_total`
  - `loadtest_latency_seconds`
  - `loadtest_errors_total`
  - `loadtest_active_workers`

#### Grafana
- Pre-built dashboards
- Real-time visualization
- Alert integration
- Historical analysis

#### Logging
- Structured JSON logs
- Log levels: DEBUG, INFO, WARN, ERROR
- Centralized logging (ELK/CloudWatch ready)
- Request tracing

## Data Flow

### Test Creation Flow

```
1. User creates scenario via UI
   ↓
2. Frontend → POST /api/v1/scenarios
   ↓
3. Controller validates & stores in MongoDB
   ↓
4. Returns scenario ID to frontend
```

### Test Execution Flow

```
1. User starts test
   ↓
2. Frontend → POST /api/v1/scenarios/{id}/start
   ↓
3. Controller generates worker tasks
   ↓
4. Tasks pushed to Redis queue (RPUSH loadtest:tasks)
   ↓
5. Workers poll queue (LPOP loadtest:tasks)
   ↓
6. Workers execute HTTP requests
   ↓
7. Workers aggregate metrics locally
   ↓
8. Workers push results to Redis (RPUSH loadtest:results)
   ↓
9. Controller processes results (batch processing)
   ↓
10. Controller stores in MongoDB
    ↓
11. Controller broadcasts via WebSocket
    ↓
12. Frontend receives real-time updates
```

### Metrics Aggregation Flow

```
Worker → Redis Queue → Controller → MongoDB
                            ↓
                       WebSocket
                            ↓
                       Frontend
```

## Scalability

### Horizontal Scaling

**Workers**: Scale from 5 to 50+ pods
```bash
kubectl scale deployment worker --replicas=50
```

**Controller**: Scale from 2 to 10 replicas
```bash
kubectl scale deployment controller --replicas=5
```

**Frontend**: Behind CDN (CloudFront/CloudFlare)

### Performance Characteristics

| Load (RPS) | Workers | Controller | MongoDB | Redis | Latency (P95) |
|-----------|---------|------------|---------|-------|---------------|
| 10K       | 2       | 1          | 1       | 1     | < 50ms        |
| 50K       | 10      | 2          | 1       | 1     | < 75ms        |
| 100K      | 20      | 3          | 3       | 2     | < 100ms       |
| 500K+     | 100+    | 10         | 5       | 3     | < 150ms       |

### Auto-Scaling Configuration

**Kubernetes HPA**:
```yaml
metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## Security Architecture

### Authentication Flow

```
1. User login → Frontend
   ↓
2. POST /api/v1/auth/login
   ↓
3. Controller validates credentials
   ↓
4. Generate JWT token (24h expiry)
   ↓
5. Return token to frontend
   ↓
6. Frontend stores in localStorage
   ↓
7. Include in Authorization header for all requests
   ↓
8. Controller validates JWT on each request
```

### Security Measures

- JWT tokens with HS512 signing
- Password hashing with BCrypt
- HTTPS enforcement (production)
- CORS configuration
- Rate limiting (API Gateway)
- Input validation
- SQL injection prevention (NoSQL)
- XSS prevention
- CSRF protection

## Deployment Architecture

### Kubernetes Deployment

```
Namespace: loadtest
  ├── Deployments
  │   ├── controller (3 replicas)
  │   ├── worker (10-50 replicas, HPA enabled)
  │   └── frontend (2 replicas)
  ├── StatefulSets
  │   └── mongodb (3 replicas)
  ├── Services
  │   ├── controller-service (ClusterIP)
  │   ├── mongodb-service (Headless)
  │   ├── redis-service (ClusterIP)
  │   └── frontend-service (LoadBalancer)
  └── Ingress
      └── loadtest-ingress (TLS enabled)
```

### High Availability

- Multiple controller replicas
- MongoDB replica set (3 nodes)
- Redis Sentinel (HA mode)
- Worker auto-scaling
- Health checks and liveness probes
- Graceful shutdown handling
- Circuit breakers for external dependencies

## Disaster Recovery

### Backup Strategy

- **MongoDB**: Daily snapshots to S3
- **Redis**: AOF (Append Only File) + RDB snapshots
- **Configuration**: GitOps (stored in Git)

### Recovery Time Objectives

- **RTO** (Recovery Time): < 30 minutes
- **RPO** (Recovery Point): < 1 hour

### Backup Script

```bash
# Daily at 2 AM
0 2 * * * /scripts/backup-data.sh
```

## Performance Optimization

### Database Optimizations

- Compound indexes on frequently queried fields
- TTL indexes for automatic data cleanup
- Connection pooling (min: 10, max: 100)
- Read preference: Primary for writes, Secondary for reads

### Caching Strategy

- Redis cache for hot data
- In-memory cache for controller (Caffeine)
- CDN for static frontend assets
- Browser caching headers

### Network Optimizations

- HTTP/2 enabled
- gRPC for internal services (future)
- Compression (gzip/brotli)
- Keep-alive connections
- Connection pooling

## Monitoring & Alerting

### Key Metrics

**System Health**:
- CPU utilization
- Memory usage
- Disk I/O
- Network throughput

**Application Metrics**:
- Request rate (RPS)
- Latency percentiles (P50, P95, P99)
- Error rate
- Active workers
- Queue depth

### Alert Thresholds

- CPU > 80% for 5 minutes
- Memory > 85% for 5 minutes
- Error rate > 5% for 2 minutes
- P95 latency > 1000ms for 5 minutes
- No workers available

## Future Enhancements

1. **Multi-Region Support**: Deploy across AWS regions
2. **Custom Protocols**: gRPC, WebSocket, GraphQL testing
3. **AI-Powered Insights**: ML-based anomaly detection
4. **Test Recording**: Record and replay traffic
5. **Distributed Tracing**: OpenTelemetry integration
6. **Cost Optimization**: Spot instances for workers
7. **Advanced Scripting**: JavaScript scenario scripting

## Conclusion

This architecture provides a scalable, resilient, and performant platform for enterprise load testing. The microservices design allows independent scaling and deployment of components, while the message queue pattern ensures loose coupling and fault tolerance.