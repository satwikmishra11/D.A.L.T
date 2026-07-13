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
@Document(collection = "scheduled_tests")
public class ScheduledTest {
    @Id
    private String id;
    private String userId;
    private String scenarioId;
    private String name;
    private String cronExpression;
    private Boolean enabled = true;
    
    private Instant nextRunAt;
    private Instant lastRunAt;
    private String lastRunStatus;
    private String lastRunScenarioId;
    private Instant createdAt;
}
