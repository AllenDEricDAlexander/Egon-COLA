package top.egon.cola.component.agentflow.runtime;

import com.google.adk.agents.BaseAgent;
import com.google.adk.runner.InMemoryRunner;
import top.egon.cola.component.agentflow.api.AgentFlowDescriptorDTO;

import java.time.Instant;

/** Internal immutable owner of one compiled ADK runner and its safe diagnostics. */
public record AgentFlowRuntimeBO(
        String flowId,
        String rootAgentName,
        String chatModelBeanName,
        String modelName,
        BaseAgent rootAgent,
        InMemoryRunner runner,
        Instant createdAt) {

    public AgentFlowRuntimeBO {
        requireText(flowId, "flowId");
        requireText(rootAgentName, "rootAgentName");
        requireText(chatModelBeanName, "chatModelBeanName");
        requireText(modelName, "modelName");
        if (rootAgent == null || runner == null || createdAt == null) {
            throw new IllegalArgumentException("runtime values must be present");
        }
    }

    public AgentFlowDescriptorDTO descriptor() {
        return new AgentFlowDescriptorDTO(flowId, rootAgentName, chatModelBeanName, modelName);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
