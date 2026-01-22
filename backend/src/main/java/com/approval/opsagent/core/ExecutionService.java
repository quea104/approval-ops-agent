package com.approval.opsagent.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExecutionService {
    private final WorkRepo repo;
    private final ToolRegistry registry;
    private final ObjectMapper om = new ObjectMapper();

    public ExecutionService(WorkRepo repo, ToolRegistry registry) {
        this.repo = repo;
        this.registry = registry;
    }

    public Map<String, Object> execute(String actor, long requestId) throws Exception {
        long start = System.currentTimeMillis();

        Map<String, Object> wr = repo.find(requestId);
        String status = String.valueOf(wr.get("status"));

        if (!"APPROVED".equals(status)) {
            throw new IllegalStateException("승인 상태에서만 실행 가능. 현재=" + status);
        }
        // 만약 완료 상태가 있다면 추가 차단
        if ("DONE".equals(status) || "COMPLETED".equals(status)) {
            throw new IllegalStateException("실행 완료된 요청.");
        }

        repo.markExecuting(requestId);

        String planJson = (String) wr.get("plan_json");
        if (planJson == null || planJson.isBlank()) {
            throw new IllegalStateException("plan_json이 없어. 먼저 계획 생성해줘.");
        }

        List<Object> toolResults = new ArrayList<>();
        try {
            JsonNode root = om.readTree(planJson);
            JsonNode steps = root.path("steps");
            if (!steps.isArray()) throw new IllegalStateException("계획 형식 오류: steps 없음");

            for (JsonNode s : steps) {
                String toolName = s.path("tool").asText();
                Map<String, Object> args = om.convertValue(s.path("args"), new TypeReference<>() {});
                long t0 = System.currentTimeMillis();

                try {
                    Object r = registry.get(toolName).run(requestId, args);
                    repo.audit(requestId, actor, "TOOL", toolName + " ok", true, (int) (System.currentTimeMillis() - t0));
                    toolResults.add(Map.of("tool", toolName, "result", r));
                } catch (Exception e) {
                    repo.audit(requestId, actor, "TOOL", toolName + " fail: " + e.getMessage(), false, (int) (System.currentTimeMillis() - t0));
                    throw e;
                }
            }

            String resultJson = om.writeValueAsString(Map.of("toolResults", toolResults));
            repo.finish(requestId, true, resultJson);
            repo.audit(requestId, actor, "EXECUTE", "done", true, (int) (System.currentTimeMillis() - start));

            return Map.of(
                    "requestId", requestId,
                    "tickets", repo.tickets(requestId),
                    "wikiPages", repo.wiki(requestId)
            );

        } catch (Exception e) {
            repo.finish(requestId, false, "{\"error\":\"" + safe(e.getMessage()) + "\"}");
            repo.audit(requestId, actor, "EXECUTE", "failed: " + e.getMessage(), false, (int) (System.currentTimeMillis() - start));
            throw e;
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "'");
    }
}
