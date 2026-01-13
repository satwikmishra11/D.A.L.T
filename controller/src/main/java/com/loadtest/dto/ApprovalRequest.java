package com.loadtest.dto;

import com.loadtest.model.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    @NotNull(message = "Target status is required")
    private ApprovalStatus targetStatus;

    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;
}
