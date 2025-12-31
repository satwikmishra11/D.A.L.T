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

// ========== Exception Classes ==========
package com.loadtest.exception;

public class ScenarioNotFoundException extends RuntimeException {
    public ScenarioNotFoundException(String scenarioId) {
        super("Scenario not found: " + scenarioId);
    }
}

public class InsufficientWorkersException extends RuntimeException {
    public InsufficientWorkersException(int required, int available) {
        super(String.format("Insufficient workers: required %d, available %d", required, available));
    }
}

public class ScenarioAlreadyRunningException extends RuntimeException {
    public ScenarioAlreadyRunningException(String scenarioId) {
        super("Scenario already running: " + scenarioId);
    }
}

public class InvalidScenarioConfigException extends RuntimeException {
    public InvalidScenarioConfigException(String message) {
        super("Invalid scenario configuration: " + message);
    }
}

public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super("Quota exceeded: " + message);
    }
}

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}

public class ExportException extends RuntimeException {
    public ExportException(String message, Throwable cause) {
        super("Export failed: " + message, cause);
    }
}

// ========== Global Exception Handler ==========
package com.loadtest.exception;

import com.loadtest.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ScenarioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleScenarioNotFound(
            ScenarioNotFoundException ex, WebRequest request) {
        
        log.error("Scenario not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("SCENARIO_NOT_FOUND")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(Instant.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(InsufficientWorkersException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientWorkers(
            InsufficientWorkersException ex, WebRequest request) {
        
        log.error("Insufficient workers: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("INSUFFICIENT_WORKERS")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .timestamp(Instant.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    @ExceptionHandler(ScenarioAlreadyRunningException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyRunning(
            ScenarioAlreadyRunningException ex, WebRequest request) {
        
        log.error("Scenario already running: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("SCENARIO_ALREADY_RUNNING")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.CONFLICT.value())
                .timestamp(Instant.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(InvalidScenarioConfigException.class)
    public ResponseEntity<ErrorResponse> handleInvalidConfig(
            InvalidScenarioConfigException ex, WebRequest request) {
        
        log.error("Invalid scenario config: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("INVALID_CONFIGURATION")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(
            QuotaExceededException ex, WebRequest request) {
        
        log.error("Quota exceeded: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("QUOTA_EXCEEDED")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .timestamp(Instant.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, WebRequest request) {
        
        log.error("Authentication error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("AUTHENTICATION_FAILED")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.UNAUTHORIZED.value())
                .timestamp(Instant.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        
        log.error("Access denied: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("ACCESS_DENIED")
                .message("You don't have permission to access this resource")
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.FORBIDDEN.value())
                .timestamp(Instant.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation error: {}", errors);
        
        ErrorResponse error = ErrorResponse.builder()
                .error("VALIDATION_FAILED")
                .message("Request validation failed")
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now())
                .details(errors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(Instant.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}