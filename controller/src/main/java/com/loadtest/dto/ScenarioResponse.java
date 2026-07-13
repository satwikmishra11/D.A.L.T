package com.loadtest.dto;

import com.loadtest.model.LoadTestScenario;
import com.loadtest.model.LoadProfile;
import com.loadtest.model.ScenarioStatus;
import com.loadtest.model.HttpMethod;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.List;

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
