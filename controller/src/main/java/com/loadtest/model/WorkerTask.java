package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Integer timeoutSeconds;
    private Boolean ignoreTlsErrors;
    private LoadProfile loadProfile;

    public static List<WorkerTask> fromScenario(LoadTestScenario scenario, String executionId) {
        List<WorkerTask> tasks = new ArrayList<>();
        int workers = scenario.getNumWorkers() > 0 ? scenario.getNumWorkers() : 1;
        
        int baseRps = 10;
        if (scenario.getLoadProfile() != null) {
            baseRps = scenario.getLoadProfile().getTargetRps() > 0 ? scenario.getLoadProfile().getTargetRps() / workers : scenario.getLoadProfile().getInitialRps() / workers;
            if (baseRps <= 0) baseRps = 1;
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
                    .timeoutSeconds(30)
                    .ignoreTlsErrors(scenario.isIgnoreTlsErrors())
                    .loadProfile(scenario.getLoadProfile())
                    .build());
        }
        return tasks;
    }
}

