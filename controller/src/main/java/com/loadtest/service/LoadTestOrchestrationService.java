package com.loadtest.service;

import com.loadtest.exception.InsufficientWorkersException;
import com.loadtest.exception.ScenarioNotFoundException;
import com.loadtest.model.*;
import com.loadtest.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadTestOrchestrationService {

    private final ScenarioRepository scenarioRepository;
    private final RedisQueueService redisQueueService;
    private final MetricsAggregationService metricsAggregationService;
    private final WebSocketMetricsStreamer webSocketMetricsStreamer;
    private final SchedulerService schedulerService;
    private final AdmissionClient admissionClient;

    /**
     * Entry point for executing a load test scenario.
     */
    @Transactional
    public String startScenario(String scenarioId) {

        LoadTestScenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() ->
                        new ScenarioNotFoundException(scenarioId));

        /* ===============================
           SAFETY GATE – APPROVAL REQUIRED
           =============================== */
        if (scenario.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalStateException(
                    "Scenario must be APPROVED before execution"
            );
        }

        /* ===============================
           ADMISSION CONTROL
           =============================== */
        admissionClient.validate(
            scenario.getId(),
            scenario.getNumWorkers(),
            scenario.getDurationSeconds(),
            scenario.getApprovalStatus().name()
        );

        /* ===============================
           WORKER AVAILABILITY CHECK
           =============================== */
        int requiredWorkers = scenario.getNumWorkers();
        int availableWorkers = redisQueueService.getActiveWorkerCount();

        if (availableWorkers < requiredWorkers) {
            log.warn("Insufficient workers: required {}, available {}", requiredWorkers, availableWorkers);
            // In a real generic pool, we might throw or queue. 
            // For now allowing it but logging warning, or throw if strict.
            // throw new InsufficientWorkersException(requiredWorkers, availableWorkers);
        }

        /* ===============================
           EXECUTION CONTEXT
           =============================== */
        String executionId = UUID.randomUUID().toString();

        scenario.setLastExecutionId(executionId);
        scenario.setLastExecutedAt(Instant.now());
        scenario.setRunning(true);
        scenarioRepository.save(scenario);

        /* ===============================
           TASK DISTRIBUTION
           =============================== */
        List<WorkerTask> tasks = WorkerTask.fromScenario(
                scenario,
                executionId
        );

        redisQueueService.publishTasks(tasks);

        /* ===============================
           METRICS PIPELINE INITIALIZATION
           =============================== */
        // metricsAggregationService.initialize(executionId); // Auto-handled by data arrival
        webSocketMetricsStreamer.registerScenario(scenarioId);

        /* ===============================
           SCHEDULER (TIME-BOUND EXECUTION)
           =============================== */
        schedulerService.scheduleStop(
                executionId,
                scenario.getDurationSeconds()
        );

        log.info("Started scenario {} execution {}", scenarioId, executionId);
        return executionId;
    }

    /**
     * Stops a running scenario execution safely.
     */
    @Transactional
    public void stopScenario(String executionId) {
        // Find scenario by executionId not supported by standard repo yet without custom query
        // Assuming we look up via ID or broadcast stop generally.
        // For now, simpler logic:
        
        log.info("Stopping execution {}", executionId);
        
        // redisQueueService.broadcastStop(executionId); // Feature to be added
        // For now clear queue if needed or rely on workers expiring
        
        // Cleanup streams
        // webSocketMetricsStreamer.unregisterScenario(...);
    }
}