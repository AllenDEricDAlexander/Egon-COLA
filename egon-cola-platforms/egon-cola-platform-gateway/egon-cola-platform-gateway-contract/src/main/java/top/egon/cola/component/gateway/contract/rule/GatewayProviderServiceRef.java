package top.egon.cola.component.gateway.contract.rule;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;

import java.util.Objects;

public record GatewayProviderServiceRef(
        String env,
        String namespace,
        GatewayProtocol protocol,
        String serviceName,
        String group,
        String version,
        String transport
) {

    public GatewayProviderServiceRef {
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        protocol = Objects.requireNonNull(protocol, "protocol");
        serviceName = required(serviceName, "serviceName");
        group = required(group, "group");
        version = required(version, "version");
        transport = required(transport, "transport");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
