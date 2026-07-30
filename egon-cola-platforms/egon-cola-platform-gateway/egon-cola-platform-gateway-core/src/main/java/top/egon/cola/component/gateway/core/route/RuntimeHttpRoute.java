package top.egon.cola.component.gateway.core.route;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record RuntimeHttpRoute(
        String routeId,
        String operationId,
        String gatewayGroupId,
        Set<AccessZone> accessZones,
        String hostPattern,
        Set<String> methods,
        String pathPattern,
        boolean externalAccessible,
        ProviderServiceKey upstream,
        Set<String> policyRefs,
        int priority,
        GatewayResponseMode responseMode,
        Map<String, String> metadata,
        EffectiveGatewayTransportPolicy transportPolicy
) {

    public RuntimeHttpRoute(
            String routeId,
            String operationId,
            String gatewayGroupId,
            Set<AccessZone> accessZones,
            String hostPattern,
            Set<String> methods,
            String pathPattern,
            boolean externalAccessible,
            ProviderServiceKey upstream,
            Set<String> policyRefs,
            int priority,
            GatewayResponseMode responseMode,
            Map<String, String> metadata) {
        this(
                routeId,
                operationId,
                gatewayGroupId,
                accessZones,
                hostPattern,
                methods,
                pathPattern,
                externalAccessible,
                upstream,
                policyRefs,
                priority,
                responseMode,
                metadata,
                EffectiveGatewayTransportPolicy.legacy()
        );
    }

    public RuntimeHttpRoute {
        routeId = required(routeId, "routeId");
        operationId = required(operationId, "operationId");
        gatewayGroupId = required(gatewayGroupId, "gatewayGroupId");
        accessZones = Set.copyOf(Objects.requireNonNull(accessZones, "accessZones"));
        if (accessZones.isEmpty()) {
            throw new IllegalArgumentException("accessZones must not be empty");
        }
        hostPattern = required(hostPattern, "hostPattern")
                .toLowerCase(Locale.ROOT);
        validateHostPattern(hostPattern);
        methods = Objects.requireNonNull(methods, "methods")
                .stream()
                .map(value -> required(value, "method").toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("methods must not be empty");
        }
        pathPattern = required(pathPattern, "pathPattern");
        if (!pathPattern.startsWith("/")) {
            throw new IllegalArgumentException("pathPattern must start with '/'");
        }
        upstream = Objects.requireNonNull(upstream, "upstream");
        policyRefs = Set.copyOf(Objects.requireNonNull(policyRefs, "policyRefs"));
        responseMode = Objects.requireNonNull(responseMode, "responseMode");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
        transportPolicy = Objects.requireNonNull(
                transportPolicy,
                "transportPolicy"
        );
        if (accessZones.contains(AccessZone.PUBLIC) && !externalAccessible) {
            throw new IllegalArgumentException(
                    "PUBLIC route must be externally accessible"
            );
        }
    }

    public boolean exactHost() {
        return !hostPattern.equals("*") && !hostPattern.startsWith("*.");
    }

    private static void validateHostPattern(String pattern) {
        if (pattern.equals("*")) {
            return;
        }
        if (pattern.startsWith("*.")) {
            if (pattern.length() < 4 || pattern.substring(2).contains("*")) {
                throw new IllegalArgumentException("invalid wildcard host");
            }
            return;
        }
        if (pattern.contains("*")) {
            throw new IllegalArgumentException("invalid host pattern");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
