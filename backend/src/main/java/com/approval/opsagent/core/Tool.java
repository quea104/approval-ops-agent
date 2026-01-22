package com.approval.opsagent.core;

import java.util.Map;

public interface Tool {
    String name();
    Object run(long requestId, Map<String, Object> args);
}
