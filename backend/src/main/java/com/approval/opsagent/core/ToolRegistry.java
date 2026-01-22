package com.approval.opsagent.core;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {
    private final Map<String, Tool> tools = new HashMap<>();

    public ToolRegistry(List<Tool> toolList) {
        for (Tool t : toolList) tools.put(t.name(), t);
    }

    public Tool get(String name) {
        Tool t = tools.get(name);
        if (t == null) throw new IllegalArgumentException("unknown tool: " + name);
        return t;
    }
}
