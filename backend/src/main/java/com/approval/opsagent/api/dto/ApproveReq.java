package com.approval.opsagent.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveReq(
        @NotBlank String decision, // APPROVE or REJECT
        String comment
) {}
