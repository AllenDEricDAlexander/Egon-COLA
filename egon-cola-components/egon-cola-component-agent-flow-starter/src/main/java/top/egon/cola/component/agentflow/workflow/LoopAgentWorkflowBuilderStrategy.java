package top.egon.cola.component.agentflow.workflow;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LoopAgent;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.agentflow.config.AgentWorkflowConfigDTO;
import top.egon.cola.component.agentflow.config.AgentWorkflowTypeEnum;

import java.util.List;

/** Builds bounded loop ADK workflows with the validated iteration limit. */
@Slf4j
public class LoopAgentWorkflowBuilderStrategy implements AgentWorkflowBuilderStrategy {

    @Override
    public boolean supports(AgentWorkflowConfigDTO workflow) {
        return workflow != null && workflow.type() == AgentWorkflowTypeEnum.LOOP;
    }

    @Override
    public BaseAgent build(AgentWorkflowConfigDTO workflow, List<? extends BaseAgent> children) {
        return LoopAgent.builder()
                .name(workflow.name())
                .description(workflow.description())
                .subAgents(children)
                .maxIterations(workflow.maxIterations())
                .build();
    }
}
