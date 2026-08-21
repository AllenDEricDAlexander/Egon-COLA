package top.egon.cola.component.rpc.consumer.channel;

/** Stable transport identity shared by all logical RPC queries. */
public record RpcChannelKey(String host, int port, boolean secure) {

    public RpcChannelKey {
        host = normalizeHost(host);
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("RPC channel port is invalid");
        }
    }

    public static RpcChannelKey from(RpcEndpoint endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("RPC endpoint is required");
        }
        return new RpcChannelKey(endpoint.host(), endpoint.port(), endpoint.secure());
    }

    private static String normalizeHost(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RPC channel host is required");
        }
        String normalized = value.trim();
        if ("0.0.0.0".equals(normalized)
                || "::".equals(normalized)
                || "[::]".equals(normalized)) {
            throw new IllegalArgumentException("RPC channel host must be routable");
        }
        return normalized;
    }
}
