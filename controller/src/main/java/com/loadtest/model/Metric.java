package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@Document(collection = "metrics")
public class Metric {
    @Id
    private String id;
    private String scenarioId;
    private String workerId;
    
    private Instant timestamp;
    private long latencyMs;
    private int statusCode;
    private boolean success;
    private String errorMessage;
    
    // Aggregated data
    private int requestCount;
}
