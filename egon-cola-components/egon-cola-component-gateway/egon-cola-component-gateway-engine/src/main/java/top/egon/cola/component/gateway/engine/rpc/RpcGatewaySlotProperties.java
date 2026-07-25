package top.egon.cola.component.gateway.engine.rpc;

public record RpcGatewaySlotProperties(
        boolean enabled,
        String env,
        String namespace,
        String instanceId,
        String advertisedHost,
        String serviceName,
        String group,
        String version,
        String gatewayGroupCode,
        String gatewayVersion,
        String rpcRuntimeVersion,
        int leaseSeconds,
        int heartbeatIntervalSeconds
) {

    public RpcGatewaySlotProperties {
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        instanceId = required(instanceId, "instanceId");
        advertisedHost = required(advertisedHost, "advertisedHost");
        serviceName = required(serviceName, "serviceName");
        group = required(group, "group");
        version = required(version, "version");
        gatewayGroupCode = required(gatewayGroupCode, "gatewayGroupCode");
        gatewayVersion = required(gatewayVersion, "gatewayVersion");
        rpcRuntimeVersion = required(rpcRuntimeVersion, "rpcRuntimeVersion");
        if (leaseSeconds <= 0
                || heartbeatIntervalSeconds <= 0
                || heartbeatIntervalSeconds >= leaseSeconds) {
            throw new IllegalArgumentException(
                    "heartbeat interval must be less than lease"
            );
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
