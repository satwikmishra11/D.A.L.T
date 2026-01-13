package com.loadtest.controller;

import com.loadtest.dto.ApprovalRequest;
import com.loadtest.dto.ScenarioResponse;
import com.loadtest.model.LoadTestScenario;
import com.loadtest.service.ScenarioApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scenarios/{id}/approval")
@Tag(name = "Scenario Approval", description = "API for approving or rejecting load test scenarios")
public class ScenarioApprovalController {

    private final ScenarioApprovalService service;

    public ScenarioApprovalController(ScenarioApprovalService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve or Reject Scenario", description = "Transitions the approval status of a load test scenario. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = ScenarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition or input",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Scenario not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content)
    })
    public ScenarioResponse approve(
            @Parameter(description = "ID of the scenario to update") @PathVariable String id,
            @Valid @RequestBody ApprovalRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-Actor", required = false) String actor
    ) {
        // Fallback for actor if not provided in header (e.g., from SecurityContext)
        String actualActor = (actor != null && !actor.isEmpty()) ? actor : "system";
        
        LoadTestScenario scenario =
                service.transition(
                        id,
                        request.getTargetStatus(),
                        actualActor,
                        request.getComment()
                );
        return new ScenarioResponse(scenario);
    }
}
