package com.loadtest.service;

import com.loadtest.model.WorkerResult;
import com.loadtest.model.ScenarioStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultProcessorService {
    
    private final RedisQueueService queueService;
    private final MetricsAggregationService metricsService;
    private final WebSocketMetricsStreamer metricsStreamer;
    private final AlertService alertService;
    
    @Scheduled(fixedRate = 500) // Every 500ms
    public void processResults() {
        try {
            // Poll multiple results at once for efficiency
            List<WorkerResult> results = queueService.pollResults(100);
            
            if (!results.isEmpty()) {
                log.debug("Processing {} results", results.size());
                
                results.forEach(result -> {
                    metricsService.saveWorkerResult(result);
                    metricsStreamer.streamResult(result);
                });

                // Check SLAs for scenarios that received metrics in this batch
                results.stream()
                    .map(r -> metricsService.getScenarioIdForTask(r.getTaskId()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(scenarioId -> {
                        try {
                            ScenarioStats stats = metricsService.getRealTimeStats(scenarioId, 10);
                            alertService.checkSla(scenarioId, stats);
                        } catch (Exception e) {
                            log.error("Failed to check SLA for scenario {}", scenarioId, e);
                        }
                    });
            }
        } catch (Exception e) {
            log.error("Error processing results", e);
        }
    }
}