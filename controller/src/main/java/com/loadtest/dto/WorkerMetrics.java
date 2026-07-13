package com.loadtest.dto;

import com.loadtest.model.WorkerStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerMetrics {
    private String workerId;
    private WorkerStatus status;
    private long requestsProcessed;
    private double avgLatency;
    private Instant lastHeartbeat;
    private String currentTaskId;
}
