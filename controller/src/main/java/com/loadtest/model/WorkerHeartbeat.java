package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;

@Data
@Builder
public class WorkerHeartbeat {
    private String workerId;
    private Instant timestamp;
    private WorkerStatus status;
    private String currentTaskId;
    private long requestsProcessed;
}
