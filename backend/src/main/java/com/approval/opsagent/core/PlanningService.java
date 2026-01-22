package com.approval.opsagent.core;

import com.approval.opsagent.ai.AiServiceClient;
import com.approval.opsagent.ai.dto.PlanRequestPayload;
import com.approval.opsagent.plan.PlanGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlanningService {

    private final WorkRepo repo;
    private final PlanGenerator generator;
    private final AiServiceClient ai;
    private final ObjectMapper om = new ObjectMapper();

    public PlanningService(WorkRepo repo, PlanGenerator generator, AiServiceClient ai) {
        this.repo = repo;
        this.generator = generator;
        this.ai = ai;
    }

    public Map<String, Object> plan(String actor, long requestId) throws JsonProcessingException {
        long t0 = System.currentTimeMillis();

        Map<String, Object> wr = repo.find(requestId);
        String title = String.valueOf(wr.get("title"));
        String input = String.valueOf(wr.get("input_text"));

        // 하드코딩
        // String planJson = generator.makePlanJson(title, input);

        // ✅ 1) LLM에게 "허용된 도구 목록 + 인자 스키마" 제공 (에이전트의 도구 목록)
        List<PlanRequestPayload.ToolSpec> tools = toolsForAi();

        // requester는 DB에 없을 수도 있으니 actor로 통일(안전)
        var payload = new PlanRequestPayload(
                requestId,
                actor,
                title,
                input,
                tools,
                4
        );

        // ✅ 2) AI 서비스 호출 (Spring Boot -> REST -> AI Agent)
        String aiJson = ai.createPlan(payload);
        JsonNode aiResult = om.readTree(aiJson);
        JsonNode planNode = aiResult.path("plan");
        if (planNode.isMissingNode() || planNode.isNull()) {
            throw new IllegalStateException("AI plan missing. aiResult=" + aiResult);
        }

        // ✅ 3) 최소 안전장치: 허용된 tool만 쓰는지 검증(진짜 실무 포인트)
        validatePlan(planNode, tools);

        // ✅ 4) plan JSON 문자열로 저장
        String planJson;
        try {
            planJson = om.writeValueAsString(planNode);
        } catch (Exception e) {
            throw new IllegalStateException("plan json serialize failed: " + e.getMessage(), e);
        }

        repo.savePlan(requestId, planJson);
        repo.audit(requestId, actor, "PLAN", "plan created", true, (int) (System.currentTimeMillis() - t0));

        return Map.of(
                "requestId", requestId,
                "planJson", planJson
        );
    }

    private void validatePlan(JsonNode plan, List<PlanRequestPayload.ToolSpec> toolSpecs) {
        Set<String> allowed = new HashSet<>();
        for (PlanRequestPayload.ToolSpec t : toolSpecs) allowed.add(t.name());

        JsonNode steps = plan.path("steps");
        if (!steps.isArray()) {
            throw new IllegalArgumentException("계획 형식 오류: steps 없음");
        }

        for (JsonNode s : steps) {
            String tool = s.path("tool").asText(null);
            if (tool == null || tool.isBlank()) {
                throw new IllegalArgumentException("계획 형식 오류: step.tool 없음");
            }
            if (!allowed.contains(tool)) {
                throw new IllegalArgumentException("허용되지 않은 tool: " + tool);
            }
        }
    }

    private List<PlanRequestPayload.ToolSpec> toolsForAi() {
        // ✅ LLM이 스키마(type/properties/required)를 args로 복사하지 않도록
        //    args_schema를 "값 예시 형태"로 단순화

        Map<String, Object> ticketSchema = Map.of(
                "mode", "string (items | count) - 선택",
                "items", List.of(
                        Map.of(
                                "title", "string (예: ApprovalOpsAgent - 원인분석)",
                                "desc", "string (예: 배포 오류 원인 파악: 로그/지표/재현 조건 정리)"
                        )
                ),
                "count", "number (예: 5)",
                "titlePrefix", "string (예: 이번 주 안전점검)",
                "desc", "string (예: 점검 항목 확인)"
        );

        Map<String, Object> wikiSchema = Map.of(
                "title", "string (예: ApprovalOpsAgent - 배포 후 오류 대응)",
                "body", "string (예: 변경 배경/영향도/롤백 방법을 포함한 본문)"
        );

        return List.of(
                new PlanRequestPayload.ToolSpec(
                        "ticket.createMany",
                        "티켓을 여러 개 생성한다. args는 다음 중 하나의 형태로 작성한다.\n" +
                                "A) items 방식(서로 다른 티켓): {\"items\":[{\"title\":\"...\",\"desc\":\"...\"}, ...]}\n" +
                                "B) count 방식(유사 티켓 N개): {\"count\":5,\"titlePrefix\":\"...\",\"desc\":\"...\"}\n" +
                                "args에 type/properties/required 같은 스키마 정의는 넣지 말 것.",
                        ticketSchema
                ),
                new PlanRequestPayload.ToolSpec(
                        "wiki.createPage",
                        "위키 문서를 생성한다. args는 반드시 {\"title\":\"...\",\"body\":\"...\"} 형태의 '값'만 포함해야 한다. " +
                                "type/properties/required 같은 스키마 정의를 args에 절대 넣지 말 것." +
                                "body에는 변경 배경/영향도/롤백 방법을 포함할 것.",
                        wikiSchema
                )
        );
    }
}
