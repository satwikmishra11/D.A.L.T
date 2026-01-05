package com.loadtest.repository;

import com.loadtest.model.ScenarioApprovalEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ScenarioApprovalEventRepository
        extends MongoRepository<ScenarioApprovalEvent, String> {

    List<ScenarioApprovalEvent> findByScenarioIdOrderByTimestampDesc(
            String scenarioId
    );
}
