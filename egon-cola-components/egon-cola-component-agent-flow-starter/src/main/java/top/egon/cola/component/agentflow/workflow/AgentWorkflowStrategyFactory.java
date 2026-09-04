package top.egon.cola.component.agentflow.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.agentflow.config.AgentWorkflowConfigDTO;
import top.egon.cola.component.agentflow.config.AgentWorkflowTypeEnum;
import top.egon.cola.component.agentflow.exception.AgentFlowConfigurationException;

import java.util.List;
import java.util.Objects;

/** Selects the single strategy registered for a validated workflow type. */
@RequiredArgsConstructor
@Slf4j
public class AgentWorkflowStrategyFactory {

    @Qualifier("agentWorkflowBuilderStrategies")
    private final List<AgentWorkflowBuilderStrategy> strategies;

    public AgentWorkflowBuilderStrategy getStrategy(AgentWorkflowTypeEnum type) {
        Objects.requireNonNull(type, "workflow type must not be null");
        log.debug("Selecting Agent Flow workflow strategy for type={}", type);
        AgentWorkflowBuilderStrategy match = null;
        for (AgentWorkflowBuilderStrategy strategy : strategies) {
            if (strategy == null || !strategy.supports(new AgentWorkflowConfigDTO(type, "strategy", null, List.of("child"),
                    type == AgentWorkflowTypeEnum.LOOP ? 1 : null))) {
                continue;
            }
            if (match != null) {
                throw new AgentFlowConfigurationException("properties", type.name(), "duplicate workflow strategy");
            }
            match = strategy;
        }
        if (match == null) {
            throw new AgentFlowConfigurationException("properties", type.name(), "unsupported workflow type");
        }
        return match;
    }
}
