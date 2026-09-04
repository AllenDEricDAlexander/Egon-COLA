package top.egon.cola.component.agentflow.runtime;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.runner.InMemoryRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.agentflow.config.AgentConfigDTO;
import top.egon.cola.component.agentflow.config.AgentFlowConfigDTO;
import top.egon.cola.component.agentflow.config.AgentWorkflowConfigDTO;
import top.egon.cola.component.agentflow.exception.AgentFlowConfigurationException;
import top.egon.cola.component.agentflow.workflow.AgentWorkflowStrategyFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compiles one validated flow configuration into an ADK root and in-memory runner. */
@RequiredArgsConstructor
@Slf4j
public class AgentFlowFactory {

    @Qualifier("springAiModelAdapterFactory")
    private final SpringAiModelAdapterFactory modelAdapterFactory;

    @Qualifier("agentWorkflowStrategyFactory")
    private final AgentWorkflowStrategyFactory strategyFactory;

    public AgentFlowCompilationBO create(String flowId, AgentFlowConfigDTO config,
                                         Map<String, ChatModel> chatModels) {
        if (flowId == null || flowId.isBlank() || config == null || chatModels == null) {
            throw new AgentFlowConfigurationException(flowId == null ? "flow" : flowId, "flow", "flow inputs are required");
        }
        InMemoryRunner runner = null;
        try {
            log.debug("Compiling Agent Flow flowId={} stage=BUILD", flowId);
            ChatModel chatModel = chatModels.get(config.chatModelBeanName());
            if (chatModel == null) {
                throw new AgentFlowConfigurationException(flowId, config.chatModelBeanName(), "ChatModel bean is missing");
            }
            BaseAgent root = buildRoot(flowId, config, modelAdapterFactory.create(chatModel, config.modelName()));
            runner = new InMemoryRunner(root, flowId);
            return new AgentFlowCompilationBO(flowId, root, runner);
        } catch (AgentFlowConfigurationException failure) {
            closeQuietly(runner, failure);
            throw failure;
        } catch (RuntimeException failure) {
            closeQuietly(runner, failure);
            throw new AgentFlowConfigurationException(flowId, "flow", "ADK flow construction failed", failure);
        }
    }

    private BaseAgent buildRoot(String flowId, AgentFlowConfigDTO config, com.google.adk.models.BaseLlm model) {
        Map<String, BaseAgent> agents = new HashMap<>();
        for (AgentConfigDTO agentConfig : config.agents()) {
            try {
                LlmAgent.Builder builder = LlmAgent.builder()
                        .name(agentConfig.name())
                        .description(agentConfig.description())
                        .model(model)
                        .instruction(agentConfig.instruction());
                if (agentConfig.outputKey() != null) {
                    builder.outputKey(agentConfig.outputKey());
                }
                BaseAgent previous = agents.putIfAbsent(agentConfig.name(), builder.build());
                if (previous != null) {
                    throw new AgentFlowConfigurationException(flowId, agentConfig.name(), "duplicate agent name");
                }
            } catch (AgentFlowConfigurationException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw new AgentFlowConfigurationException(flowId, agentConfig.name(), "leaf agent construction failed", failure);
            }
        }
        Map<String, AgentWorkflowConfigDTO> workflows = new HashMap<>();
        for (AgentWorkflowConfigDTO workflow : config.workflows()) {
            AgentWorkflowConfigDTO previous = workflows.putIfAbsent(workflow.name(), workflow);
            if (previous != null || agents.containsKey(workflow.name())) {
                throw new AgentFlowConfigurationException(flowId, workflow.name(), "duplicate workflow name");
            }
        }
        Map<String, BaseAgent> memo = new HashMap<>(agents);
        BaseAgent root = buildNode(flowId, config.rootAgentName(), workflows, memo, new HashSet<>());
        if (root == null) {
            throw new AgentFlowConfigurationException(flowId, config.rootAgentName(), "root agent could not be built");
        }
        return root;
    }

    private BaseAgent buildNode(String flowId, String name, Map<String, AgentWorkflowConfigDTO> workflows,
                                Map<String, BaseAgent> memo, Set<String> visiting) {
        BaseAgent leaf = memo.get(name);
        if (leaf != null) {
            return leaf;
        }
        AgentWorkflowConfigDTO workflow = workflows.get(name);
        if (workflow == null || !visiting.add(name)) {
            throw new AgentFlowConfigurationException(flowId, name, workflow == null ? "node is missing" : "cycle detected");
        }
        try {
            List<BaseAgent> children = workflow.subAgentNames().stream()
                    .map(child -> buildNode(flowId, child, workflows, memo, visiting))
                    .toList();
            BaseAgent built = strategyFactory.getStrategy(workflow.type()).build(workflow, children);
            memo.put(name, built);
            return built;
        } catch (AgentFlowConfigurationException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new AgentFlowConfigurationException(flowId, name, "workflow construction failed", failure);
        } finally {
            visiting.remove(name);
        }
    }

    private static void closeQuietly(InMemoryRunner runner, RuntimeException failure) {
        if (runner == null) {
            return;
        }
        try {
            runner.close().blockingAwait();
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    public record AgentFlowCompilationBO(String flowId, BaseAgent rootAgent,
                                          InMemoryRunner runner) {
        public AgentFlowCompilationBO {
            if (flowId == null || flowId.isBlank() || rootAgent == null || runner == null) {
                throw new IllegalArgumentException("flow compilation values must be present");
            }
        }
    }
}
