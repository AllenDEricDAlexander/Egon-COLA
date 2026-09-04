package top.egon.cola.component.agentflow.api;

/** Safe read-only description of a compiled Agent Flow. */
public record AgentFlowDescriptorDTO(
        String flowId,
        String rootAgentName,
        String chatModelBeanName,
        String modelName) {

    public AgentFlowDescriptorDTO {
        requireText(flowId, "flowId");
        requireText(rootAgentName, "rootAgentName");
        requireText(chatModelBeanName, "chatModelBeanName");
        requireText(modelName, "modelName");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
