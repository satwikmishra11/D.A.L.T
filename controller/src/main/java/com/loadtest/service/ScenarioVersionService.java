package com.loadtest.service;

import com.loadtest.model.LoadTestScenario;
import com.loadtest.model.ScenarioVersion;
import com.loadtest.repository.ScenarioRepository;
import com.loadtest.repository.ScenarioVersionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScenarioVersionService {

    private final ScenarioVersionRepository versionRepo;
    private final ScenarioRepository scenarioRepo;

    public ScenarioVersionService(
            ScenarioVersionRepository versionRepo,
            ScenarioRepository scenarioRepo
    ) {
        this.versionRepo = versionRepo;
        this.scenarioRepo = scenarioRepo;
    }

    public void snapshot(LoadTestScenario scenario) {
        int nextVersion = scenario.getVersion() + 1;

        ScenarioVersion version = new ScenarioVersion(
                scenario.getId(),
                nextVersion,
                scenario.getConfigJson()
        );

        scenario.setVersion(nextVersion);

        versionRepo.save(version);
        scenarioRepo.save(scenario);
    }

    public List<ScenarioVersion> getVersions(String scenarioId) {
        return versionRepo.findByScenarioIdOrderByVersionDesc(scenarioId);
    }

    public ScenarioVersion getVersion(String scenarioId, int version) {
        return versionRepo.findByScenarioIdAndVersion(scenarioId, version)
                .orElseThrow(() ->
                        new RuntimeException("Scenario version not found"));
    }
}
