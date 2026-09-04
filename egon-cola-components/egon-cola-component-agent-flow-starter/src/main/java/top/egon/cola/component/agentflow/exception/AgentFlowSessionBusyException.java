package top.egon.cola.component.agentflow.exception;

/** Raised when a flow session already owns an active execution lease. */
public class AgentFlowSessionBusyException extends AgentFlowException {

    public AgentFlowSessionBusyException(String flowId) {
        super("Agent Flow session is busy for flow: " + flowId);
    }
}
