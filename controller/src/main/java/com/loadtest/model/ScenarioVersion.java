package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scenario_versions")
public class ScenarioVersion {
    @Id
    private String id;
    private String scenarioId;
    private int version;
    private String configJson;
    private boolean rollback;
    private Instant createdAt = Instant.now();

    public ScenarioVersion(String scenarioId, int version, String configJson, boolean rollback) {
        this.scenarioId = scenarioId;
        this.version = version;
        this.configJson = configJson;
        this.rollback = rollback;
        this.createdAt = Instant.now();
    }

    public ScenarioVersion(String scenarioId, int version, String configJson) {
        this.scenarioId = scenarioId;
        this.version = version;
        this.configJson = configJson;
        this.rollback = false;
        this.createdAt = Instant.now();
    }
}
