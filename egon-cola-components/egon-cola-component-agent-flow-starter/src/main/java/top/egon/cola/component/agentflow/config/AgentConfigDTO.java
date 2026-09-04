package top.egon.cola.component.agentflow.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Immutable leaf Agent configuration consumed by the ADK builder. */
public record AgentConfigDTO(
        @NotBlank @Size(min = 1, max = 128)
        @Pattern(regexp = "^_?[a-zA-Z0-9]*([. _-][a-zA-Z0-9]+)*$") String name,
        @Size(max = 1024) String description,
        @NotBlank @Size(min = 1, max = 32_000) String instruction,
        @Size(min = 1, max = 128) String outputKey) {

    public AgentConfigDTO {
        name = normalize(name);
        description = normalizeOptional(description);
        outputKey = normalizeOptional(outputKey);
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
