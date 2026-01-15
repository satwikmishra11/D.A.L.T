package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

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
    private int numWorkers; // parallelUsers
    
    private ScenarioStatus status;
    private ApprovalStatus approvalStatus;
    
    private int version;
    private String configJson; // For versioning/snapshots
    
    private String lastExecutionId;
    private Instant lastExecutedAt;
    private boolean running;
    
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
}