package com.loadtest.service;

import com.loadtest.model.*;
import com.loadtest.repository.*;
import org.springframework.stereotype.Service;

@Service
public class ScenarioApprovalService {

    private final ScenarioRepository scenarioRepo;
    private final ScenarioApprovalEventRepository eventRepo;

    public ScenarioApprovalService(
            ScenarioRepository scenarioRepo,
            ScenarioApprovalEventRepository eventRepo
    ) {
        this.scenarioRepo = scenarioRepo;
        this.eventRepo = eventRepo;
    }

    public LoadTestScenario transition(
            String scenarioId,
            ApprovalStatus target,
            String actor,
            String comment
    ) {
        LoadTestScenario scenario = scenarioRepo.findById(scenarioId)
                .orElseThrow(() ->
                        new RuntimeException("Scenario not found"));

        ApprovalStatus from = scenario.getApprovalStatus();

        validateTransition(from, target);

        scenario.setApprovalStatus(target);
        scenario.setApprovedBy(actor);
        scenario.setApprovalComment(comment);

        scenarioRepo.save(scenario);

        eventRepo.save(new ScenarioApprovalEvent(
                scenarioId,
                from,
                target,
                actor,
                comment
        ));

        return scenario;
    }

    private void validateTransition(
            ApprovalStatus from,
            ApprovalStatus to
    ) {
        if (from == ApprovalStatus.APPROVED && to == ApprovalStatus.DRAFT) {
            throw new RuntimeException("Cannot revert approved scenario");
        }
    }
}
