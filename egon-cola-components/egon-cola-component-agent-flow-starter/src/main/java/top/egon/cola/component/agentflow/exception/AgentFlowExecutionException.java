package top.egon.cola.component.agentflow.exception;

/** Raised when an ADK execution fails after the session boundary has been accepted. */
public class AgentFlowExecutionException extends AgentFlowException {

    public AgentFlowExecutionException(String flowId, Throwable cause) {
        super("Agent Flow execution failed for flow: " + flowId, cause);
    }
}
