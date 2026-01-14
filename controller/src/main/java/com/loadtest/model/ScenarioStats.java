package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;
import java.util.Map;

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
