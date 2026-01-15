package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
public class WorkerTask {
    private String taskId;
    private String scenarioId;
    private String targetUrl;
    private HttpMethod method;
    private Map<String, String> headers;
    private String body;
    
    private int rps; // derived from profile?
    private int durationSeconds;
    private Instant startTime;

    public static List<WorkerTask> fromScenario(LoadTestScenario scenario, String executionId) {
        List<WorkerTask> tasks = new ArrayList<>();
        int workers = scenario.getNumWorkers() > 0 ? scenario.getNumWorkers() : 1;
        
        // Simple equal distribution strategy
        int baseRps = 10; // Default or calculate from LoadProfile
        if (scenario.getLoadProfile() != null) {
            baseRps = scenario.getLoadProfile().getTargetRps() / workers;
        }

        for (int i = 0; i < workers; i++) {
            tasks.add(WorkerTask.builder()
                    .taskId(executionId + "-w" + i)
                    .scenarioId(scenario.getId())
                    .targetUrl(scenario.getTargetUrl())
                    .method(scenario.getMethod())
                    .headers(scenario.getHeaders())
                    .body(scenario.getBody())
                    .rps(baseRps)
                    .durationSeconds(scenario.getDurationSeconds())
                    .startTime(Instant.now())
                    .build());
        }
        return tasks;
    }
}
