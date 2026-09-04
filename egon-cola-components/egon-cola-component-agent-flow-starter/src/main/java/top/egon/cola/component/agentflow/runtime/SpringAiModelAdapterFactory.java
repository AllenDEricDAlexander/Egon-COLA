package top.egon.cola.component.agentflow.runtime;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.springai.SpringAI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import top.egon.cola.component.agentflow.exception.AgentFlowConfigurationException;

/** Creates the official Google ADK Spring AI adapter for a host-provided model. */
@Slf4j
public class SpringAiModelAdapterFactory {

    public BaseLlm create(ChatModel chatModel, String modelName) {
        if (chatModel == null) {
            throw new AgentFlowConfigurationException("properties", "chatModel", "ChatModel bean is required");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new AgentFlowConfigurationException("properties", "modelName", "model name is required");
        }
        try {
            return new SpringAI(chatModel, modelName.trim());
        } catch (RuntimeException failure) {
            throw new AgentFlowConfigurationException("properties", "modelName", "Spring AI adapter creation failed", failure);
        }
    }
}
