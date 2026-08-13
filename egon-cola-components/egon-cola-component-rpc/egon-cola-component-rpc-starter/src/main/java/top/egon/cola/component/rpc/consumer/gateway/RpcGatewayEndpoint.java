package top.egon.cola.component.rpc.consumer.gateway;

import java.time.Instant;

public record RpcGatewayEndpoint(
        String instanceId,
        String leaseId,
        String host,
        int port,
        boolean secure,
        Instant leaseExpireAt
) {

    public RpcGatewayEndpoint {
        String normalizedInstanceId = normalize(instanceId);
        String normalizedLeaseId = normalize(leaseId);
        String normalizedHost = normalize(host);
        if (normalizedInstanceId == null
                || normalizedLeaseId == null) {
            throw new IllegalArgumentException(
                    "RPC Gateway lease identity is required"
            );
        }
        if (normalizedHost == null
                || "0.0.0.0".equals(normalizedHost)
                || "::".equals(normalizedHost)
                || "[::]".equals(normalizedHost)) {
            throw new IllegalArgumentException(
                    "RPC Gateway host must be routable"
            );
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException(
                    "RPC Gateway port is invalid"
            );
        }
        if (leaseExpireAt == null) {
            throw new IllegalArgumentException(
                    "RPC Gateway lease expiry is required"
            );
        }
        instanceId = normalizedInstanceId;
        leaseId = normalizedLeaseId;
        host = normalizedHost;
    }

    public boolean activeAt(Instant now) {
        return leaseExpireAt != null
                && leaseExpireAt.isAfter(now);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
