package com.loadtest.service;

import com.loadtest.model.ScenarioStats;
import com.loadtest.model.WorkerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketMetricsStreamer {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final MetricsAggregationService metricsService;
    private final RedisQueueService queueService;
    
    private final Map<String, String> activeScenarios = new ConcurrentHashMap<>();
    
    public void registerScenario(String scenarioId) {
        activeScenarios.put(scenarioId, scenarioId);
        log.info("Registered scenario {} for real-time streaming", scenarioId);
    }
    
    public void unregisterScenario(String scenarioId) {
        activeScenarios.remove(scenarioId);
        log.info("Unregistered scenario {} from real-time streaming", scenarioId);
    }
    
    @Scheduled(fixedRate = 1000) // Every second
    public void streamMetrics() {
        activeScenarios.keySet().forEach(scenarioId -> {
            try {
                ScenarioStats stats = metricsService.getRealTimeStats(scenarioId, 5);
                
                messagingTemplate.convertAndSend(
                    "/topic/metrics/" + scenarioId,
                    stats
                );
            } catch (Exception e) {
                log.error("Failed to stream metrics for scenario {}", scenarioId, e);
            }
        });
    }
    
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void streamWorkerStatus() {
        try {
            int activeWorkers = queueService.getActiveWorkerCount();
            long taskQueueSize = queueService.getTaskQueueSize();
            
            Map<String, Object> status = Map.of(
                "activeWorkers", activeWorkers,
                "taskQueueSize", taskQueueSize,
                "timestamp", System.currentTimeMillis()
            );
            
            messagingTemplate.convertAndSend("/topic/workers/status", status);
        } catch (Exception e) {
            log.error("Failed to stream worker status", e);
        }
    }
    
    public void streamResult(WorkerResult result) {
        messagingTemplate.convertAndSend(
            "/topic/results/" + result.getTaskId(),
            result
        );
    }
}
