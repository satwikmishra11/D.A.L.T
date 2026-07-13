package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestReport {
    private String scenarioId;
    private String userId;
    private String title;
    private String summary;
    private ScenarioStats stats;
    private List<String> insights;
    private List<String> recommendations;
    private Instant generatedAt;
}
