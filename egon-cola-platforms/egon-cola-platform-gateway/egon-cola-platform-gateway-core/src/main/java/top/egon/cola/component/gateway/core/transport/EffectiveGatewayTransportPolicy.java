package top.egon.cola.component.gateway.core.transport;

import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record EffectiveGatewayTransportPolicy(
        GatewayRouteProfile profile,
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
        boolean retryAllowed,
        boolean authorizationForwardingAllowed
) {

    public EffectiveGatewayTransportPolicy {
        profile = Objects.requireNonNull(profile, "profile");
        transportProtocol = Objects.requireNonNull(
                transportProtocol,
                "transportProtocol"
        );
        requestBodyMode = Objects.requireNonNull(
                requestBodyMode,
                "requestBodyMode"
        );
        responseMode = Objects.requireNonNull(responseMode, "responseMode");
        positive(maxRequestBodyBytes, "maxRequestBodyBytes");
        maxResponseBodyBytes = positive(
                maxResponseBodyBytes,
                "maxResponseBodyBytes"
        );
        connectTimeout = positive(connectTimeout, "connectTimeout");
        responseHeaderTimeout = positive(
                responseHeaderTimeout,
                "responseHeaderTimeout"
        );
        streamIdleTimeout = positive(
                streamIdleTimeout,
                "streamIdleTimeout"
        );
        totalTimeout = positive(totalTimeout, "totalTimeout");
        websocketIdleTimeout = positive(
                websocketIdleTimeout,
                "websocketIdleTimeout"
        );
        websocketMaxFrameBytes = positive(
                websocketMaxFrameBytes,
                "websocketMaxFrameBytes"
        );
        if (transportProtocol != GatewayTransportProtocol.WEBSOCKET
                && (websocketIdleTimeout.isPresent()
                || websocketMaxFrameBytes.isPresent())) {
            throw new IllegalArgumentException(
                    "websocket limits require WEBSOCKET transport"
            );
        }
    }

    public static EffectiveGatewayTransportPolicy legacy() {
        GatewayTransportDefaults defaults = GatewayTransportDefaults.legacy();
        return new EffectiveGatewayTransportPolicy(
                GatewayRouteProfile.DEFAULT,
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
                defaults.retryAllowed(),
                false
        );
    }

    private static long positive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static Optional<Duration> positive(
            Optional<Duration> value,
            String field) {
        Objects.requireNonNull(value, field);
        value.ifPresent(duration -> positive(duration, field));
        return value;
    }

    private static OptionalLong positive(OptionalLong value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isPresent() && value.getAsLong() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
