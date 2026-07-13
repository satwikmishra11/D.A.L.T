package com.loadtest.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    private long totalScenarios;
    private long activeScenarios;
    private long completedToday;
    private int activeWorkers;
    private long totalRequestsToday;
    private double avgSuccessRate;
    private int activeAlerts;
    private List<RecentScenario> recentScenarios;
}
