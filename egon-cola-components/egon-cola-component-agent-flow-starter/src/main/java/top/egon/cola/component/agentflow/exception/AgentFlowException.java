package top.egon.cola.component.agentflow.exception;

/** Base unchecked exception for the Agent Flow component. */
public class AgentFlowException extends RuntimeException {

    public AgentFlowException(String message) {
        super(message);
    }

    public AgentFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
