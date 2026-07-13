package com.loadtest.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatencyStats {
    private double avgMs;
    private double minMs;
    private double maxMs;
    private double p50Ms;
    private double p75Ms;
    private double p90Ms;
    private double p95Ms;
    private double p99Ms;
    private double p999Ms;
}
