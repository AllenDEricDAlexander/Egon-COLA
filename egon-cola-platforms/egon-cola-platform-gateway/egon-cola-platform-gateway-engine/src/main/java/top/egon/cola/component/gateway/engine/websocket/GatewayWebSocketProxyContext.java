package top.egon.cola.component.gateway.engine.websocket;

import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.transport.GatewayCommitGuard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record GatewayWebSocketProxyContext(
        ProviderInstance provider,
        String pathAndQuery,
        Map<String, List<String>> headers,
        List<String> subprotocolCandidates,
        EffectiveGatewayTransportPolicy policy,
        GatewayCommitGuard commitGuard,
        GatewayWebSocketObserver observer
) {

    private static final Pattern TOKEN = Pattern.compile(
            "[!#$%&'*+\\-.^_`|~0-9A-Za-z]+"
    );

    public GatewayWebSocketProxyContext {
        provider = Objects.requireNonNull(provider, "provider");
        if (pathAndQuery == null
                || !pathAndQuery.startsWith("/")
                || pathAndQuery.contains("://")
                || pathAndQuery.indexOf('#') >= 0
                || pathAndQuery.indexOf('\r') >= 0
                || pathAndQuery.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(
                    "pathAndQuery must be an origin-form target"
            );
        }
        headers = immutableHeaders(headers);
        subprotocolCandidates = List.copyOf(Objects.requireNonNull(
                subprotocolCandidates,
                "subprotocolCandidates"
        ));
        if (subprotocolCandidates.stream().anyMatch(candidate ->
                candidate == null || !TOKEN.matcher(candidate).matches())) {
            throw new IllegalArgumentException(
                    "invalid WebSocket subprotocol candidate"
            );
        }
        policy = Objects.requireNonNull(policy, "policy");
        if (policy.transportProtocol()
                != GatewayTransportProtocol.WEBSOCKET) {
            throw new IllegalArgumentException(
                    "WebSocket context requires WEBSOCKET policy"
            );
        }
        if (policy.websocketIdleTimeout().isEmpty()
                || policy.websocketMaxFrameBytes().isEmpty()) {
            throw new IllegalArgumentException(
                    "WebSocket policy requires idle and frame limits"
            );
        }
        commitGuard = Objects.requireNonNull(
                commitGuard,
                "commitGuard"
        );
        observer = Objects.requireNonNull(observer, "observer");
    }

    public boolean acceptsSubprotocol(String selected) {
        return selected == null || selected.isBlank()
                || subprotocolCandidates.contains(selected);
    }

    private static Map<String, List<String>> immutableHeaders(
            Map<String, List<String>> source) {
        Objects.requireNonNull(source, "headers");
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((name, values) -> {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "WebSocket header name is required"
                );
            }
            List<String> safeValues = List.copyOf(
                    Objects.requireNonNull(values, "header values")
            );
            if (safeValues.stream().anyMatch(value -> value == null
                    || value.indexOf('\r') >= 0
                    || value.indexOf('\n') >= 0)) {
                throw new IllegalArgumentException(
                        "invalid WebSocket header value"
                );
            }
            copy.put(name, safeValues);
        });
        return Map.copyOf(copy);
    }
}
