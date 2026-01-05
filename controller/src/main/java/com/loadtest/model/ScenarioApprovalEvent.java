package com.loadtest.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "scenario_approval_events")
public class ScenarioApprovalEvent {

    @Id
    private String id;

    private String scenarioId;
    private ApprovalStatus fromStatus;
    private ApprovalStatus toStatus;
    private String actor;
    private String comment;
    private Instant timestamp = Instant.now();

    public ScenarioApprovalEvent(
            String scenarioId,
            ApprovalStatus fromStatus,
            ApprovalStatus toStatus,
            String actor,
            String comment
    ) {
        this.scenarioId = scenarioId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.comment = comment;
    }
}
