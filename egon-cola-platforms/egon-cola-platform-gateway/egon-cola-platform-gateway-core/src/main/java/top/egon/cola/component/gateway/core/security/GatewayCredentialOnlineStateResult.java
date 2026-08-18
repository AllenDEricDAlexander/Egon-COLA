package top.egon.cola.component.gateway.core.security;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of checking whether an already authenticated USER credential remains online.
 */
public record GatewayCredentialOnlineStateResult(
        Outcome outcome,
        Map<String, List<String>> responseHeaders
) {

    public enum Outcome {
        ACTIVE,
        INACTIVE,
        UNAVAILABLE
    }

    public GatewayCredentialOnlineStateResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
        responseHeaders = immutableHeaders(responseHeaders);
        if (outcome != Outcome.INACTIVE && !responseHeaders.isEmpty()) {
            throw new IllegalArgumentException(
                    "only INACTIVE may contain response headers");
        }
    }

    public static GatewayCredentialOnlineStateResult active() {
        return new GatewayCredentialOnlineStateResult(Outcome.ACTIVE, Map.of());
    }

    public static GatewayCredentialOnlineStateResult inactive(
            Map<String, List<String>> responseHeaders) {
        return new GatewayCredentialOnlineStateResult(
                Outcome.INACTIVE,
                responseHeaders);
    }

    public static GatewayCredentialOnlineStateResult unavailable() {
        return new GatewayCredentialOnlineStateResult(
                Outcome.UNAVAILABLE,
                Map.of());
    }

    private static Map<String, List<String>> immutableHeaders(
            Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> result = new java.util.LinkedHashMap<>();
        source.forEach((name, values) -> {
            if (name == null || !"set-cookie".equalsIgnoreCase(name)) {
                throw new IllegalArgumentException(
                        "only Set-Cookie response headers are allowed");
            }
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException(
                        "Set-Cookie values are required");
            }
            result.put("set-cookie", List.copyOf(values));
        });
        return Map.copyOf(result);
    }
}
