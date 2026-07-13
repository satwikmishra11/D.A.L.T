package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadProfile {
    private ProfileType type;
    private int initialRps;
    private int targetRps;
    private int rampUpSeconds;
    private List<BurstConfig> bursts;
}
