package com.approval.opsagent.core;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class WorkRepo {
    private final JdbcTemplate jdbc;

    public WorkRepo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long create(String requester, String title, String inputText) {
        var kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO work_request(requester,title,input_text,status) VALUES (?,?,?, 'DRAFT')",
                    new String[]{"id"}
            );
            ps.setString(1, requester);
            ps.setString(2, title);
            ps.setString(3, inputText);
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public List<Map<String, Object>> list() {
        return jdbc.queryForList("""
      SELECT id, title, status, requester, created_at, updated_at
      FROM work_request
      ORDER BY id DESC
    """);
    }

    public Map<String, Object> find(long id) {
        return jdbc.queryForMap("SELECT * FROM work_request WHERE id=?", id);
    }

    public void savePlan(long id, String planJson) {
        jdbc.update("""
      UPDATE work_request
      SET plan_json=?, status='PLANNED', updated_at=now()
      WHERE id=?
    """, planJson, id);
    }

    public void approve(long id, String approver, boolean ok) {
        jdbc.update("""
      UPDATE work_request
      SET status=?, approved_by=?, approved_at=now(), updated_at=now()
      WHERE id=?
    """, ok ? "APPROVED" : "REJECTED", approver, id);
    }

    public void markExecuting(long id) {
        jdbc.update("""
      UPDATE work_request
      SET status='EXECUTING', updated_at=now()
      WHERE id=?
    """, id);
    }

    public void finish(long id, boolean ok, String resultJson) {
        jdbc.update("""
      UPDATE work_request
      SET status=?, result_json=?, executed_at=now(), updated_at=now()
      WHERE id=?
    """, ok ? "DONE" : "FAILED", resultJson, id);
    }

    public void audit(long requestId, String actor, String action, String message, boolean success, int latencyMs) {
        jdbc.update("""
      INSERT INTO audit_log(request_id, actor, action, message, success, latency_ms)
      VALUES (?,?,?,?,?,?)
    """, requestId, actor, action, message, success, latencyMs);
    }

    public List<Map<String, Object>> auditList(long requestId) {
        return jdbc.queryForList("SELECT * FROM audit_log WHERE request_id=? ORDER BY id ASC", requestId);
    }

    public void insertTicket(long requestId, String title, String desc) {
        jdbc.update("INSERT INTO ticket(request_id,title,description) VALUES (?,?,?)", requestId, title, desc);
    }

    public void insertWiki(long requestId, String title, String body) {
        jdbc.update("INSERT INTO wiki_page(request_id,title,body) VALUES (?,?,?)", requestId, title, body);
    }

    public List<Map<String, Object>> tickets(long requestId) {
        return jdbc.queryForList("SELECT * FROM ticket WHERE request_id=? ORDER BY id ASC", requestId);
    }

    public List<Map<String, Object>> wiki(long requestId) {
        return jdbc.queryForList("SELECT * FROM wiki_page WHERE request_id=? ORDER BY id ASC", requestId);
    }

    public Map<String, Object> stats() {
        // 아주 단순한 통계(제출용)
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM work_request", Integer.class);
        Integer done = jdbc.queryForObject("SELECT COUNT(*) FROM work_request WHERE status='DONE'", Integer.class);
        Integer failed = jdbc.queryForObject("SELECT COUNT(*) FROM work_request WHERE status='FAILED'", Integer.class);
        Double avgLatency = jdbc.queryForObject("SELECT COALESCE(AVG(latency_ms),0) FROM audit_log", Double.class);

        return Map.of(
                "totalRequests", total == null ? 0 : total,
                "done", done == null ? 0 : done,
                "failed", failed == null ? 0 : failed,
                "avgAuditLatencyMs", avgLatency == null ? 0 : Math.round(avgLatency)
        );
    }
}
