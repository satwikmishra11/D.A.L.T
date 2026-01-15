package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerResult {
    private String scenarioId; // Added for context
    private String taskId;
    private String workerId;
    private Instant timestamp;
    
    private long latencyMs;
    private int statusCode;
    private boolean success;
    private String error;
    
    // Batch results
    private int totalRequests;
    private int successCount;
    private int errorCount;
    private double avgLatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
}