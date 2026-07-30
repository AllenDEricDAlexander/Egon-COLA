package top.egon.cola.component.gateway.engine.http;

import top.egon.cola.component.gateway.engine.security.GatewayTransportSecurity;

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
        int upstreamPendingAcquireMaxCount,
        long absoluteMaxRequestBodyBytes,
        int bodyLogSampleBytes,
        int absoluteMaxBodyLogSampleBytes,
        Duration maxConnectTimeout,
        Duration maxResponseHeaderTimeout,
        Duration maxStreamIdleTimeout,
        Duration maxTotalTimeout,
        Duration maxWebsocketIdleTimeout,
        long maxWebsocketFrameBytes
) {

    private static final long MIB = 1024L * 1024L;

    private static final long LEGACY_AGGREGATED_MAX_BODY_BYTES = 64L * MIB;

    public GatewayHttpEngineProperties(
            Listener publicListener,
            Listener internalListener,
            int maxHeaderCount,
            int maxHeaderBytes,
            long defaultMaxBodyBytes,
            Duration connectionIdleTimeout,
            Duration drainTimeout,
            int upstreamMaxConnections,
            int upstreamPendingAcquireMaxCount) {
        this(
                publicListener,
                internalListener,
                maxHeaderCount,
                maxHeaderBytes,
                defaultMaxBodyBytes,
                connectionIdleTimeout,
                drainTimeout,
                upstreamMaxConnections,
                upstreamPendingAcquireMaxCount,
                1024L * MIB,
                8 * 1024,
                64 * 1024,
                Duration.ofSeconds(60),
                Duration.ofMinutes(10),
                Duration.ofMinutes(30),
                Duration.ofHours(2),
                Duration.ofHours(2),
                64L * MIB
        );
    }

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
        if (internalListener.enabled()
                && internalListener.transportSecurity().enabled()
                && !internalListener.transportSecurity()
                .clientCertificateRequired()) {
            throw new IllegalArgumentException(
                    "INTERNAL HTTP TLS must require a client certificate"
            );
        }
        if (maxHeaderCount < 1 || maxHeaderBytes < 256) {
            throw new IllegalArgumentException("invalid HTTP header limits");
        }
        if (absoluteMaxRequestBodyBytes < 1
                || absoluteMaxRequestBodyBytes > 1024L * MIB) {
            throw new IllegalArgumentException(
                    "absoluteMaxRequestBodyBytes must be between 1 byte and "
                            + "1 GiB"
            );
        }
        long aggregatedMaximum = Math.min(
                LEGACY_AGGREGATED_MAX_BODY_BYTES,
                absoluteMaxRequestBodyBytes
        );
        if (defaultMaxBodyBytes < 1
                || defaultMaxBodyBytes > aggregatedMaximum) {
            throw new IllegalArgumentException(
                    "defaultMaxBodyBytes must be between 1 byte and "
                            + "the lower of 64 MiB and "
                            + "absoluteMaxRequestBodyBytes"
            );
        }
        if (absoluteMaxBodyLogSampleBytes < 1
                || absoluteMaxBodyLogSampleBytes > 64 * 1024
                || bodyLogSampleBytes < 1
                || bodyLogSampleBytes > absoluteMaxBodyLogSampleBytes) {
            throw new IllegalArgumentException(
                    "invalid HTTP body log sample limits"
            );
        }
        connectionIdleTimeout = positive(
                connectionIdleTimeout,
                "connectionIdleTimeout"
        );
        drainTimeout = positive(drainTimeout, "drainTimeout");
        maxConnectTimeout = range(
                maxConnectTimeout,
                Duration.ofMillis(100),
                Duration.ofSeconds(60),
                "maxConnectTimeout"
        );
        maxResponseHeaderTimeout = range(
                maxResponseHeaderTimeout,
                Duration.ofSeconds(1),
                Duration.ofMinutes(10),
                "maxResponseHeaderTimeout"
        );
        maxStreamIdleTimeout = range(
                maxStreamIdleTimeout,
                Duration.ofSeconds(1),
                Duration.ofMinutes(30),
                "maxStreamIdleTimeout"
        );
        maxTotalTimeout = range(
                maxTotalTimeout,
                Duration.ofSeconds(1),
                Duration.ofHours(2),
                "maxTotalTimeout"
        );
        maxWebsocketIdleTimeout = range(
                maxWebsocketIdleTimeout,
                Duration.ofSeconds(1),
                Duration.ofHours(2),
                "maxWebsocketIdleTimeout"
        );
        if (maxWebsocketFrameBytes < 1024L
                || maxWebsocketFrameBytes > 64L * MIB) {
            throw new IllegalArgumentException(
                    "maxWebsocketFrameBytes must be between 1 KiB and 64 MiB"
            );
        }
        if (upstreamMaxConnections < 1
                || upstreamPendingAcquireMaxCount < 0) {
            throw new IllegalArgumentException(
                    "invalid upstream connection pool limits"
            );
        }
    }

    public record Listener(
            boolean enabled,
            String host,
            int port,
            GatewayTransportSecurity transportSecurity
    ) {

        public Listener(boolean enabled, String host, int port) {
            this(
                    enabled,
                    host,
                    port,
                    GatewayTransportSecurity.developmentPlaintextConfig()
            );
        }

        public Listener {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("listener host is required");
            }
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException(
                        "listener port must be between 0 and 65535"
                );
            }
            transportSecurity = Objects.requireNonNull(
                    transportSecurity,
                    "transportSecurity"
            );
        }
    }

    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static Duration range(
            Duration value,
            Duration minimum,
            Duration maximum,
            String field) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(
                    field + " must be between " + minimum + " and " + maximum
            );
        }
        return value;
    }
}
