package com.loadtest.repository;

import com.loadtest.model.LoadTestScenario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository
        extends MongoRepository<LoadTestScenario, String> {

    List<LoadTestScenario> findByOrganizationId(String organizationId);

    Optional<LoadTestScenario> findByIdAndOrganizationId(
            String id,
            String organizationId
    );
}
