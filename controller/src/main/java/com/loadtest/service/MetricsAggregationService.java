package com.loadtest.service;

import com.loadtest.model.Metric;
import com.loadtest.model.ScenarioStats;
import com.loadtest.model.WorkerResult;
import com.loadtest.repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsAggregationService {

    private final MetricRepository metricRepository;

    public void saveWorkerResult(WorkerResult result) {
        try {
            Metric metric = Metric.builder()
                    .scenarioId(result.getScenarioId()) 
                    .workerId(result.getWorkerId())
                    .timestamp(result.getTimestamp())
                    .latencyMs((long) result.getAvgLatencyMs())
                    .success(result.getErrorCount() == 0)
                    .requestCount(result.getTotalRequests())
                    .build();
            
            metricRepository.save(metric);
        } catch (Exception e) {
            log.error("Failed to save metric", e);
        }
    }

    public ScenarioStats getRealTimeStats(String scenarioId, int seconds) {
        Instant now = Instant.now();
        Instant start = now.minusSeconds(seconds);

        List<Metric> metrics = metricRepository.findByScenarioIdAndTimestampBetween(scenarioId, start, now);

        if (metrics.isEmpty()) {
            return ScenarioStats.builder()
                    .scenarioId(scenarioId)
                    .lastUpdated(now)
                    .statusCodeDistribution(new HashMap<>())
                    .build();
        }

        long totalReqs = 0;
        long successReqs = 0;
        long failedReqs = 0;
        double totalLatencyProduct = 0;
        double minLatency = Double.MAX_VALUE;
        double maxLatency = 0;
        Map<Integer, Long> statusDist = new HashMap<>();

        for (Metric m : metrics) {
            long count = m.getRequestCount() > 0 ? m.getRequestCount() : 1;
            totalReqs += count;
            
            if (m.isSuccess()) {
                successReqs += count;
            } else {
                failedReqs += count;
            }
            
            // Approximate aggregation from pre-aggregated metrics
            totalLatencyProduct += (m.getLatencyMs() * count);
            minLatency = Math.min(minLatency, m.getLatencyMs());
            maxLatency = Math.max(maxLatency, m.getLatencyMs());
            
            statusDist.merge(m.getStatusCode(), count, Long::sum);
        }

        double avgLatency = totalReqs > 0 ? totalLatencyProduct / totalReqs : 0;

        return ScenarioStats.builder()
                .scenarioId(scenarioId)
                .totalRequests(totalReqs)
                .successfulRequests(successReqs)
                .failedRequests(failedReqs)
                .successRate(totalReqs > 0 ? (double) successReqs / totalReqs : 0)
                .avgLatencyMs(avgLatency)
                .minLatencyMs(totalReqs > 0 ? minLatency : 0)
                .maxLatencyMs(maxLatency)
                .currentRps(totalReqs / (double) seconds) // Approximate
                .statusCodeDistribution(statusDist)
                .lastUpdated(now)
                .build();
    }
}
