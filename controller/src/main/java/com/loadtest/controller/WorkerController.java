package com.loadtest.controller;

import com.loadtest.dto.WorkerMetrics;
import com.loadtest.model.WorkerHeartbeat;
import com.loadtest.model.WorkerStatus;
import com.loadtest.service.RedisQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkerController {

    private final RedisQueueService queueService;

    @GetMapping("/status")
    public ResponseEntity<List<WorkerMetrics>> getStatus() {
        List<String> workerIds = queueService.getActiveWorkerIds();
        List<WorkerMetrics> metrics = workerIds.stream()
                .map(workerId -> {
                    WorkerHeartbeat heartbeat = queueService.getWorkerHeartbeat(workerId);
                    return WorkerMetrics.builder()
                            .workerId(workerId)
                            .status(heartbeat != null ? heartbeat.getStatus() : WorkerStatus.OFFLINE)
                            .requestsProcessed(heartbeat != null ? heartbeat.getRequestsProcessed() : 0)
                            .lastHeartbeat(heartbeat != null ? heartbeat.getTimestamp() : null)
                            .currentTaskId(heartbeat != null ? heartbeat.getCurrentTaskId() : null)
                            .build();
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/{workerId}/heartbeat")
    public ResponseEntity<WorkerHeartbeat> getHeartbeat(@PathVariable String workerId) {
        WorkerHeartbeat heartbeat = queueService.getWorkerHeartbeat(workerId);
        if (heartbeat == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(heartbeat);
    }
}
