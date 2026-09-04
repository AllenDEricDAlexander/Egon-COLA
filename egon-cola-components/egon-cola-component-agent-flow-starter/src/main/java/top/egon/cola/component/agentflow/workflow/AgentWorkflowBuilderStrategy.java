package top.egon.cola.component.agentflow.workflow;

import com.google.adk.agents.BaseAgent;
import top.egon.cola.component.agentflow.config.AgentWorkflowConfigDTO;

import java.util.List;

/** Builds one supported Google ADK composite workflow type. */
public interface AgentWorkflowBuilderStrategy {

    boolean supports(AgentWorkflowConfigDTO workflow);

    BaseAgent build(AgentWorkflowConfigDTO workflow, List<? extends BaseAgent> children);
}
