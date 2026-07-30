package top.egon.cola.component.gateway.core.transport;

import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public final class GatewayRouteProfileResolver {

    private static final long MIB = 1024L * 1024L;
    private static final Duration MIN_CONNECT_TIMEOUT =
            Duration.ofMillis(100);
    private static final Duration MIN_STREAM_TIMEOUT = Duration.ofSeconds(1);
    private static final long MIN_WEBSOCKET_FRAME_BYTES = 1024L;

    public EffectiveGatewayTransportPolicy resolve(
            GatewayRouteTransportPolicy routePolicy,
            GatewayTransportDefaults defaults,
            GatewayTransportPolicyOverrides policyOverrides,
            GatewayTransportSafetyLimits safetyLimits) {
        Objects.requireNonNull(defaults, "defaults");
        Objects.requireNonNull(policyOverrides, "policyOverrides");
        Objects.requireNonNull(safetyLimits, "safetyLimits");

        GatewayRouteProfile profile = routePolicy == null
                || routePolicy.profile() == null
                ? GatewayRouteProfile.DEFAULT
                : routePolicy.profile();
        ProfileDefaults profileDefaults = defaults(profile, defaults);
        GatewayTransportProtocol protocol = value(
                routePolicy == null ? null : routePolicy.transportProtocol(),
                profileDefaults.transportProtocol()
        );
        long maxRequestBodyBytes = requestBodyLimit(
                routePolicy,
                profileDefaults.maxRequestBodyBytes(),
                policyOverrides,
                safetyLimits
        );
        OptionalLong maxResponseBodyBytes = responseBodyLimit(
                profileDefaults.maxResponseBodyBytes(),
                policyOverrides.maxResponseBodyBytes()
        );
        Duration connectTimeout = duration(
                routePolicy == null ? null : routePolicy.connectTimeoutMs(),
                profileDefaults.connectTimeout(),
                MIN_CONNECT_TIMEOUT,
                safetyLimits.maxConnectTimeout(),
                "connectTimeoutMs"
        );
        Duration responseHeaderTimeout = streamDuration(
                routePolicy == null
                        ? null
                        : routePolicy.responseHeaderTimeoutMs(),
                profileDefaults.responseHeaderTimeout(),
                profile == GatewayRouteProfile.DEFAULT,
                safetyLimits.maxResponseHeaderTimeout(),
                "responseHeaderTimeoutMs"
        );
        Duration streamIdleTimeout = streamDuration(
                routePolicy == null ? null : routePolicy.streamIdleTimeoutMs(),
                profileDefaults.streamIdleTimeout(),
                profile == GatewayRouteProfile.DEFAULT,
                safetyLimits.maxStreamIdleTimeout(),
                "streamIdleTimeoutMs"
        );
        Optional<Duration> totalTimeout = totalTimeout(
                routePolicy,
                profileDefaults.totalTimeout(),
                policyOverrides.totalTimeout(),
                safetyLimits.maxTotalTimeout()
        );
        Optional<Duration> websocketIdleTimeout = websocketIdleTimeout(
                routePolicy,
                protocol,
                profileDefaults.websocketIdleTimeout(),
                safetyLimits.maxWebsocketIdleTimeout()
        );
        OptionalLong websocketMaxFrameBytes = websocketFrameLimit(
                routePolicy,
                protocol,
                profileDefaults.websocketMaxFrameBytes(),
                safetyLimits.maxWebsocketFrameBytes()
        );

        return new EffectiveGatewayTransportPolicy(
                profile,
                protocol,
                value(
                        routePolicy == null
                                ? null
                                : routePolicy.requestBodyMode(),
                        profileDefaults.requestBodyMode()
                ),
                value(
                        routePolicy == null ? null : routePolicy.responseMode(),
                        profileDefaults.responseMode()
                ),
                maxRequestBodyBytes,
                maxResponseBodyBytes,
                connectTimeout,
                responseHeaderTimeout,
                streamIdleTimeout,
                totalTimeout,
                websocketIdleTimeout,
                websocketMaxFrameBytes,
                value(
                        routePolicy == null
                                ? null
                                : routePolicy.bodyLogEnabled(),
                        profileDefaults.bodyLogEnabled()
                ),
                value(
                        routePolicy == null ? null : routePolicy.retryEnabled(),
                        profileDefaults.retryAllowed()
                ),
                profile == GatewayRouteProfile.OPENAI_HTTP
        );
    }

    private ProfileDefaults defaults(
            GatewayRouteProfile profile,
            GatewayTransportDefaults defaults) {
        return switch (profile) {
            case DEFAULT -> new ProfileDefaults(
                    GatewayTransportProtocol.HTTP,
                    GatewayRequestBodyMode.AGGREGATED,
                    GatewayTransportResponseMode.STANDARD,
                    defaults.maxRequestBodyBytes(),
                    defaults.maxResponseBodyBytes(),
                    defaults.connectTimeout(),
                    defaults.responseHeaderTimeout(),
                    defaults.streamIdleTimeout(),
                    defaults.totalTimeout(),
                    Optional.empty(),
                    OptionalLong.empty(),
                    defaults.bodyLogEnabled(),
                    defaults.retryAllowed()
            );
            case OPENAI_HTTP -> new ProfileDefaults(
                    GatewayTransportProtocol.HTTP,
                    GatewayRequestBodyMode.STREAMING,
                    GatewayTransportResponseMode.AUTO_STREAM,
                    512L * MIB,
                    OptionalLong.empty(),
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(120),
                    Duration.ofSeconds(90),
                    Optional.of(Duration.ofMinutes(30)),
                    Optional.of(Duration.ofMinutes(5)),
                    OptionalLong.of(16L * MIB),
                    false,
                    false
            );
        };
    }

    private long requestBodyLimit(
            GatewayRouteTransportPolicy routePolicy,
            long profileDefault,
            GatewayTransportPolicyOverrides policyOverrides,
            GatewayTransportSafetyLimits safetyLimits) {
        Long configured = routePolicy == null
                ? null
                : routePolicy.maxRequestBodyBytes();
        long routeLimit;
        if (configured == null) {
            routeLimit = Math.min(
                    profileDefault,
                    safetyLimits.maxRequestBodyBytes()
            );
        } else {
            routeLimit = range(
                    configured,
                    1L,
                    safetyLimits.maxRequestBodyBytes(),
                    "maxRequestBodyBytes"
            );
        }
        if (policyOverrides.maxRequestBodyBytes().isPresent()) {
            routeLimit = Math.min(
                    routeLimit,
                    policyOverrides.maxRequestBodyBytes().getAsLong()
            );
        }
        return Math.min(routeLimit, safetyLimits.maxRequestBodyBytes());
    }

    private OptionalLong responseBodyLimit(
            OptionalLong profileDefault,
            OptionalLong policyOverride) {
        if (profileDefault.isEmpty()) {
            return policyOverride;
        }
        if (policyOverride.isEmpty()) {
            return profileDefault;
        }
        return OptionalLong.of(Math.min(
                profileDefault.getAsLong(),
                policyOverride.getAsLong()
        ));
    }

    private Optional<Duration> totalTimeout(
            GatewayRouteTransportPolicy routePolicy,
            Optional<Duration> profileDefault,
            Optional<Duration> policyOverride,
            Duration safetyMaximum) {
        Long configured = routePolicy == null
                ? null
                : routePolicy.totalTimeoutMs();
        if (configured != null) {
            return Optional.of(duration(
                    configured,
                    MIN_STREAM_TIMEOUT,
                    safetyMaximum,
                    "totalTimeoutMs"
            ));
        }
        Optional<Duration> selected = policyOverride.isPresent()
                ? policyOverride
                : profileDefault;
        return selected.map(timeout -> duration(
                timeout,
                MIN_STREAM_TIMEOUT,
                safetyMaximum,
                "totalTimeout"
        ));
    }

    private Optional<Duration> websocketIdleTimeout(
            GatewayRouteTransportPolicy routePolicy,
            GatewayTransportProtocol protocol,
            Optional<Duration> profileDefault,
            Duration safetyMaximum) {
        Long configured = routePolicy == null
                ? null
                : routePolicy.websocketIdleTimeoutMs();
        Optional<Duration> selected = configured == null
                ? profileDefault
                : Optional.of(duration(
                        configured,
                        MIN_STREAM_TIMEOUT,
                        safetyMaximum,
                        "websocketIdleTimeoutMs"
                ));
        if (protocol != GatewayTransportProtocol.WEBSOCKET) {
            return Optional.empty();
        }
        return selected.map(timeout -> duration(
                timeout,
                MIN_STREAM_TIMEOUT,
                safetyMaximum,
                "websocketIdleTimeout"
        ));
    }

    private OptionalLong websocketFrameLimit(
            GatewayRouteTransportPolicy routePolicy,
            GatewayTransportProtocol protocol,
            OptionalLong profileDefault,
            long safetyMaximum) {
        Long configured = routePolicy == null
                ? null
                : routePolicy.websocketMaxFrameBytes();
        OptionalLong selected = configured == null
                ? profileDefault
                : OptionalLong.of(range(
                        configured,
                        MIN_WEBSOCKET_FRAME_BYTES,
                        safetyMaximum,
                        "websocketMaxFrameBytes"
                ));
        if (protocol != GatewayTransportProtocol.WEBSOCKET) {
            return OptionalLong.empty();
        }
        if (selected.isPresent()) {
            range(
                    selected.getAsLong(),
                    MIN_WEBSOCKET_FRAME_BYTES,
                    safetyMaximum,
                    "websocketMaxFrameBytes"
            );
        }
        return selected;
    }

    private Duration duration(
            Long configuredMs,
            Duration fallback,
            Duration minimum,
            Duration maximum,
            String field) {
        return configuredMs == null
                ? duration(fallback, minimum, maximum, field)
                : duration(
                        Duration.ofMillis(configuredMs),
                        minimum,
                        maximum,
                        field
                );
    }

    private Duration streamDuration(
            Long configuredMs,
            Duration fallback,
            boolean legacyInherited,
            Duration maximum,
            String field) {
        if (configuredMs != null) {
            return duration(
                    configuredMs,
                    MIN_STREAM_TIMEOUT,
                    maximum,
                    field
            );
        }
        if (!legacyInherited) {
            return duration(
                    fallback,
                    MIN_STREAM_TIMEOUT,
                    maximum,
                    field
            );
        }
        if (fallback.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(
                    field + " must not exceed " + maximum
            );
        }
        return fallback;
    }

    private Duration duration(
            long configuredMs,
            Duration minimum,
            Duration maximum,
            String field) {
        return duration(
                Duration.ofMillis(configuredMs),
                minimum,
                maximum,
                field
        );
    }

    private Duration duration(
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

    private long range(long value, long minimum, long maximum, String field) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    field + " must be between " + minimum + " and " + maximum
            );
        }
        return value;
    }

    private <T> T value(T configured, T fallback) {
        return configured == null ? fallback : configured;
    }

    private boolean value(Boolean configured, boolean fallback) {
        return configured == null ? fallback : configured;
    }

    private record ProfileDefaults(
            GatewayTransportProtocol transportProtocol,
            GatewayRequestBodyMode requestBodyMode,
            GatewayTransportResponseMode responseMode,
            long maxRequestBodyBytes,
            OptionalLong maxResponseBodyBytes,
            Duration connectTimeout,
            Duration responseHeaderTimeout,
            Duration streamIdleTimeout,
            Optional<Duration> totalTimeout,
            Optional<Duration> websocketIdleTimeout,
            OptionalLong websocketMaxFrameBytes,
            boolean bodyLogEnabled,
            boolean retryAllowed) {
    }
}
