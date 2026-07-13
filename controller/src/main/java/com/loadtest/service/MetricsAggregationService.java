package com.loadtest.service;

import com.loadtest.model.Metric;
import com.loadtest.model.ScenarioStats;
import com.loadtest.model.WorkerResult;
import com.loadtest.repository.MetricRepository;
import com.loadtest.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsAggregationService {

    private final MetricRepository metricRepository;
    private final ScenarioRepository scenarioRepository;
    private final Map<String, String> executionToScenarioCache = new ConcurrentHashMap<>();

    public String getScenarioIdForTask(String taskId) {
        if (taskId == null || !taskId.contains("-w")) {
            return null;
        }
        String executionId = taskId.substring(0, taskId.lastIndexOf("-w"));
        return executionToScenarioCache.computeIfAbsent(executionId, execId -> 
            scenarioRepository.findByLastExecutionId(execId)
                .map(com.loadtest.model.LoadTestScenario::getId)
                .orElse(null)
        );
    }

    public void saveWorkerResult(WorkerResult result) {
        try {
            String scenarioId = result.getScenarioId();
            if (scenarioId == null) {
                scenarioId = getScenarioIdForTask(result.getTaskId());
            }
            if (scenarioId == null) {
                log.warn("Could not map taskId {} to scenarioId", result.getTaskId());
                return;
            }

            int statusCode = 200;
            if (result.getStatusCode() > 0) {
                statusCode = result.getStatusCode();
            } else if (result.getStatusCodes() != null && !result.getStatusCodes().isEmpty()) {
                statusCode = result.getStatusCodes().keySet().iterator().next();
            } else if (!result.isSuccess()) {
                statusCode = 500;
            }

            String errorMsg = result.getError();
            if (errorMsg == null && result.getErrorTypes() != null && !result.getErrorTypes().isEmpty()) {
                errorMsg = result.getErrorTypes().keySet().iterator().next();
            }

            Metric metric = Metric.builder()
                    .scenarioId(scenarioId) 
                    .workerId(result.getWorkerId())
                    .timestamp(result.getTimestamp())
                    .latencyMs((long) result.getAvgLatencyMs())
                    .success(result.isSuccess())
                    .statusCode(statusCode)
                    .errorMessage(errorMsg)
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
                    .errorTypeDistribution(new HashMap<>())
                    .build();
        }

        long totalReqs = 0;
        long successReqs = 0;
        long failedReqs = 0;
        double totalLatencyProduct = 0;
        double minLatency = Double.MAX_VALUE;
        double maxLatency = 0;
        Map<Integer, Long> statusDist = new HashMap<>();
        Map<String, Long> errorDist = new HashMap<>();

        for (Metric m : metrics) {
            long count = m.getRequestCount() > 0 ? m.getRequestCount() : 1;
            totalReqs += count;
            
            if (m.isSuccess()) {
                successReqs += count;
            } else {
                failedReqs += count;
            }
            
            totalLatencyProduct += (m.getLatencyMs() * count);
            minLatency = Math.min(minLatency, m.getLatencyMs());
            maxLatency = Math.max(maxLatency, m.getLatencyMs());
            
            statusDist.merge(m.getStatusCode(), count, Long::sum);
            if (m.getErrorMessage() != null) {
                errorDist.merge(m.getErrorMessage(), count, Long::sum);
            }
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
                .p50LatencyMs(avgLatency) // Fallback for realtime stats percentiles
                .p75LatencyMs(avgLatency)
                .p90LatencyMs(avgLatency)
                .p95LatencyMs(avgLatency)
                .p99LatencyMs(avgLatency)
                .currentRps(totalReqs / (double) seconds)
                .statusCodeDistribution(statusDist)
                .errorTypeDistribution(errorDist)
                .lastUpdated(now)
                .build();
    }

    public ScenarioStats getAggregatedStats(String scenarioId) {
        List<Metric> metrics = metricRepository.findByScenarioId(scenarioId);
        Instant now = Instant.now();

        if (metrics.isEmpty()) {
            return ScenarioStats.builder()
                    .scenarioId(scenarioId)
                    .lastUpdated(now)
                    .statusCodeDistribution(new HashMap<>())
                    .errorTypeDistribution(new HashMap<>())
                    .build();
        }

        long totalReqs = 0;
        long successReqs = 0;
        long failedReqs = 0;
        double totalLatencyProduct = 0;
        double minLatency = Double.MAX_VALUE;
        double maxLatency = 0;
        Map<Integer, Long> statusDist = new HashMap<>();
        Map<String, Long> errorDist = new HashMap<>();

        List<Double> latencies = new java.util.ArrayList<>();

        for (Metric m : metrics) {
            long count = m.getRequestCount() > 0 ? m.getRequestCount() : 1;
            totalReqs += count;
            
            if (m.isSuccess()) {
                successReqs += count;
            } else {
                failedReqs += count;
            }
            
            totalLatencyProduct += (m.getLatencyMs() * count);
            minLatency = Math.min(minLatency, m.getLatencyMs());
            maxLatency = Math.max(maxLatency, m.getLatencyMs());
            
            for (int k = 0; k < count; k++) {
                latencies.add((double) m.getLatencyMs());
            }
            
            statusDist.merge(m.getStatusCode(), count, Long::sum);
            if (m.getErrorMessage() != null) {
                errorDist.merge(m.getErrorMessage(), count, Long::sum);
            }
        }

        double avgLatency = totalReqs > 0 ? totalLatencyProduct / totalReqs : 0;
        double successRate = totalReqs > 0 ? (double) successReqs / totalReqs : 0;

        java.util.Collections.sort(latencies);
        double p50 = getPercentile(latencies, 50);
        double p75 = getPercentile(latencies, 75);
        double p90 = getPercentile(latencies, 90);
        double p95 = getPercentile(latencies, 95);
        double p99 = getPercentile(latencies, 99);

        double avgRps = 0;
        if (!metrics.isEmpty()) {
            Instant first = metrics.get(0).getTimestamp();
            Instant last = metrics.get(metrics.size() - 1).getTimestamp();
            long duration = java.time.Duration.between(first, last).toSeconds();
            if (duration > 0) {
                avgRps = (double) totalReqs / duration;
            } else {
                avgRps = totalReqs;
            }
        }

        return ScenarioStats.builder()
                .scenarioId(scenarioId)
                .totalRequests(totalReqs)
                .successfulRequests(successReqs)
                .failedRequests(failedReqs)
                .successRate(successRate)
                .avgLatencyMs(avgLatency)
                .minLatencyMs(totalReqs > 0 ? minLatency : 0)
                .maxLatencyMs(maxLatency)
                .p50LatencyMs(p50)
                .p75LatencyMs(p75)
                .p90LatencyMs(p90)
                .p95LatencyMs(p95)
                .p99LatencyMs(p99)
                .avgRps(avgRps)
                .currentRps(avgRps)
                .statusCodeDistribution(statusDist)
                .errorTypeDistribution(errorDist)
                .lastUpdated(now)
                .build();
    }

    private double getPercentile(List<Double> sortedList, double percentile) {
        if (sortedList.isEmpty()) return 0.0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedList.size()) - 1;
        return sortedList.get(Math.max(0, Math.min(sortedList.size() - 1, index)));
    }
}

