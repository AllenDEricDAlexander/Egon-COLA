package top.egon.cola.component.agentflow.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Immutable composite workflow configuration consumed by a workflow strategy. */
public record AgentWorkflowConfigDTO(
        @NotNull AgentWorkflowTypeEnum type,
        @NotNull @Size(min = 1, max = 128)
        @Pattern(regexp = "^_?[a-zA-Z0-9]*([. _-][a-zA-Z0-9]+)*$") String name,
        @Size(max = 1024) String description,
        @NotEmpty @Size(max = 128) List<@NotNull @Size(min = 1, max = 128) String> subAgentNames,
        @Min(1) @Max(100) Integer maxIterations) {

    public AgentWorkflowConfigDTO {
        name = normalize(name);
        description = normalizeOptional(description);
        subAgentNames = subAgentNames == null
                ? List.of()
                : subAgentNames.stream().map(AgentWorkflowConfigDTO::normalize).toList();
        if (maxIterations == null && type == AgentWorkflowTypeEnum.LOOP) {
            maxIterations = 3;
        }
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
