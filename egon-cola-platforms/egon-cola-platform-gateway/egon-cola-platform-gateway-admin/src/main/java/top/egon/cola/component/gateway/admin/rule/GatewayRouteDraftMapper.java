package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Keeps the route draft JSON boundary compatible while emitting one canonical
 * shape for new writes and release compilation.
 */
public final class GatewayRouteDraftMapper {

    private static final Set<String> LEGACY_KEYS = Set.of(
            "listener",
            "method",
            "path",
            "protocol",
            "fullMethodName",
            "providerServiceName",
            "operationExternalAccessible"
    );

    public Map<String, Object> canonicalize(Map<String, Object> content) {
        Objects.requireNonNull(content, "content");
        Map<String, Object> canonical = new LinkedHashMap<>(content);
        LEGACY_KEYS.forEach(canonical::remove);

        putText(canonical, "host", text(content.get("host")), false);
        putText(
                canonical,
                "httpMethod",
                canonicalOrLegacy(content, "httpMethod", "method"),
                true
        );
        putText(
                canonical,
                "pathPattern",
                canonicalOrLegacy(content, "pathPattern", "path"),
                false
        );
        putAccessZones(canonical, content);
        canonical.put(
                "priority",
                content.get("priority") == null ? 0 : content.get("priority")
        );
        copyTransportPolicy(canonical, content.get("transportPolicy"));
        return Collections.unmodifiableMap(canonical);
    }

    public GatewayRouteTransportPolicy transportPolicy(
            Map<String, Object> canonicalContent) {
        Object raw = canonicalContent.get("transportPolicy");
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof Map<?, ?> policy)) {
            throw new IllegalArgumentException(
                    "transportPolicy must be an object"
            );
        }
        return new GatewayRouteTransportPolicy(
                enumeration(policy, "profile", GatewayRouteProfile.class),
                enumeration(
                        policy,
                        "transportProtocol",
                        GatewayTransportProtocol.class
                ),
                enumeration(
                        policy,
                        "requestBodyMode",
                        GatewayRequestBodyMode.class
                ),
                enumeration(
                        policy,
                        "responseMode",
                        GatewayTransportResponseMode.class
                ),
                number(policy, "maxRequestBodyBytes"),
                number(policy, "connectTimeoutMs"),
                number(policy, "responseHeaderTimeoutMs"),
                number(policy, "streamIdleTimeoutMs"),
                number(policy, "totalTimeoutMs"),
                number(policy, "websocketIdleTimeoutMs"),
                number(policy, "websocketMaxFrameBytes"),
                flag(policy, "bodyLogEnabled"),
                flag(policy, "retryEnabled")
        );
    }

    private void putAccessZones(
            Map<String, Object> canonical,
            Map<String, Object> content) {
        if (content.containsKey("accessZones")) {
            Object zones = content.get("accessZones");
            if (zones instanceof Iterable<?> values) {
                canonical.put("accessZones", normalizedZones(values));
            }
            return;
        }
        String listener = text(content.get("listener"));
        if (listener == null) {
            canonical.remove("accessZones");
            return;
        }
        canonical.put(
                "accessZones",
                List.of(listener.toUpperCase(Locale.ROOT))
        );
    }

    private List<String> normalizedZones(Iterable<?> values) {
        LinkedHashSet<String> zones = new LinkedHashSet<>();
        for (Object value : values) {
            String zone = text(value);
            if (zone != null) {
                zones.add(zone.toUpperCase(Locale.ROOT));
            }
        }
        return List.copyOf(zones);
    }

    private void copyTransportPolicy(
            Map<String, Object> canonical,
            Object raw) {
        if (raw == null) {
            canonical.remove("transportPolicy");
            return;
        }
        if (raw instanceof Map<?, ?> policy) {
            Map<String, Object> copy = new LinkedHashMap<>();
            policy.forEach((key, value) -> copy.put(key.toString(), value));
            canonical.put(
                    "transportPolicy",
                    Collections.unmodifiableMap(copy)
            );
        }
    }

    private String canonicalOrLegacy(
            Map<String, Object> content,
            String canonical,
            String legacy) {
        return content.containsKey(canonical)
                ? text(content.get(canonical))
                : text(content.get(legacy));
    }

    private void putText(
            Map<String, Object> canonical,
            String key,
            String value,
            boolean uppercase) {
        if (value == null) {
            canonical.remove(key);
            return;
        }
        canonical.put(
                key,
                uppercase ? value.toUpperCase(Locale.ROOT) : value
        );
    }

    private String text(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString().trim();
    }

    private <E extends Enum<E>> E enumeration(
            Map<?, ?> policy,
            String field,
            Class<E> type) {
        Object value = policy.get(field);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toString());
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException(
                    "transportPolicy." + field + " contains an unknown value",
                    unknown
            );
        }
    }

    private Long number(Map<?, ?> policy, String field) {
        Object value = policy.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                    "transportPolicy." + field + " must be an integer"
            );
        }
        return number.longValue();
    }

    private Boolean flag(Map<?, ?> policy, String field) {
        Object value = policy.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Boolean flag)) {
            throw new IllegalArgumentException(
                    "transportPolicy." + field + " must be a boolean"
            );
        }
        return flag;
    }
}
