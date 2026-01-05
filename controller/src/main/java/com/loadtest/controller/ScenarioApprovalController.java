package com.loadtest.controller;

import com.loadtest.dto.ApprovalRequest;
import com.loadtest.dto.ScenarioResponse;
import com.loadtest.model.LoadTestScenario;
import com.loadtest.service.ScenarioApprovalService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scenarios/{id}/approval")
public class ScenarioApprovalController {

    private final ScenarioApprovalService approvalService;

    public ScenarioApprovalController(
            ScenarioApprovalService approvalService
    ) {
        this.approvalService = approvalService;
    }

    @PostMapping
    public ScenarioResponse approve(
            @PathVariable String id,
            @RequestBody ApprovalRequest request,
            @RequestHeader("X-Actor") String actor
    ) {
        LoadTestScenario scenario =
                approvalService.transition(
                        id,
                        request.getTargetStatus(),
                        actor,
                        request.getComment()
                );

        return new ScenarioResponse(scenario);
    }
}
