package com.loadtest.dto;

import com.loadtest.model.ApprovalStatus;

public class ApprovalRequest {

    private ApprovalStatus targetStatus;
    private String comment;

    public ApprovalStatus getTargetStatus() {
        return targetStatus;
    }

    public String getComment() {
        return comment;
    }
}
