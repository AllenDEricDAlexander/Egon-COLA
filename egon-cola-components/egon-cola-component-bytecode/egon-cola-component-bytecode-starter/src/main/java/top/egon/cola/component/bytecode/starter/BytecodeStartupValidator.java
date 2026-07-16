package top.egon.cola.component.bytecode.starter;

import top.egon.cola.component.bytecode.bridge.AgentBridgeStatus;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;

public final class BytecodeStartupValidator {

    private final Validation validation;
    private final AgentBridgeStatus agentStatus;

    public BytecodeStartupValidator() {
        AgentBridgeStatus status = DispatcherRegistry.agentStatus();
        this.agentStatus = status;
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
        if ("FAILED".equals(status.state())) {
            throw new IllegalStateException(
                    "Bytecode Agent entered fatal state before Spring startup");
        }
    }

    public Validation validation() {
        return validation;
    }

    public void requireAgentCapability(BridgeCapability capability) {
        if (!validation.agentAvailable()) {
            throw new IllegalStateException(
                    "Bytecode Agent is required for " + capability.name());
        }
        if (!"ACTIVE".equals(agentStatus.state())
                && !"DEGRADED".equals(agentStatus.state())) {
            throw new IllegalStateException(
                    "Bytecode Agent is not active: " + agentStatus.state());
        }
        if (!agentStatus.effectiveFeatures().contains(capability)) {
            throw new IllegalStateException(
                    "Bytecode Agent capability is not effective: " + capability.name());
        }
    }

    public record Validation(boolean agentAvailable, boolean protocolCompatible, String state) {
    }
}
