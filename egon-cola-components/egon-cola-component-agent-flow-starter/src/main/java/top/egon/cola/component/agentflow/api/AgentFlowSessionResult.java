package top.egon.cola.component.agentflow.api;

import java.time.Instant;

/** Immutable result for a newly created in-memory flow session. */
public record AgentFlowSessionResult(String flowId, String sessionId, Instant createdAt) {

    public AgentFlowSessionResult {
        if (flowId == null || flowId.isBlank() || sessionId == null || sessionId.isBlank() || createdAt == null) {
            throw new IllegalArgumentException("session result values must be present");
        }
    }
}
