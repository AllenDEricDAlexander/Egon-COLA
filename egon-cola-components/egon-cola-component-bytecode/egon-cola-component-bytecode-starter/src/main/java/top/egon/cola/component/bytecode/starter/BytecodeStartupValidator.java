package top.egon.cola.component.bytecode.starter;

import top.egon.cola.component.bytecode.bridge.AgentBridgeStatus;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;

public final class BytecodeStartupValidator {

    private final Validation validation;

    public BytecodeStartupValidator() {
        AgentBridgeStatus status = DispatcherRegistry.agentStatus();
        boolean agentAvailable = !"DISABLED".equals(status.state());
        boolean compatible = !agentAvailable || status.protocolMajor() == BridgeProtocol.MAJOR;
        this.validation = new Validation(
                agentAvailable,
                compatible,
                agentAvailable ? status.state() : "AGENT_UNAVAILABLE"
        );
        if (!compatible) {
            throw new IllegalStateException("Bytecode Agent protocol mismatch: bridge="
                    + BridgeProtocol.MAJOR + ", agent=" + status.protocolMajor());
        }
    }

    public Validation validation() {
        return validation;
    }

    public record Validation(boolean agentAvailable, boolean protocolCompatible, String state) {
    }
}
