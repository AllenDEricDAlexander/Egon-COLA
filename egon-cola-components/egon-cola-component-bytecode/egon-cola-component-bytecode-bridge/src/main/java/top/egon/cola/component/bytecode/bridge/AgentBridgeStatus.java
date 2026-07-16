package top.egon.cola.component.bytecode.bridge;

import java.util.List;
import java.util.Set;

public record AgentBridgeStatus(
        String agentVersion,
        String state,
        int protocolMajor,
        int protocolMinor,
        Set<BridgeCapability> requestedFeatures,
        Set<BridgeCapability> effectiveFeatures,
        long transformedClassCount,
        long skippedClassCount,
        long failureCount,
        List<AgentBridgeFailure> recentFailures
) {
    public AgentBridgeStatus {
        agentVersion = agentVersion == null ? "" : agentVersion;
        state = state == null ? "DISABLED" : state;
        requestedFeatures = Set.copyOf(requestedFeatures);
        effectiveFeatures = Set.copyOf(effectiveFeatures);
        recentFailures = List.copyOf(recentFailures);
    }

    public static AgentBridgeStatus disabled() {
        return new AgentBridgeStatus(
                "",
                "DISABLED",
                BridgeProtocol.MAJOR,
                BridgeProtocol.MINOR,
                Set.of(),
                Set.of(),
                0,
                0,
                0,
                List.of()
        );
    }
}
