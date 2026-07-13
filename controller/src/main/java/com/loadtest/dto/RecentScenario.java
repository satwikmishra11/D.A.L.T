package com.loadtest.dto;

import com.loadtest.model.ScenarioStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentScenario {
    private String id;
    private String name;
    private ScenarioStatus status;
    private double successRate;
    private double avgLatency;
    private Instant startedAt;
}
