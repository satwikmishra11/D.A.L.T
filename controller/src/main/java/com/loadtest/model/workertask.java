package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import com.fasterxml.jackson.annotation.JsonInclude;
import javax.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// ========== Enhanced LoadTestScenario ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scenarios")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoadTestScenario {
    @Id
    private String id;
    
    @NotNull
    @Indexed
    private String userId;
    
    @NotBlank(message = "Scenario name is required")
    @Size(min = 3, max = 100)
    private String name;
    
    private String description;
    
    @NotBlank(message = "Target URL is required")
    @Pattern(regexp = "^https?://.*", message = "Must be valid HTTP/HTTPS URL")
    private String targetUrl;
    
    @NotNull
    private HttpMethod method;
    
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
    
    private String body;
    
    @NotNull
    private LoadProfile loadProfile;
    
    @Min(1)
    @Max(3600)
    private int durationSeconds;
    
    @Min(1)
    @Max(100)
    private int numWorkers;
    
    @Indexed
    private ScenarioStatus status;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
    
    private Instant startedAt;
    private Instant completedAt;
    
    // Advanced features
    private String scheduleId; // For scheduled tests
    private String parentScenarioId; // For test chains
    private List<String> tags;
    private String environment; // dev, staging, prod
    
    // SLA Configuration
    private SlaConfig slaConfig;
    
    // Alerting
    @Builder.Default
    private List<AlertConfig> alerts = List.of();
    
    // Results
    private String resultsSummary;
    private String errorMessage;
    
    // Metadata
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
}

// ========== SLA Configuration ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaConfig {
    @Min(0)
    @Max(100)
    private double minSuccessRate = 99.0; // Percentage
    
    @Min(0)
    private double maxAvgLatencyMs = 500.0;
    
    @Min(0)
    private double maxP95LatencyMs = 1000.0;
    
    @Min(0)
    private double maxP99LatencyMs = 2000.0;
    
    @Min(0)
    private double maxErrorRate = 1.0; // Percentage
}

// ========== Alert Configuration ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfig {
    private AlertType type;
    private AlertChannel channel;
    private String recipient; // email, webhook URL, etc.
    private Map<String, String> config;
}

public enum AlertType {
    SLA_VIOLATION,
    HIGH_ERROR_RATE,
    HIGH_LATENCY,
    WORKER_FAILURE,
    TEST_COMPLETED,
    TEST_FAILED
}

public enum AlertChannel {
    EMAIL,
    SLACK,
    WEBHOOK,
    SMS,
    PAGERDUTY
}

// ========== Enhanced LoadProfile ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadProfile {
    @NotNull
    private ProfileType type;
    
    @Min(1)
    private int initialRps;
    
    private int targetRps;
    private int rampUpSeconds;
    
    @Builder.Default
    private List<BurstConfig> bursts = List.of();
    
    // Advanced options
    private ThinkTime thinkTime;
    private ConnectionPoolConfig connectionPool;
    private RetryConfig retryConfig;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThinkTime {
    private int minMs = 0;
    private int maxMs = 0;
    private ThinkTimeDistribution distribution = ThinkTimeDistribution.UNIFORM;
}

public enum ThinkTimeDistribution {
    UNIFORM,
    GAUSSIAN,
    EXPONENTIAL
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionPoolConfig {
    @Builder.Default
    private int maxConnectionsPerWorker = 100;
    
    @Builder.Default
    private int connectionTimeoutMs = 5000;
    
    @Builder.Default
    private int requestTimeoutMs = 30000;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryConfig {
    @Builder.Default
    private boolean enabled = false;
    
    @Builder.Default
    private int maxRetries = 3;
    
    @Builder.Default
    private int retryDelayMs = 1000;
    
    @Builder.Default
    private List<Integer> retryableStatusCodes = List.of(408, 429, 500, 502, 503, 504);
}

// ========== Enhanced Metric ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "metrics")
public class Metric {
    @Id
    private String id;
    
    @Indexed
    private String scenarioId;
    
    @Indexed
    private String workerId;
    
    @Indexed
    private Instant timestamp;
    
    private long latencyMs;
    private long dnsLookupMs;
    private long tcpConnectionMs;
    private long tlsHandshakeMs;
    private long ttfbMs; // Time to first byte
    
    private int statusCode;
    private boolean success;
    private String errorMessage;
    private String errorType; // TIMEOUT, CONNECTION_REFUSED, etc.
    
    private int requestCount;
    private long requestSize;
    private long responseSize;
    
    // Geographic data
    private String region;
    private String datacenter;
}

// ========== Enhanced ScenarioStats ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioStats {
    private String scenarioId;
    
    // Request stats
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double successRate;
    
    // Latency stats
    private double avgLatencyMs;
    private double minLatencyMs;
    private double maxLatencyMs;
    private double p50LatencyMs;
    private double p75LatencyMs;
    private double p90LatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    private double p999LatencyMs;
    
    // Throughput
    private double currentRps;
    private double avgRps;
    private double peakRps;
    
    // Network stats
    private long totalBytesSent;
    private long totalBytesReceived;
    private double avgRequestSize;
    private double avgResponseSize;
    
    // Status codes
    @Builder.Default
    private Map<Integer, Long> statusCodeDistribution = new HashMap<>();
    
    // Error breakdown
    @Builder.Default
    private Map<String, Long> errorTypeDistribution = new HashMap<>();
    
    // Time series data (last N data points)
    @Builder.Default
    private List<TimeSeriesPoint> timeSeriesData = List.of();
    
    // SLA status
    private boolean slaViolated;
    @Builder.Default
    private List<String> slaViolations = List.of();
    
    private Instant lastUpdated;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesPoint {
    private Instant timestamp;
    private double rps;
    private double avgLatency;
    private double errorRate;
}

// ========== User Model ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    @Email
    private String email;
    
    private String name;
    private String passwordHash;
    
    @Indexed
    private String organizationId;
    
    @Builder.Default
    private List<String> roles = List.of("USER");
    
    @Builder.Default
    private UserQuota quota = new UserQuota();
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
    
    private Instant lastLoginAt;
    
    private boolean enabled = true;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuota {
    @Builder.Default
    private int maxConcurrentTests = 5;
    
    @Builder.Default
    private int maxWorkersPerTest = 10;
    
    @Builder.Default
    private long maxRequestsPerMonth = 1_000_000;
    
    private long requestsUsedThisMonth = 0;
    
    @Builder.Default
    private int maxTestDurationSeconds = 3600;
}

// ========== Alert Model ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alerts")
public class Alert {
    @Id
    private String id;
    
    @Indexed
    private String scenarioId;
    
    @Indexed
    private String userId;
    
    private AlertType type;
    private AlertSeverity severity;
    
    private String title;
    private String message;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @CreatedDate
    private Instant createdAt;
    
    private boolean acknowledged = false;
    private Instant acknowledgedAt;
    private String acknowledgedBy;
}

public enum AlertSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

// ========== Scheduled Test ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scheduled_tests")
public class ScheduledTest {
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String scenarioId;
    
    private String name;
    private String cronExpression; // e.g., "0 0 * * *" for daily at midnight
    
    private boolean enabled = true;
    
    @CreatedDate
    private Instant createdAt;
    
    private Instant lastRunAt;
    private Instant nextRunAt;
    
    private String lastRunStatus;
    private String lastRunScenarioId;
}

// ========== Test Report ==========
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reports")
public class TestReport {
    @Id
    private String id;
    
    @Indexed
    private String scenarioId;
    
    @Indexed
    private String userId;
    
    private String title;
    private String summary;
    
    private ScenarioStats stats;
    
    @Builder.Default
    private List<String> insights = List.of();
    
    @Builder.Default
    private List<String> recommendations = List.of();
    
    private String exportFormat; // PDF, JSON, HTML
    private String exportUrl;
    
    @CreatedDate
    private Instant generatedAt;
}

// ========== Enums ==========
public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
}

public enum ProfileType {
    CONSTANT,
    RAMP,
    BURST,
    SPIKE,
    STEP,
    CUSTOM
}

public enum ScenarioStatus {
    DRAFT,
    SCHEDULED,
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    TIMEOUT
}

public enum WorkerStatus {
    IDLE,
    BUSY,
    PAUSED,
    ERROR,
    OFFLINE,
    TERMINATED
}