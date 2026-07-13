package com.loadtest.dto;

import com.loadtest.model.SlaConfig;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaStatus {
    private boolean violated;
    private List<String> violations;
    private SlaConfig config;
}
