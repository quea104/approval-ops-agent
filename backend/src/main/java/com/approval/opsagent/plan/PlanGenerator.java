package com.approval.opsagent.plan;

public interface PlanGenerator {
    String makePlanJson(String title, String inputText);
}
