package com.loadtest.service;

import com.loadtest.model.WorkerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultProcessorService {
    
    private final RedisQueueService queueService;
    private final MetricsAggregationService metricsService;
    private final WebSocketMetricsStreamer metricsStreamer;
    
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
            }
        } catch (Exception e) {
            log.error("Error processing results", e);
        }
    }
}