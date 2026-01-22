package com.approval.opsagent.ai.dto;

import java.util.List;
import java.util.Map;

public record PlanRequestPayload(
        long request_id,
        String requester,
        String title,
        String inputText,
        List<ToolSpec> tools,
        int top_k
) {
    public record ToolSpec(
            String name,
            String description,
            Map<String, Object> args_schema
    ) {}
}
