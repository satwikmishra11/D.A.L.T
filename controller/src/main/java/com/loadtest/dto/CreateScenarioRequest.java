// ========== DTOs (Data Transfer Objects) ==========
package com.loadtest.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.loadtest.model.*;
import jakarta.validation.constraints.*;
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
    
    private String description;
    
    @NotBlank(message = "Target URL is required")
    @Pattern(regexp = "^https?://.*", message = "Must be a valid HTTP/HTTPS URL")
    private String targetUrl;
    
    @NotNull(message = "HTTP Method is required")
    private HttpMethod method;
    
    private Map<String, String> headers;
    private String body;
    
    @Min(value = 1, message = "Duration must be at least 1 second")
    private int durationSeconds;
    
    @Min(value = 1, message = "Number of workers must be at least 1")
    @Max(value = 100, message = "Maximum 100 workers allowed")
    private int numWorkers;
    
    private LoadProfileRequest loadProfile;
    private SlaConfigRequest slaConfig;
    private List<AlertConfigRequest> alerts;
    private Boolean ignoreTlsErrors;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoadProfileRequest {
    private ProfileType type;
    private int initialRps;
    private int targetRps;
    private int rampUpSeconds;
    private List<BurstConfigRequest> bursts;
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
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionPoolConfigRequest {
    private int maxConnections;
    private int idleTimeoutMs;
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