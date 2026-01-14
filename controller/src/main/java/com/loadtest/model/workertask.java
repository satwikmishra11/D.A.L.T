package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;
import java.util.Map;

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
