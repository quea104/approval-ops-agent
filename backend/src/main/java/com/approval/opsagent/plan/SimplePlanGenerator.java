package com.approval.opsagent.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SimplePlanGenerator implements PlanGenerator {
    private static final Pattern COUNT = Pattern.compile("(\\d+)\\s*개");
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String makePlanJson(String title, String inputText) {
        int count = extractCount(inputText, 5);

        String desc = "점검 항목: 보호구, 추락방지, 전기, 장비, 화재 (요청 기반)";
        String wikiBody = """
요청 요약:
- %s

생성 내용:
- 안전점검 티켓 %d개 생성
- 점검 요약 문서 작성

주의:
- 승인 전 실행 금지
- 근거 없는 내용은 추가 확인 필요
""".formatted(inputText, count);

        Map<String, Object> plan = Map.of(
                "goal", title,
                "steps", List.of(
                        Map.of("id", 1, "tool", "ticket.createMany",
                                "args", Map.of("count", count, "titlePrefix", title, "desc", desc)),
                        Map.of("id", 2, "tool", "wiki.createPage",
                                "args", Map.of("title", title + " 요약", "body", wikiBody))
                ),
                "riskNotes", List.of("승인 전 실행 금지", "근거 없는 내용 생성 주의")
        );

        try {
            return om.writeValueAsString(plan);
        } catch (Exception e) {
            // 최악의 경우에도 실행 가능한 기본값 반환
            return """
{"goal":"%s","steps":[{"id":1,"tool":"ticket.createMany","args":{"count":%d}},{"id":2,"tool":"wiki.createPage","args":{"title":"요약","body":"요약"}}]}
""".formatted(title, count);
        }
    }

    private int extractCount(String s, int def) {
        if (s == null) return def;
        Matcher m = COUNT.matcher(s);
        if (m.find()) {
            try {
                int v = Integer.parseInt(m.group(1));
                return Math.max(1, Math.min(v, 50));
            } catch (Exception ignored) {}
        }
        return def;
    }
}
