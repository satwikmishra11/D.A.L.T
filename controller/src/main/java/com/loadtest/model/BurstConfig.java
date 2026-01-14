package com.loadtest.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class BurstConfig {
    private int startSecond;
    private int durationSeconds;
    private int rps;
}
