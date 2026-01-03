package com.loadtest.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "scenario_versions")
public class ScenarioVersion {

    @Id
    private String id;

    private String scenarioId;
    private int version;
    private String configJson;
    private Instant createdAt;

    public ScenarioVersion(String scenarioId, int version, String configJson) {
        this.scenarioId = scenarioId;
        this.version = version;
        this.configJson = configJson;
        this.createdAt = Instant.now();
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public int getVersion() {
        return version;
    }

    public String getConfigJson() {
        return configJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
