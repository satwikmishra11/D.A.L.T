// ========== DashboardController.java ==========
package com.loadtest.controller;

import com.loadtest.dto.*;
import com.loadtest.model.*;
import com.loadtest.repository.*;
import com.loadtest.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {
    
    private final ScenarioRepository scenarioRepository;
    private final MetricRepository metricRepository;
    private final AlertRepository alertRepository;
    private final RedisQueueService queueService;
    private final MetricsAggregationService metricsService;
    
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getSummary(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        
        // Get all scenarios for user
        List<LoadTestScenario> scenarios = scenarioRepository.findByUserId(userId);
        
        // Count by status
        long totalScenarios = scenarios.size();
        long activeScenarios = scenarios.stream()
                .filter(s -> s.getStatus() == ScenarioStatus.RUNNING)
                .count();
        
        // Get today's completed scenarios
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long completedToday = scenarios.stream()
                .filter(s -> s.getStatus() == ScenarioStatus.COMPLETED)
                .filter(s -> s.getCompletedAt() != null && s.getCompletedAt().isAfter(startOfDay))
                .count();
        
        // Worker stats
        int activeWorkers = queueService.getActiveWorkerCount();
        
        // Calculate total requests today
        long totalRequestsToday = scenarios.stream()
                .filter(s -> s.getCompletedAt() != null && s.getCompletedAt().isAfter(startOfDay))
                .mapToLong(s -> {
                    ScenarioStats stats = metricsService.getAggregatedStats(s.getId());
                    return stats.getTotalRequests();
                })
                .sum();
        
        // Calculate average success rate
        double avgSuccessRate = scenarios.stream()
                .filter(s -> s.getStatus() == ScenarioStatus.COMPLETED)
                .filter(s -> s.getCompletedAt() != null && s.getCompletedAt().isAfter(startOfDay))
                .mapToDouble(s -> {
                    ScenarioStats stats = metricsService.getAggregatedStats(s.getId());
                    return stats.getSuccessRate();
                })
                .average()
                .orElse(0.0);
        
        // Get unacknowledged alerts
        List<Alert> alerts = alertRepository
                .findByUserIdAndAcknowledgedFalseOrderByCreatedAtDesc(userId);
        int activeAlerts = alerts.size();
        
        // Get recent scenarios
        List<RecentScenario> recentScenarios = scenarios.stream()
                .sorted((a, b) -> {
                    Instant aTime = a.getStartedAt() != null ? a.getStartedAt() : a.getCreatedAt();
                    Instant bTime = b.getStartedAt() != null ? b.getStartedAt() : b.getCreatedAt();
                    return bTime.compareTo(aTime);
                })
                .limit(5)
                .map(s -> {
                    ScenarioStats stats = metricsService.getAggregatedStats(s.getId());
                    return RecentScenario.builder()
                            .id(s.getId())
                            .name(s.getName())
                            .status(s.getStatus())
                            .successRate(stats.getSuccessRate())
                            .avgLatency(stats.getAvgLatencyMs())
                            .startedAt(s.getStartedAt())
                            .build();
                })
                .collect(Collectors.toList());
        
        DashboardSummary summary = DashboardSummary.builder()
                .totalScenarios(totalScenarios)
                .activeScenarios(activeScenarios)
                .completedToday(completedToday)
                .activeWorkers(activeWorkers)
                .totalRequestsToday(totalRequestsToday)
                .avgSuccessRate(avgSuccessRate)
                .activeAlerts(activeAlerts)
                .recentScenarios(recentScenarios)
                .build();
        
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/system-health")
    public ResponseEntity<SystemHealth> getSystemHealth() {
        int activeWorkers = queueService.getActiveWorkerCount();
        long taskQueueSize = queueService.getTaskQueueSize();
        long resultQueueSize = queueService.getResultQueueSize();
        
        List<String> workerIds = queueService.getActiveWorkerIds();
        List<WorkerMetrics> workerMetrics = workerIds.stream()
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
        
        SystemHealth health = SystemHealth.builder()
                .activeWorkers(activeWorkers)
                .taskQueueSize(taskQueueSize)
                .resultQueueSize(resultQueueSize)
                .workerMetrics(workerMetrics)
                .timestamp(Instant.now())
                .build();
        
        return ResponseEntity.ok(health);
    }
}

@Data
@Builder
class SystemHealth {
    private int activeWorkers;
    private long taskQueueSize;
    private long resultQueueSize;
    private List<WorkerMetrics> workerMetrics;
    private Instant timestamp;
}
