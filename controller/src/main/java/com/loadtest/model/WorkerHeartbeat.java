package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerHeartbeat {
    private String workerId;
    private Instant timestamp;
    private WorkerStatus status;
    private String currentTaskId;
    private long requestsProcessed;
}
