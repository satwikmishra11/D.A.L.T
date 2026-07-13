// ========== DTOs (Data Transfer Objects) ==========
package com.loadtest.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.loadtest.model.*;
import javax.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScenarioRequest {
    @NotBlank(message = "Scenario name is required")
    @Size(min = 3, max = 100)
    private String name;
    
    @Size(max = 500)
    private String description;
    
    @NotBlank(message = "Target URL is required")
    @Pattern(regexp = "^https?://.*", message = "Must be valid HTTP/HTTPS URL")
    private String targetUrl;
    
    @NotNull
    private HttpMethod method;
    
    private Map<String, String> headers;
    private String body;
    
    @Min(value = 1, message = "Duration must be at least 1 second")
    @Max(value = 7200, message = "Duration cannot exceed 2 hours")
    private int durationSeconds;
    
    @Min(value = 1, message = "Must have at least 1 worker")
    @Max(value = 100, message = "Cannot exceed 100 workers")
    private int numWorkers;
    
    @NotNull
    private LoadProfileRequest loadProfile;
    
    private String environment;
    private List<String> tags;
    private SlaConfigRequest slaConfig;
    private List<AlertConfigRequest> alerts;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadProfileRequest {
    @NotNull
    private ProfileType type;
    
    @Min(1)
    private int initialRps;
    
    private int targetRps;
    private int rampUpSeconds;
    private List<BurstConfigRequest> bursts;
    private ThinkTimeRequest thinkTime;
    private ConnectionPoolConfigRequest connectionPool;
    private RetryConfigRequest retryConfig;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BurstConfigRequest {
    private int startSecond;
    private int durationSeconds;
    private int rps;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThinkTimeRequest {
    private int minMs;
    private int maxMs;
    private ThinkTimeDistribution distribution;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionPoolConfigRequest {
    private int maxConnectionsPerWorker = 100;
    private int connectionTimeoutMs = 5000;
    private int requestTimeoutMs = 30000;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryConfigRequest {
    private boolean enabled = false;
    private int maxRetries = 3;
    private int retryDelayMs = 1000;
    private List<Integer> retryableStatusCodes;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlaConfigRequest {
    private double minSuccessRate = 99.0;
    private double maxAvgLatencyMs = 500.0;
    private double maxP95LatencyMs = 1000.0;
    private double maxP99LatencyMs = 2000.0;
    private double maxErrorRate = 1.0;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfigRequest {
    private AlertType type;
    private AlertChannel channel;
    private String recipient;
    private Map<String, String> config;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioResponse {
    private String id;
    private String userId;
    private String name;
    private String description;
    private String targetUrl;
    private HttpMethod method;
    private ScenarioStatus status;
    private int durationSeconds;
    private int numWorkers;
    private LoadProfile loadProfile;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String environment;
    private List<String> tags;

    public ScenarioResponse(LoadTestScenario scenario) {
        this.id = scenario.getId();
        this.userId = scenario.getUserId();
        this.name = scenario.getName();
        this.targetUrl = scenario.getTargetUrl();
        this.method = scenario.getMethod();
        this.status = scenario.getStatus();
        this.durationSeconds = scenario.getDurationSeconds();
        this.numWorkers = scenario.getNumWorkers();
        this.loadProfile = scenario.getLoadProfile();
        this.createdAt = scenario.getCreatedAt();
        this.startedAt = scenario.getStartedAt();
        this.completedAt = scenario.getCompletedAt();
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private String scenarioId;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double successRate;
    private LatencyStats latency;
    private ThroughputStats throughput;
    private Map<Integer, Long> statusCodeDistribution;
    private Map<String, Long> errorTypeDistribution;
    private List<TimeSeriesPoint> timeSeries;
    private SlaStatus slaStatus;
    private Instant lastUpdated;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatencyStats {
    private double avgMs;
    private double minMs;
    private double maxMs;
    private double p50Ms;
    private double p75Ms;
    private double p90Ms;
    private double p95Ms;
    private double p99Ms;
    private double p999Ms;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThroughputStats {
    private double currentRps;
    private double avgRps;
    private double peakRps;
    private long totalBytesSent;
    private long totalBytesReceived;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaStatus {
    private boolean violated;
    private List<String> violations;
    private SlaConfig config;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private String path;
    private int status;
    private Instant timestamp;
    private Map<String, String> details;
}

@Data
@Builder
public class DashboardSummary {
    private long totalScenarios;
    private long activeScenarios;
    private long completedToday;
    private int activeWorkers;
    private long totalRequestsToday;
    private double avgSuccessRate;
    private int activeAlerts;
    private List<RecentScenario> recentScenarios;
}

@Data
@Builder
public class RecentScenario {
    private String id;
    private String name;
    private ScenarioStatus status;
    private double successRate;
    private double avgLatency;
    private Instant startedAt;
}

@Data
@Builder
public class WorkerMetrics {
    private String workerId;
    private WorkerStatus status;
    private long requestsProcessed;
    private double avgLatency;
    private Instant lastHeartbeat;
    private String currentTaskId;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesPoint {
    private Instant timestamp;
    private double value;
    private String metricName;
}