package com.loadtest.service;

import com.loadtest.model.LoadTestScenario;
import com.loadtest.model.ScenarioVersion;
import com.loadtest.repository.ScenarioRepository;
import com.loadtest.repository.ScenarioVersionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;

@Service
public class ScenarioVersionService {

    private final ScenarioVersionRepository versionRepo;
    private final ScenarioRepository scenarioRepo;
    private final ObjectMapper mapper;

    public ScenarioVersionService(
            ScenarioVersionRepository versionRepo,
            ScenarioRepository scenarioRepo
    ) {
        this.versionRepo = versionRepo;
        this.scenarioRepo = scenarioRepo;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @CacheEvict(value = "scenario_versions", key = "#scenario.id")
    public void snapshot(LoadTestScenario scenario) {
        int nextVersion = scenario.getVersion() + 1;

        // Ensure configJson is populated
        try {
            scenario.setConfigJson(mapper.writeValueAsString(scenario));
        } catch (Exception e) {
            // fallback
        }

        ScenarioVersion version = new ScenarioVersion(
                scenario.getId(),
                nextVersion,
                scenario.getConfigJson()
        );

        scenario.setVersion(nextVersion);

        versionRepo.save(version);
        scenarioRepo.save(scenario);
    }

    @Cacheable(value = "scenario_versions", key = "#scenarioId")
    public List<ScenarioVersion> getVersions(String scenarioId) {
        return versionRepo.findByScenarioIdOrderByVersionDesc(scenarioId);
    }

    @Cacheable(value = "scenario_version_detail", key = "#scenarioId + '-' + #version")
    public ScenarioVersion getVersion(String scenarioId, int version) {
        return versionRepo.findByScenarioIdAndVersion(scenarioId, version)
                .orElseThrow(() ->
                        new RuntimeException("Scenario version not found"));
    }

    @CacheEvict(value = "scenario_versions", key = "#scenarioId")
    public LoadTestScenario rollback(String scenarioId, int targetVersionNumber) {
        LoadTestScenario scenario = scenarioRepo.findById(scenarioId)
                .orElseThrow(() -> new RuntimeException("Scenario not found"));

        ScenarioVersion targetVersion = getVersion(scenarioId, targetVersionNumber);

        try {
            LoadTestScenario targetConfig = mapper.readValue(targetVersion.getConfigJson(), LoadTestScenario.class);

            scenario.setName(targetConfig.getName());
            scenario.setTargetUrl(targetConfig.getTargetUrl());
            scenario.setMethod(targetConfig.getMethod());
            scenario.setHeaders(targetConfig.getHeaders());
            scenario.setBody(targetConfig.getBody());
            scenario.setDurationSeconds(targetConfig.getDurationSeconds());
            scenario.setNumWorkers(targetConfig.getNumWorkers());
            scenario.setLoadProfile(targetConfig.getLoadProfile());
            scenario.setSlaConfig(targetConfig.getSlaConfig());
            scenario.setIgnoreTlsErrors(targetConfig.isIgnoreTlsErrors());
            scenario.setConfigJson(targetVersion.getConfigJson());

        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize scenario config for rollback", e);
        }

        int nextVersion = scenario.getVersion() + 1;
        scenario.setVersion(nextVersion);

        ScenarioVersion rollbackVersion = new ScenarioVersion(
                scenarioId,
                nextVersion,
                scenario.getConfigJson(),
                true
        );

        versionRepo.save(rollbackVersion);
        return scenarioRepo.save(scenario);
    }
}