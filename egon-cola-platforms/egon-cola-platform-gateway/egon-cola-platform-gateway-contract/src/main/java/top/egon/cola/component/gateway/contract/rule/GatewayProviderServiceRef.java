package top.egon.cola.component.gateway.contract.rule;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;

import java.util.Objects;

public record GatewayProviderServiceRef(
        String bizCode,
        String appCode,
        String env,
        String namespace,
        GatewayProtocol protocol,
        String serviceName,
        String group,
        String version,
        String transport
) {

    public GatewayProviderServiceRef {
        // Null is retained only for reading pre biz/app rule snapshots. New
        // releases always supply both fields and Engine compilation rejects a
        // legacy snapshot until it is republished with an explicit DDC scope.
        bizCode = optional(bizCode);
        appCode = optional(appCode);
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

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
