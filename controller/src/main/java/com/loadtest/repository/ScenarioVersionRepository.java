package com.loadtest.repository;

import com.loadtest.model.ScenarioVersion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioVersionRepository
        extends MongoRepository<ScenarioVersion, String> {

    List<ScenarioVersion> findByScenarioIdOrderByVersionDesc(String scenarioId);

    Optional<ScenarioVersion> findByScenarioIdAndVersion(
            String scenarioId,
            int version
    );
}
