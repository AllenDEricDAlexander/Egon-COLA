package top.egon.cola.component.bytecode.bridge;

import java.util.Set;

public record BridgeStatus(
        boolean registered,
        int protocolMajor,
        int protocolMinor,
        String runtimeVersion,
        Set<BridgeCapability> capabilities,
        int callSiteCount,
        int methodCount
) {
    public BridgeStatus {
        runtimeVersion = runtimeVersion == null ? "" : runtimeVersion;
        capabilities = Set.copyOf(capabilities);
    }

    public static BridgeStatus unregistered(int callSiteCount, int methodCount) {
        return new BridgeStatus(false, BridgeProtocol.MAJOR, BridgeProtocol.MINOR,
                "", Set.of(), callSiteCount, methodCount);
    }
}
