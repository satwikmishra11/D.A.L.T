package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// ========== LoadTestScenario ==========
@Data
@Builder
@Document(collection = "scenarios")
public class LoadTestScenario {
    @Id
    private String id;
    private String userId;
    private String name;
    private String targetUrl;
    private HttpMethod method;
    private Map<String, String> headers;
    private String body;
    
    private LoadProfile loadProfile;
    private int durationSeconds;
    private int numWorkers;
    
    private ScenarioStatus status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
}

// ========== LoadProfile ==========
@Data
@Builder
public class LoadProfile {
    private ProfileType type;
    private int initialRps;
    private int targetRps;
    private int rampUpSeconds;
    private List<BurstConfig> bursts;
}

@Data
@Builder
public class BurstConfig {
    private int startSecond;
    private int durationSeconds;
    private int rps;
}

// ========== Metrics ==========
@Data
@Builder
@Document(collection = "metrics")
public class Metric {
    @Id
    private String id;
    private String scenarioId;
    private String workerId;
    
    private Instant timestamp;
    private long latencyMs;
    private int statusCode;
    private boolean success;
    private String errorMessage;
    
    // Aggregated data
    private int requestCount;
}

// ========== WorkerTask ==========
@Data
@Builder
public class WorkerTask {
    private String taskId;
    private String scenarioId;
    private String targetUrl;
    private HttpMethod method;
    private Map<String, String> headers;
    private String body;
    
    private int rps;
    private int durationSeconds;
    private Instant startTime;
}

// ========== WorkerResult ==========
@Data
@Builder
public class WorkerResult {
    private String taskId;
    private String workerId;
    private Instant timestamp;
    
    private long latencyMs;
    private int statusCode;
    private boolean success;
    private String error;
    
    // Batch results
    private int totalRequests;
    private int successCount;
    private int errorCount;
    private double avgLatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
}

// ========== WorkerHeartbeat ==========
@Data
@Builder
public class WorkerHeartbeat {
    private String workerId;
    private Instant timestamp;
    private WorkerStatus status;
    private String currentTaskId;
    private long requestsProcessed;
}

// ========== Aggregated Statistics ==========
@Data
@Builder
public class ScenarioStats {
    private String scenarioId;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double successRate;
    
    private double avgLatencyMs;
    private double minLatencyMs;
    private double maxLatencyMs;
    private double p50LatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    
    private Map<Integer, Long> statusCodeDistribution;
    private double currentRps;
    private Instant lastUpdated;
}

// ========== Enums ==========
public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}

public enum ProfileType {
    CONSTANT,  // Fixed RPS
    RAMP,      // Gradual increase
    BURST,     // Sudden spikes
    SPIKE      // Single large spike
}

public enum ScenarioStatus {
    DRAFT,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

public enum WorkerStatus {
    IDLE,
    BUSY,
    ERROR,
    OFFLINE
}