package top.egon.cola.component.gateway.engine.http;

import java.time.Duration;
import java.util.Objects;

public record GatewayHttpEngineProperties(
        Listener publicListener,
        Listener internalListener,
        int maxHeaderCount,
        int maxHeaderBytes,
        long defaultMaxBodyBytes,
        Duration connectionIdleTimeout,
        Duration drainTimeout,
        int upstreamMaxConnections,
        int upstreamPendingAcquireMaxCount
) {

    public GatewayHttpEngineProperties {
        publicListener = Objects.requireNonNull(publicListener, "publicListener");
        internalListener = Objects.requireNonNull(
                internalListener,
                "internalListener"
        );
        if (publicListener.enabled()
                && internalListener.enabled()
                && publicListener.port() == internalListener.port()
                && publicListener.port() != 0) {
            throw new IllegalArgumentException(
                    "PUBLIC and INTERNAL HTTP ports must be different"
            );
        }
        if (!publicListener.enabled() && !internalListener.enabled()) {
            throw new IllegalArgumentException(
                    "at least one HTTP listener must be enabled"
            );
        }
        if (maxHeaderCount < 1 || maxHeaderBytes < 256) {
            throw new IllegalArgumentException("invalid HTTP header limits");
        }
        if (defaultMaxBodyBytes < 1
                || defaultMaxBodyBytes > 64L * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "defaultMaxBodyBytes must be between 1 byte and 64 MiB"
            );
        }
        connectionIdleTimeout = positive(
                connectionIdleTimeout,
                "connectionIdleTimeout"
        );
        drainTimeout = positive(drainTimeout, "drainTimeout");
        if (upstreamMaxConnections < 1
                || upstreamPendingAcquireMaxCount < 0) {
            throw new IllegalArgumentException(
                    "invalid upstream connection pool limits"
            );
        }
    }

    public record Listener(boolean enabled, String host, int port) {

        public Listener {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("listener host is required");
            }
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException(
                        "listener port must be between 0 and 65535"
                );
            }
        }
    }

    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
