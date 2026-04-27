// ========== DashboardController.java ==========
package com.loadtest.controller;

import com.loadtest.dto.*;
import com.loadtest.model.*;
import com.loadtest.repository.*;
import com.loadtest.service.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
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
@Tag(name = "Dashboard", description = "API for retrieving aggregated dashboard metrics and system health")
@Slf4j
public class DashboardController {
    
    private final ScenarioRepository scenarioRepository;
    private final MetricRepository metricRepository;
    private final AlertRepository alertRepository;
    private final RedisQueueService queueService;
    private final MetricsAggregationService metricsService;
    
    @GetMapping("/summary")
    @Operation(summary = "Get Dashboard Summary", description = "Retrieves aggregated metrics for the current user's scenarios.")
    @RateLimiter(name = "dashboard")
    public ResponseEntity<DashboardSummary> getSummary(
            Authentication authentication,
            @Parameter(description = "Optional start date for filtering (ISO-8601)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "Optional end date for filtering (ISO-8601)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        
        String userId = (String) authentication.getPrincipal();
        log.info("Fetching dashboard summary for user: {}, startDate: {}, endDate: {}", userId, startDate, endDate);
        
        // Get all scenarios for user
        List<LoadTestScenario> scenarios = scenarioRepository.findByUserId(userId);
        
        // Count by status
        long totalScenarios = scenarios.size();
        long activeScenarios = scenarios.stream()
                .filter(s -> s.getStatus() == ScenarioStatus.RUNNING)
                .count();
        
        // Use provided dates or default to today
        Instant filterStart = startDate != null ? startDate : Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant filterEnd = endDate != null ? endDate : Instant.now();
        
        long completedInPeriod = scenarios.stream()
                .filter(s -> s.getStatus() == ScenarioStatus.COMPLETED)
                .filter(s -> s.getCompletedAt() != null && 
                        !s.getCompletedAt().isBefore(filterStart) && 
                        !s.getCompletedAt().isAfter(filterEnd))
                .count();
        
        // Worker stats
        int activeWorkers = queueService.getActiveWorkerCount();
        
        // Calculate total requests in period
        long totalRequestsInPeriod = scenarios.stream()
                .filter(s -> s.getCompletedAt() != null && 
                        !s.getCompletedAt().isBefore(filterStart) && 
                        !s.getCompletedAt().isAfter(filterEnd))
                .mapToLong(s -> {
                    ScenarioStats stats = metricsService.getAggregatedStats(s.getId());
                    return stats.getTotalRequests();
                })
                .sum();
        
        // Calculate average success rate
        double avgSuccessRate = scenarios.stream()
                .filter(s -> s.getStatus() == ScenarioStatus.COMPLETED)
                .filter(s -> s.getCompletedAt() != null && 
                        !s.getCompletedAt().isBefore(filterStart) && 
                        !s.getCompletedAt().isAfter(filterEnd))
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
                .completedToday(completedInPeriod) // Map to the period dynamically
                .activeWorkers(activeWorkers)
                .totalRequestsToday(totalRequestsInPeriod)
                .avgSuccessRate(avgSuccessRate)
                .activeAlerts(activeAlerts)
                .recentScenarios(recentScenarios)
                .build();
        
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/system-health")
    @Operation(summary = "Get System Health", description = "Retrieves current worker status and queue metrics.")
    @RateLimiter(name = "dashboard")
    public ResponseEntity<SystemHealth> getSystemHealth() {
        log.debug("Fetching system health metrics");
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

