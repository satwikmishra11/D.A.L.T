package com.loadtest.service;

import com.loadtest.exception.InsufficientWorkersException;
import com.loadtest.exception.ScenarioNotFoundException;
import com.loadtest.model.*;
import com.loadtest.repository.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LoadTestOrchestrationService {

    private final ScenarioRepository scenarioRepository;
    private final RedisQueueService redisQueueService;
    private final MetricsAggregationService metricsAggregationService;
    private final WebSocketMetricsStreamer webSocketMetricsStreamer;
    private final SchedulerService schedulerService;

    public LoadTestOrchestrationService(
            ScenarioRepository scenarioRepository,
            RedisQueueService redisQueueService,
            MetricsAggregationService metricsAggregationService,
            WebSocketMetricsStreamer webSocketMetricsStreamer,
            SchedulerService schedulerService
    ) {
        this.scenarioRepository = scenarioRepository;
        this.redisQueueService = redisQueueService;
        this.metricsAggregationService = metricsAggregationService;
        this.webSocketMetricsStreamer = webSocketMetricsStreamer;
        this.schedulerService = schedulerService;
    }

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
           WORKER AVAILABILITY CHECK
           =============================== */
        int requiredWorkers = scenario.getParallelUsers();
        int availableWorkers = redisQueueService.availableWorkers();

        if (availableWorkers < requiredWorkers) {
            throw new InsufficientWorkersException(
                    requiredWorkers,
                    availableWorkers
            );
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

        for (WorkerTask task : tasks) {
            redisQueueService.enqueueTask(task);
        }

        /* ===============================
           METRICS PIPELINE INITIALIZATION
           =============================== */
        metricsAggregationService.initialize(executionId);
        webSocketMetricsStreamer.openStream(executionId);

        /* ===============================
           SCHEDULER (TIME-BOUND EXECUTION)
           =============================== */
        schedulerService.scheduleStop(
                executionId,
                scenario.getDurationSeconds()
        );

        return executionId;
    }

    /**
     * Stops a running scenario execution safely.
     */
    @Transactional
    public void stopScenario(String executionId) {

        LoadTestScenario scenario =
                scenarioRepository.findByLastExecutionId(executionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Execution not found: " + executionId
                                ));

        scenario.setRunning(false);
        scenarioRepository.save(scenario);

        redisQueueService.broadcastStop(executionId);
        metricsAggregationService.finalize(executionId);
        webSocketMetricsStreamer.closeStream(executionId);
    }
}
