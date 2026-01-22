package com.approval.opsagent.api;

import com.approval.opsagent.api.dto.ApproveReq;
import com.approval.opsagent.api.dto.CreateRequestReq;
import com.approval.opsagent.core.ExecutionService;
import com.approval.opsagent.core.PlanningService;
import com.approval.opsagent.core.WorkRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class WorkController {

    private final WorkRepo repo;
    private final PlanningService planning;
    private final ExecutionService exec;

    public WorkController(WorkRepo repo, PlanningService planning, ExecutionService exec) {
        this.repo = repo;
        this.planning = planning;
        this.exec = exec;
    }

    // 간단 actor: 프론트에서 헤더로 넘기거나, 없으면 "demo"
    private String actor(String xActor) {
        return (xActor == null || xActor.isBlank()) ? "demo" : xActor.trim();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true);
    }

    /**
     * 요청 생성
     * @param xActor
     * @param req
     * @return
     */
    @PostMapping("/requests")
    public Map<String, Object> create(
            @RequestHeader(value = "X-Actor", required = false) String xActor,
            @Valid @RequestBody CreateRequestReq req
    ) {
        String actor = actor(xActor);
        long t0 = System.currentTimeMillis();
        long id = repo.create(actor, req.title(), req.inputText());
        repo.audit(id, actor, "CREATE", "created", true, (int) (System.currentTimeMillis() - t0));
        return Map.of("id", id);
    }

    /**
     * 요청 생성
     * @return
     */
    @GetMapping("/requests")
    public Object list() {
        return repo.list();
    }

    /**
     * 상세(요청/감사로그/티켓/위키)
     * @param id
     * @return
     */
    @GetMapping("/requests/{id}")
    public Object detail(@PathVariable long id) {
        return Map.of(
                "request", repo.find(id),
                "audit", repo.auditList(id),
                "tickets", repo.tickets(id),
                "wikiPages", repo.wiki(id)
        );
    }

    /**
     * 상세(요청/감사로그/티켓/위키)
     * @param xActor
     * @param id
     * @return
     * @throws JsonProcessingException
     */
    @PostMapping("/requests/{id}/plan")
    public Object plan(
            @RequestHeader(value = "X-Actor", required = false) String xActor,
            @PathVariable long id
    ) throws JsonProcessingException {
        return planning.plan(actor(xActor), id);
    }

    /**
     * AI plan 생성(RAG+LLM)
     * @param xActor
     * @param id
     * @param req
     * @return
     */
    @PostMapping("/requests/{id}/approve")
    public Object approve(
            @RequestHeader(value = "X-Actor", required = false) String xActor,
            @PathVariable long id,
            @Valid @RequestBody ApproveReq req
    ) {
        String actor = actor(xActor);
        long t0 = System.currentTimeMillis();

        boolean ok = "APPROVE".equalsIgnoreCase(req.decision());
        repo.approve(id, actor, ok);
        repo.audit(id, actor, ok ? "APPROVE" : "REJECT",
                req.comment() == null ? "" : req.comment(),
                true,
                (int) (System.currentTimeMillis() - t0)
        );

        return Map.of("id", id, "status", ok ? "APPROVED" : "REJECTED");
    }

    /**
     * plan 실행(ToolRegistry)
     * @param xActor
     * @param id
     * @return
     * @throws Exception
     */
    @PostMapping("/requests/{id}/execute")
    public Object execute(
            @RequestHeader(value = "X-Actor", required = false) String xActor,
            @PathVariable long id
    ) throws Exception {
        return exec.execute(actor(xActor), id);
    }

    /**
     * 통계
     * @return
     */
    @GetMapping("/ops/stats")
    public Object stats() {
        return repo.stats();
    }
}
