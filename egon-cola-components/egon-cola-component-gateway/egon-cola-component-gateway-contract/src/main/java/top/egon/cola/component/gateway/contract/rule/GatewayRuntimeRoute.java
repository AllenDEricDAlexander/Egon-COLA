package top.egon.cola.component.gateway.contract.rule;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record GatewayRuntimeRoute(
        String routeId,
        String operationId,
        String host,
        String httpMethod,
        String pathPattern,
        Set<AccessZone> accessZones,
        int priority,
        boolean enabled
) {

    public GatewayRuntimeRoute {
        routeId = required(routeId, "routeId");
        operationId = required(operationId, "operationId");
        host = required(host, "host").toLowerCase(Locale.ROOT);
        httpMethod = required(httpMethod, "httpMethod")
                .toUpperCase(Locale.ROOT);
        pathPattern = required(pathPattern, "pathPattern");
        if (!pathPattern.startsWith("/")) {
            throw new IllegalArgumentException("pathPattern must start with /");
        }
        LinkedHashSet<AccessZone> sortedAccessZones = Objects.requireNonNull(
                        accessZones,
                        "accessZones"
                ).stream()
                .sorted(Comparator.comparing(Enum::name))
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));
        accessZones = Collections.unmodifiableSet(sortedAccessZones);
        if (accessZones.isEmpty()) {
            throw new IllegalArgumentException("accessZones must not be empty");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
