package com.loadtest.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class LoadProfile {
    private ProfileType type;
    private int initialRps;
    private int targetRps;
    private int rampUpSeconds;
    private List<BurstConfig> bursts;
}
