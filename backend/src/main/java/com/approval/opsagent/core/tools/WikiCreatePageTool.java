package com.approval.opsagent.core.tools;

import com.approval.opsagent.core.Tool;
import com.approval.opsagent.core.WorkRepo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WikiCreatePageTool implements Tool {
    private final WorkRepo repo;

    public WikiCreatePageTool(WorkRepo repo) {
        this.repo = repo;
    }

    @Override
    public String name() {
        return "wiki.createPage";
    }

    @Override
    public Object run(long requestId, Map<String, Object> args) {
        String title = String.valueOf(args.getOrDefault("title", "운영 요약"));

        // ✅ args.body가 비어있으면, 현재 requestId의 티켓을 기반으로 자동 본문 생성
        String body = String.valueOf(args.getOrDefault("body", "")).trim();
        if (body.isBlank()) {
            List<Map<String, Object>> tickets = repo.tickets(requestId);
            String ticketLines = tickets.stream()
                    .map(t -> "- " + t.get("title"))
                    .collect(Collectors.joining("\n"));

            body =
                    "## 요약\n" +
                            "- 요청에 따라 생성된 티켓을 기반으로 작업을 수행합니다.\n\n" +
                            "## 작업 티켓\n" +
                            (ticketLines.isBlank() ? "- (없음)\n" : ticketLines + "\n") +
                            "\n## 롤백/주의사항\n" +
                            "- 변경 작업은 승인 후 진행\n" +
                            "- 검증 완료 전 배포 확정 금지\n";
        }

        repo.insertWiki(requestId, title, body);
        return Map.of("created", 1, "title", title);
    }
}
