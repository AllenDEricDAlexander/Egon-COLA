package top.egon.cola.component.agentflow.autoconfigure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.egon.cola.component.agentflow.config.AgentFlowConfigDTO;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable, strictly bound configuration for the Agent Flow starter. */
@ConfigurationProperties(prefix = "egon.cola.component.agent-flow", ignoreUnknownFields = false)
public record AgentFlowProperties(
        boolean enabled,
        @NotNull Duration executionTimeout,
        @NotNull Duration shutdownTimeout,
        @Valid Map<String, @Valid AgentFlowConfigDTO> flows) {

    public AgentFlowProperties {
        executionTimeout = executionTimeout == null ? Duration.ofMinutes(2) : executionTimeout;
        shutdownTimeout = shutdownTimeout == null ? Duration.ofSeconds(10) : shutdownTimeout;
        flows = normalizeFlows(flows);
    }

    private static Map<String, AgentFlowConfigDTO> normalizeFlows(Map<String, AgentFlowConfigDTO> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, AgentFlowConfigDTO> normalized = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = key == null ? null : key.trim();
            if (normalized.containsKey(normalizedKey)) {
                throw new IllegalArgumentException("duplicate flow id: " + normalizedKey);
            }
            normalized.put(normalizedKey, value);
        });
        return Map.copyOf(normalized);
    }
}
