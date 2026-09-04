package top.egon.cola.component.agentflow.workflow;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ParallelAgent;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.agentflow.config.AgentWorkflowConfigDTO;
import top.egon.cola.component.agentflow.config.AgentWorkflowTypeEnum;

import java.util.List;

/** Builds parallel ADK workflows while preserving configured child order. */
@Slf4j
public class ParallelAgentWorkflowBuilderStrategy implements AgentWorkflowBuilderStrategy {

    @Override
    public boolean supports(AgentWorkflowConfigDTO workflow) {
        return workflow != null && workflow.type() == AgentWorkflowTypeEnum.PARALLEL;
    }

    @Override
    public BaseAgent build(AgentWorkflowConfigDTO workflow, List<? extends BaseAgent> children) {
        return ParallelAgent.builder()
                .name(workflow.name())
                .description(workflow.description())
                .subAgents(children)
                .build();
    }
}
