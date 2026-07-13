# D.A.L.T: Distributed API Load Testing & Observability Platform

A production-grade, horizontally scalable load testing platform that generates 100K+ RPS with real-time observability, dynamic load profiles, and automated admission control.

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![Rust](https://img.shields.io/badge/Rust-1.80%2B-red)
![Go](https://img.shields.io/badge/Go-1.22-blue)
![React](https://img.shields.io/badge/React-18-blue)

## Table of Contents
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Load Profiles](#load-profiles)
- [Admission Control (Go)](#admission-control-go)
- [Security Configurations](#security-configurations)
- [Development](#development)
- [Deployment](#deployment)

## Features

### Core Capabilities
- **Distributed Load Generation**: Horizontally scalable Rust workers generating high-throughput HTTP requests.
- **Dynamic Load Profiles**: Support for Constant, Ramp-up, Burst, and Spike traffic pattern simulations.
- **Real-Time Observability**: Live latency distribution, throughput, error-tracking, and system health stats.
- **Go Admission Control Service**: Features payload validation, token-bucket rate limiting, active request shedding, idempotency checks, and Redis distributed lease-lock leader election.
- **Security & Access Control**: Secure BCrypt-based authentication, restricted Actuator metrics exposure, and dynamic TLS verification bypass (`ignoreTlsErrors`).
- **Real-Time WebSocket Streaming**: Instantaneous metric updates on the React-based DevOps dashboard.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        React Dashboard                       │
│          (Real-time Charts • Scenario Management)            │
└────────────────┬────────────────────────────────────────────┘
                 │ HTTP/WebSocket
                 ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot Control Plane (8080)                │
│  • Scenario Orchestration    • Metrics Aggregation          │
│  • Worker Coordination       • Security (BCrypt)            │
│  • SLA Monitoring           • WebSocket Streaming           │
└───────┬──────────────────────┬──────────────────────┬───────┘
        │                      │                      │
        │ Redis Pub/Sub        │ MongoDB              │ gRPC
        │                      │                      │
        ▼                      ▼                      ▼
┌──────────────────┐  ┌────────────────────┐  ┌────────────────────┐
│  Redis (6379)    │  │  MongoDB (27017)   │  │ Go Admission (9090)│
│  • Task Queue    │  │  • Scenarios       │  │ • Rate Limiting    │
│  • Result Queue  │  │  • Metrics         │  │ • Request Shedding │
│  • Heartbeats    │  │  • Analytics       │  │ • Leader Lease     │
└────────┬─────────┘  └────────────────────┘  └────────────────────┘
         │
         │ Task Distribution
         │
    ┌────┴────┬────────┬────────┬────────┐
    ▼         ▼        ▼        ▼        ▼
┌────────┐ ┌────────┐ ┌────────┐ ... [N Workers]
│Worker 1│ │Worker 2│ │Worker N│
│(Rust)  │ │(Rust)  │ │(Rust)  │
│5K RPS  │ │5K RPS  │ │5K RPS  │
└────────┘ └────────┘ └────────┘
```

## Tech Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Control Plane** | Spring Boot 3.2 (Java 17) | Orchestration & API gateway |
| **Admission Service** | Go 1.22 (gRPC) | Pre-execution safety gate & rate limiter |
| **Workers** | Rust (Tokio) | High-performance concurrent load generation |
| **Message Broker** | Redis 7 | Task distribution, cancellation, and lease locking |
| **Metrics Store** | MongoDB 7 | Scenario, version, and time-series metrics storage |
| **Frontend** | React 18 + Recharts | Consolidated admin control panel |

## Quick Start

### Prerequisites
```bash
docker --version  # >= 20.10
docker-compose --version  # >= 2.0
```

### 1. Start Platform
```bash
# Start all services with 5 workers
docker-compose up -d --scale worker=5

# Verify services
docker-compose ps
```

### 2. Access Dashboard
Open your browser to `http://localhost:3000`
- **Default Credentials**: `demo@loadtest.pro` / `demo123` (automatically seeded on controller startup).

---

## Load Profiles

D.A.L.T supports 4 distinct load profiles that are dynamically calculated and recalculated by Rust workers every second of execution:

1. **Constant**: Steady rate of traffic throughout the test.
2. **Ramp-Up**: Linear ramp-up from `initialRps` to `targetRps` over `rampUpSeconds`, staying at `targetRps` for the remaining duration.
3. **Spike**: Symmetric traffic spike ramping up linearly to `targetRps` at the midpoint, then ramping down to `initialRps`.
4. **Burst**: Alternating traffic bursts where RPS increases to `burst.rps` during specified time windows, returning to `initialRps` baseline otherwise.

---

## Admission Control (Go)

Before any scenario runs, the controller checks with the Go Admission Service over gRPC:
- **Distributed Leader Election**: Admission instances acquire a distributed lease lock in Redis (`admission:leader:lease` with 5s TTL and 2s renew intervals).
- **Payload Validation**: Validates org IDs, users, and duration constraints.
- **Request Shedding**: Prevents controller overloading by rejecting executions when active tasks exceed `max_inflight`.
- **Token Bucket Rate Limiting**: Enforces requests-per-minute limits per organization.

---

## Security Configurations

- **Password Hashing**: User passwords are encrypted using BCrypt in the database.
- **Actuator Hardening**: Actuator endpoints are restricted in `application-prod.yml` to expose only `/health` and `/info` in production. Direct actuator access `/actuator/**` is blocked for unauthenticated users in `SecurityConfig.java`.
- **TLS validation bypass**: Scenarios can bypass TLS errors dynamically (`ignoreTlsErrors`) to support internal testing environments.

---

## Development

### Running Local Builds

**Go Admission Service:**
```bash
cd admission
go build -v
```

**Controller:**
```bash
cd controller
export JAVA_HOME=/path/to/openjdk17
mvn clean compile
```

**Worker:**
```bash
cd worker
cargo build --release
```

**Frontend:**
```bash
cd frontend
npm install
npm run build
```

## License
MIT
