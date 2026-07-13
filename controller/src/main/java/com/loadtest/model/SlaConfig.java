package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaConfig {
    private double minSuccessRate = 99.0;
    private double maxAvgLatencyMs = 500.0;
    private double maxP95LatencyMs = 1000.0;
    private double maxP99LatencyMs = 2000.0;
    private double maxErrorRate = 1.0;
}
