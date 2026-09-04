package top.egon.cola.component.agentflow.exception;

/** Safe configuration failure that identifies only the offending flow/node boundary. */
public class AgentFlowConfigurationException extends AgentFlowException {

    public AgentFlowConfigurationException(String flowId, String nodeName, String reason) {
        super("Invalid Agent Flow configuration for flow '" + flowId + "' at '" + nodeName + "': " + reason);
    }

    public AgentFlowConfigurationException(String flowId, String nodeName, String reason, Throwable cause) {
        super("Invalid Agent Flow configuration for flow '" + flowId + "' at '" + nodeName + "': " + reason, cause);
    }
}
