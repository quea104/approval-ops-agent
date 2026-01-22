package com.approval.opsagent.core.tools;

import com.approval.opsagent.core.Tool;
import com.approval.opsagent.core.WorkRepo;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TicketCreateManyTool implements Tool {
    private final WorkRepo repo;

    public TicketCreateManyTool(WorkRepo repo) {
        this.repo = repo;
    }

    @Override
    public String name() {
        return "ticket.createMany";
    }

    @Override
    public Object run(long requestId, Map<String, Object> args) {
        Object itemsObj = args.get("items");
        if (itemsObj instanceof List<?> list && !list.isEmpty()) {
            int created = 0;
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) continue;

                Object titleObj = m.get("title");
                String title = (titleObj == null) ? "작업 티켓" : String.valueOf(titleObj);

                Object descObj = m.get("desc");
                String desc = (descObj == null) ? "" : String.valueOf(descObj);

                repo.insertTicket(requestId, title, desc);
                created++;
            }
            return Map.of("created", created);
        }

        int count = ((Number) args.getOrDefault("count", 3)).intValue();
        String prefix = String.valueOf(args.getOrDefault("titlePrefix", "운영 작업"));
        String desc = String.valueOf(args.getOrDefault("desc", "작업 내용"));

        int base = repo.tickets(requestId).size(); // 이미 생성된 티켓 수
        for (int i = 1; i <= count; i++) {
            int n = base + i;
            repo.insertTicket(requestId, prefix + " #" + n, desc + " (단계 " + n + ")");
        }
        return Map.of("created", count);
    }
}
