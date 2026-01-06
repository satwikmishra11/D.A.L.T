package com.loadtest.controller;

import com.loadtest.dto.ApprovalRequest;
import com.loadtest.dto.ScenarioResponse;
import com.loadtest.model.LoadTestScenario;
import com.loadtest.service.ScenarioApprovalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scenarios/{id}/approval")
public class ScenarioApprovalController {

    private final ScenarioApprovalService service;

    public ScenarioApprovalController(ScenarioApprovalService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ScenarioResponse approve(
            @PathVariable String id,
            @RequestBody ApprovalRequest request,
            @RequestHeader("X-Actor") String actor
    ) {
        LoadTestScenario scenario =
                service.transition(
                        id,
                        request.getTargetStatus(),
                        actor,
                        request.getComment()
                );
        return new ScenarioResponse(scenario);
    }
}
