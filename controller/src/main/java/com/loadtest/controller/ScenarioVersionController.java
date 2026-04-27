package com.loadtest.controller;

import com.loadtest.dto.RollbackRequest;
import com.loadtest.dto.ScenarioResponse;
import com.loadtest.model.LoadTestScenario;
import com.loadtest.model.ScenarioVersion;
import com.loadtest.service.ScenarioVersionService;
import com.loadtest.utils.JsonDiffUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/scenarios/{id}/versions")
@Tag(name = "Scenario Versions", description = "API for managing and viewing scenario version history")
@Slf4j
public class ScenarioVersionController {

    private final ScenarioVersionService versionService;

    public ScenarioVersionController(ScenarioVersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping
    @Operation(summary = "List Versions", description = "Retrieves all versions of a specific scenario")
    public List<ScenarioVersion> list(
            @Parameter(description = "Scenario ID") @PathVariable String id) {
        log.info("Fetching versions for scenario id: {}", id);
        return versionService.getVersions(id);
    }

    @GetMapping("/diff")
    @Operation(summary = "Compare Versions", description = "Compares two scenario versions and returns the JSON diff")
    public Map<String, Object> diff(
            @Parameter(description = "Scenario ID") @PathVariable String id,
            @Parameter(description = "First version number") @RequestParam int v1,
            @Parameter(description = "Second version number") @RequestParam int v2
    ) throws Exception {
        log.info("Comparing versions {} and {} for scenario id: {}", v1, v2, id);

        ScenarioVersion oldV = versionService.getVersion(id, v1);
        ScenarioVersion newV = versionService.getVersion(id, v2);

        return JsonDiffUtil.diff(
                oldV.getConfigJson(),
                newV.getConfigJson()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rollback")
    @Operation(summary = "Rollback Scenario", description = "Rolls back a scenario to a specific target version. Requires ADMIN role.")
    public ScenarioResponse rollback(
            @Parameter(description = "Scenario ID") @PathVariable String id,
            @RequestBody RollbackRequest request
    ) {
        log.info("Rolling back scenario {} to version {}", id, request.getTargetVersion());
        LoadTestScenario scenario =
                versionService.rollback(id, request.getTargetVersion());

        return new ScenarioResponse(scenario);
    }
}
