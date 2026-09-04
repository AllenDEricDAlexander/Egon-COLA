package top.egon.cola.component.agentflow.exception;

/** Raised when a flow execution exceeds its configured total deadline. */
public class AgentFlowExecutionTimeoutException extends AgentFlowException {

    public AgentFlowExecutionTimeoutException(String flowId, Throwable cause) {
        super("Agent Flow execution timed out for flow: " + flowId, cause);
    }
}
