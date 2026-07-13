package com.loadtest.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private String scenarioId;
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private double successRate;
    private LatencyStats latency;
    private ThroughputStats throughput;
    private Map<Integer, Long> statusCodeDistribution;
    private Map<String, Long> errorTypeDistribution;
    private List<TimeSeriesPoint> timeSeries;
    private SlaStatus slaStatus;
    private Instant lastUpdated;
}
