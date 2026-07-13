package com.loadtest.dto;

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
public class SystemHealth {
    private int activeWorkers;
    private long taskQueueSize;
    private long resultQueueSize;
    private List<WorkerMetrics> workerMetrics;
    private Instant timestamp;
}
