package com.loadtest.controller;

import com.loadtest.dto.CreateScenarioRequest;
import com.loadtest.model.*;
import com.loadtest.repository.ScenarioRepository;
import com.loadtest.service.LoadTestOrchestrationService;
import com.loadtest.service.MetricsAggregationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/scenarios")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ScenarioController {

    private final ScenarioRepository scenarioRepository;
    private final LoadTestOrchestrationService orchestrationService;
    private final MetricsAggregationService metricsService;

    @PostMapping
    public ResponseEntity<LoadTestScenario> create(
            Authentication authentication,
            @Valid @RequestBody CreateScenarioRequest request) {
        String userId = (String) authentication.getPrincipal();
        log.info("Creating scenario for user {}: {}", userId, request.getName());

        LoadProfile profile = null;
        if (request.getLoadProfile() != null) {
            profile = LoadProfile.builder()
                    .type(request.getLoadProfile().getType())
                    .initialRps(request.getLoadProfile().getInitialRps())
                    .targetRps(request.getLoadProfile().getTargetRps())
                    .rampUpSeconds(request.getLoadProfile().getRampUpSeconds())
                    .bursts(request.getLoadProfile().getBursts() == null ? null :
                            request.getLoadProfile().getBursts().stream()
                                    .map(b -> BurstConfig.builder()
                                            .startSecond(b.getStartSecond())
                                            .durationSeconds(b.getDurationSeconds())
                                            .rps(b.getRps())
                                            .build())
                                    .collect(Collectors.toList()))
                    .build();
        }

        SlaConfig sla = null;
        if (request.getSlaConfig() != null) {
            sla = SlaConfig.builder()
                    .minSuccessRate(request.getSlaConfig().getMinSuccessRate())
                    .maxAvgLatencyMs(request.getSlaConfig().getMaxAvgLatencyMs())
                    .maxP95LatencyMs(request.getSlaConfig().getMaxP95LatencyMs())
                    .maxP99LatencyMs(request.getSlaConfig().getMaxP99LatencyMs())
                    .maxErrorRate(request.getSlaConfig().getMaxErrorRate())
                    .build();
        }

        LoadTestScenario scenario = LoadTestScenario.builder()
                .userId(userId)
                .name(request.getName())
                .targetUrl(request.getTargetUrl())
                .method(request.getMethod())
                .headers(request.getHeaders())
                .body(request.getBody())
                .durationSeconds(request.getDurationSeconds())
                .numWorkers(request.getNumWorkers())
                .loadProfile(profile)
                .slaConfig(sla)
                .status(ScenarioStatus.DRAFT)
                .approvalStatus(ApprovalStatus.APPROVED) // Auto-approve so they can be run immediately
                .ignoreTlsErrors(request.getIgnoreTlsErrors() != null ? request.getIgnoreTlsErrors() : false)
                .createdAt(Instant.now())
                .build();

        LoadTestScenario saved = scenarioRepository.save(scenario);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<LoadTestScenario>> getAll(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<LoadTestScenario> scenarios = scenarioRepository.findByUserId(userId);
        return ResponseEntity.ok(scenarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoadTestScenario> getById(
            Authentication authentication,
            @PathVariable String id) {
        String userId = (String) authentication.getPrincipal();
        LoadTestScenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));
        
        if (!scenario.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(scenario);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<StartResponse> start(
            Authentication authentication,
            @PathVariable String id) {
        String userId = (String) authentication.getPrincipal();
        LoadTestScenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        if (!scenario.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        String executionId = orchestrationService.startScenario(id);
        scenario.setStatus(ScenarioStatus.RUNNING);
        scenarioRepository.save(scenario);

        return ResponseEntity.ok(new StartResponse(executionId));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stop(
            Authentication authentication,
            @PathVariable String id) {
        String userId = (String) authentication.getPrincipal();
        LoadTestScenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        if (!scenario.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        if (scenario.getLastExecutionId() != null) {
            orchestrationService.stopScenario(scenario.getLastExecutionId());
        }

        scenario.setRunning(false);
        scenario.setStatus(ScenarioStatus.CANCELLED);
        scenarioRepository.save(scenario);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/stats/realtime")
    public ResponseEntity<ScenarioStats> getStats(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int lastNSeconds) {
        String userId = (String) authentication.getPrincipal();
        LoadTestScenario scenario = scenarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        if (!scenario.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        ScenarioStats stats = metricsService.getRealTimeStats(id, lastNSeconds);
        return ResponseEntity.ok(stats);
    }

    @lombok.Value
    public static class StartResponse {
        String executionId;
    }
}
