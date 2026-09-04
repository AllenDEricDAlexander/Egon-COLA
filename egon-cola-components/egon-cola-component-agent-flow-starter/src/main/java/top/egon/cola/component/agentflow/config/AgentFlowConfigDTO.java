package top.egon.cola.component.agentflow.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Immutable configuration boundary for one named Agent Flow. */
public record AgentFlowConfigDTO(
        @NotBlank @Size(min = 1, max = 128) String chatModelBeanName,
        @NotBlank @Size(min = 1, max = 256) String modelName,
        @NotBlank @Size(min = 1, max = 128)
        @Pattern(regexp = "^_?[a-zA-Z0-9]*([. _-][a-zA-Z0-9]+)*$") String rootAgentName,
        @NotEmpty @Size(max = 128) List<@NotNull @Valid AgentConfigDTO> agents,
        @Size(max = 128) List<@NotNull @Valid AgentWorkflowConfigDTO> workflows) {

    public AgentFlowConfigDTO {
        chatModelBeanName = required(chatModelBeanName, "chatModelBeanName");
        modelName = required(modelName, "modelName");
        rootAgentName = required(rootAgentName, "rootAgentName");
        agents = agents == null ? List.of() : List.copyOf(agents);
        workflows = workflows == null ? List.of() : List.copyOf(workflows);
    }

    private static String required(String value, String field) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
