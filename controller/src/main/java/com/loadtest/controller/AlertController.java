package com.loadtest.controller;

import com.loadtest.model.Alert;
import com.loadtest.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertRepository alertRepository;

    @GetMapping("/unacknowledged")
    public ResponseEntity<List<Alert>> getUnacknowledged(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<Alert> alerts = alertRepository.findByUserIdAndAcknowledgedFalseOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/scenario/{scenarioId}")
    public ResponseEntity<List<Alert>> getForScenario(@PathVariable String scenarioId) {
        List<Alert> alerts = alertRepository.findByScenarioId(scenarioId);
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledge(@PathVariable String id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setAcknowledged(true);
        Alert saved = alertRepository.save(alert);
        return ResponseEntity.ok(saved);
    }
}
