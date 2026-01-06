package com.loadtest.controller;

import com.loadtest.model.ScenarioVersion;
import com.loadtest.service.ScenarioVersionService;
import com.loadtest.utils.JsonDiffUtil;
import org.springframework.web.bind.annotation.*;
import com.loadtest.dto.RollbackRequest;
import com.loadtest.dto.ScenarioResponse;
import com.loadtest.model.LoadTestScenario;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scenarios/{id}/versions")
public class ScenarioVersionController {

    private final ScenarioVersionService versionService;

    public ScenarioVersionController(ScenarioVersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping
    public List<ScenarioVersion> list(@PathVariable String id) {
        return versionService.getVersions(id);
    }

    @GetMapping("/diff")
    public Map<String, Object> diff(
            @PathVariable String id,
            @RequestParam int v1,
            @RequestParam int v2
    ) throws Exception {

        ScenarioVersion oldV = versionService.getVersion(id, v1);
        ScenarioVersion newV = versionService.getVersion(id, v2);

        return JsonDiffUtil.diff(
                oldV.getConfigJson(),
                newV.getConfigJson()
        );
    }
}
@PostMapping("/rollback")
public ScenarioResponse rollback(
        @PathVariable String id,
        @RequestBody RollbackRequest request
) {
    LoadTestScenario scenario =
            versionService.rollback(id, request.getTargetVersion());

    return new ScenarioResponse(scenario);
}

@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/rollback")
public ScenarioResponse RollbackRequest rollback(
        @PathVariable String id,
        @RequestBody RollbackRequest request
) {
    LoadTestScenario scenario =
            versionService.rollback(id, request.getTargetVersion());

    return new ScenarioResponse(scenario);
}
