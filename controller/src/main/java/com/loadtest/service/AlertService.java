package com.loadtest.service;

import com.loadtest.model.*;
import com.loadtest.repository.AlertRepository;
import com.loadtest.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final ScenarioRepository scenarioRepository;

    public void checkSla(String scenarioId, ScenarioStats stats) {
        LoadTestScenario scenario = scenarioRepository.findById(scenarioId).orElse(null);
        if (scenario == null || scenario.getSlaConfig() == null) {
            return;
        }

        SlaConfig sla = scenario.getSlaConfig();
        List<String> violations = new ArrayList<>();

        if (stats.getSuccessRate() * 100 < sla.getMinSuccessRate()) {
            violations.add(String.format("Success rate %.2f%% is below SLA threshold of %.2f%%", 
                stats.getSuccessRate() * 100, sla.getMinSuccessRate()));
        }

        if (stats.getAvgLatencyMs() > sla.getMaxAvgLatencyMs()) {
            violations.add(String.format("Average latency %.2fms exceeds SLA threshold of %.2fms", 
                stats.getAvgLatencyMs(), sla.getMaxAvgLatencyMs()));
        }

        if (stats.getP95LatencyMs() > sla.getMaxP95LatencyMs()) {
            violations.add(String.format("P95 latency %.2fms exceeds SLA threshold of %.2fms", 
                stats.getP95LatencyMs(), sla.getMaxP95LatencyMs()));
        }

        if (stats.getP99LatencyMs() > sla.getMaxP99LatencyMs()) {
            violations.add(String.format("P99 latency %.2fms exceeds SLA threshold of %.2fms", 
                stats.getP99LatencyMs(), sla.getMaxP99LatencyMs()));
        }

        if (!violations.isEmpty()) {
            String message = String.join("; ", violations);
            log.warn("SLA breach detected for scenario {}: {}", scenarioId, message);
            
            // Check if we already raised an unacknowledged alert for this scenario to avoid spamming
            boolean exists = alertRepository.existsByScenarioIdAndAcknowledgedFalse(scenarioId);
            if (!exists) {
                Alert alert = Alert.builder()
                        .userId(scenario.getUserId())
                        .scenarioId(scenarioId)
                        .type("SLA_BREACH")
                        .severity("CRITICAL")
                        .title("SLA Violation - " + scenario.getName())
                        .message(message)
                        .acknowledged(false)
                        .createdAt(Instant.now())
                        .build();
                alertRepository.save(alert);
            }
        }
    }
}
