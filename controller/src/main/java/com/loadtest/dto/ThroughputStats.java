package com.loadtest.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThroughputStats {
    private double currentRps;
    private double avgRps;
    private double peakRps;
    private long totalBytesSent;
    private long totalBytesReceived;
}
