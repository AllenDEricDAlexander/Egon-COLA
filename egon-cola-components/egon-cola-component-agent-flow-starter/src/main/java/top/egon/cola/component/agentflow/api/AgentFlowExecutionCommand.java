package top.egon.cola.component.agentflow.api;

import com.google.genai.types.Content;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Immutable command carrying one host-owned message to an existing flow session. */
public record AgentFlowExecutionCommand(
        @NotBlank @Size(min = 1, max = 128) String flowId,
        @NotBlank @Size(min = 1, max = 256) String userId,
        @NotBlank @Size(min = 1, max = 256) String sessionId,
        @NotNull Content content) {

    public AgentFlowExecutionCommand {
        flowId = normalize(flowId);
        userId = normalize(userId);
        sessionId = normalize(sessionId);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
