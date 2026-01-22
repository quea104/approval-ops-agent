package com.approval.opsagent.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRequestReq(
        @NotBlank String title,
        @NotBlank String inputText
) {}
