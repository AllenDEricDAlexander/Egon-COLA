package top.egon.cola.component.rpc.provider;

import top.egon.cola.component.rpc.context.RpcProcessIdentity;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * 单个 RPC Provider 服务端点的注册中心无关描述。
 *
 * <p>Registry-neutral description of one RPC Provider service endpoint.
 */
public record RpcProviderRegistration(
        RpcServiceIdentity serviceIdentity,
        RpcProcessIdentity processIdentity,
        String host,
        int port,
        boolean secure,
        Map<String, String> metadata,
        int leaseSeconds,
        int heartbeatIntervalSeconds
) {

    public RpcProviderRegistration {
        if (serviceIdentity == null || processIdentity == null) {
            throw new IllegalArgumentException(
                    "RPC Provider identity is required"
            );
        }
        if (host == null || host.isBlank()
                || "0.0.0.0".equals(host.trim())
                || "::".equals(host.trim())
                || "[::]".equals(host.trim())) {
            throw new IllegalArgumentException(
                    "RPC Provider host must be routable"
            );
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException(
                    "RPC Provider port is invalid"
            );
        }
        if (leaseSeconds <= 0
                || heartbeatIntervalSeconds <= 0
                || heartbeatIntervalSeconds >= leaseSeconds) {
            throw new IllegalArgumentException(
                    "RPC Provider lease timing is invalid"
            );
        }
        host = host.trim();
        metadata = metadata == null
                ? Map.of()
                : Collections.unmodifiableMap(new TreeMap<>(metadata));
    }
}
