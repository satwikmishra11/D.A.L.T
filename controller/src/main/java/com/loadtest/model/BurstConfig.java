package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurstConfig {
    private int startSecond;
    private int durationSeconds;
    private int rps;
}
