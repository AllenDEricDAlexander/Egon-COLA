package top.egon.cola.component.agentflow.exception;

/** Raised when a requested flow is not present in the compiled registry. */
public class AgentFlowNotFoundException extends AgentFlowException {

    public AgentFlowNotFoundException(String flowId) {
        super("Agent Flow not found: " + flowId);
    }
}
