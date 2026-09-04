package top.egon.cola.component.agentflow.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import top.egon.cola.component.agentflow.config.AgentFlowSessionValidationGroup;

/** Reusable command whose session identifier rules differ between create and delete. */
public record AgentFlowSessionCommand(
        @NotBlank(groups = {Default.class, AgentFlowSessionValidationGroup.Create.class,
                AgentFlowSessionValidationGroup.Delete.class})
        @Size(min = 1, max = 128, groups = {Default.class, AgentFlowSessionValidationGroup.Create.class,
                AgentFlowSessionValidationGroup.Delete.class})
        String flowId,
        @NotBlank(groups = {Default.class, AgentFlowSessionValidationGroup.Create.class,
                AgentFlowSessionValidationGroup.Delete.class})
        @Size(min = 1, max = 256, groups = {Default.class, AgentFlowSessionValidationGroup.Create.class,
                AgentFlowSessionValidationGroup.Delete.class})
        String userId,
        @Null(groups = AgentFlowSessionValidationGroup.Create.class)
        @NotBlank(groups = AgentFlowSessionValidationGroup.Delete.class)
        @Size(max = 256, groups = {Default.class, AgentFlowSessionValidationGroup.Create.class,
                AgentFlowSessionValidationGroup.Delete.class})
        String sessionId) {

    public AgentFlowSessionCommand {
        flowId = normalize(flowId);
        userId = normalize(userId);
        sessionId = normalizeOptional(sessionId);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
