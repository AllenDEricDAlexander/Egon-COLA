package top.egon.cola.component.agentflow.exception;

/** Raised when a flow session is absent at an operation boundary. */
public class AgentFlowSessionNotFoundException extends AgentFlowException {

    public AgentFlowSessionNotFoundException(String flowId) {
        super("Agent Flow session not found for flow: " + flowId);
    }
}
