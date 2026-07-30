package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies the fixed transport-policy publication invariants at both draft and
 * final rule compilation boundaries.
 */
public final class GatewayRouteTransportPolicyValidator {

    private static final Map<String, Set<String>> ENUM_VALUES = Map.of(
            "profile", Set.of("DEFAULT", "OPENAI_HTTP"),
            "transportProtocol", Set.of("HTTP", "WEBSOCKET"),
            "requestBodyMode", Set.of("AGGREGATED", "STREAMING"),
            "responseMode", Set.of(
                    "STANDARD",
                    "AUTO_STREAM",
                    "SSE",
                    "BINARY_STREAM"
            )
    );

    private static final List<String> ENUM_FIELDS = List.of(
            "profile",
            "transportProtocol",
            "requestBodyMode",
            "responseMode"
    );

    private static final Map<String, Range> NUMERIC_RANGES;

    static {
        Map<String, Range> ranges = new LinkedHashMap<>();
        ranges.put("maxRequestBodyBytes", new Range(1L, 1_073_741_824L));
        ranges.put("connectTimeoutMs", new Range(100L, 60_000L));
        ranges.put(
                "responseHeaderTimeoutMs",
                new Range(1_000L, 600_000L)
        );
        ranges.put(
                "streamIdleTimeoutMs",
                new Range(1_000L, 1_800_000L)
        );
        ranges.put("totalTimeoutMs", new Range(1_000L, 7_200_000L));
        ranges.put(
                "websocketIdleTimeoutMs",
                new Range(1_000L, 7_200_000L)
        );
        ranges.put(
                "websocketMaxFrameBytes",
                new Range(1_024L, 67_108_864L)
        );
        NUMERIC_RANGES = Collections.unmodifiableMap(ranges);
    }

    public List<ValidationIssue> validate(
            Map<String, Object> routeContent,
            GatewayProtocol operationProtocol,
            GatewayResponseMode operationResponseMode) {
        List<ValidationIssue> issues = new ArrayList<>();
        String host = text(routeContent.get("host"));
        String method = text(routeContent.get("httpMethod"));
        String pathPattern = text(routeContent.get("pathPattern"));
        if (host == null) {
            issues.add(issue(
                    "host",
                    "ROUTE_HOST_REQUIRED",
                    "Host is required"
            ));
        }
        if (method == null) {
            issues.add(issue(
                    "httpMethod",
                    "ROUTE_METHOD_REQUIRED",
                    "HTTP Method is required"
            ));
        }
        if (pathPattern == null || !pathPattern.startsWith("/")) {
            issues.add(issue(
                    "pathPattern",
                    "ROUTE_PATH_INVALID",
                    "Path Pattern must start with /"
            ));
        }
        validateAccessZones(routeContent.get("accessZones"), issues);
        validatePolicy(
                method,
                routeContent.get("transportPolicy"),
                operationProtocol,
                operationResponseMode,
                issues
        );
        return List.copyOf(issues);
    }

    public List<ValidationIssue> validate(
            GatewayRuntimeRoute route,
            GatewayRuntimeOperation operation) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("host", route.host());
        content.put("httpMethod", route.httpMethod());
        content.put("pathPattern", route.pathPattern());
        content.put(
                "accessZones",
                route.accessZones().stream().map(AccessZone::name).toList()
        );
        if (route.transportPolicy() != null) {
            content.put("transportPolicy", policy(route.transportPolicy()));
        }
        return validate(
                content,
                operation.protocol(),
                GatewayResponseMode.valueOf(operation.responseMode())
        );
    }

    private void validateAccessZones(
            Object raw,
            List<ValidationIssue> issues) {
        if (!(raw instanceof Collection<?> zones) || zones.isEmpty()) {
            issues.add(issue(
                    "accessZones",
                    "ROUTE_ACCESS_ZONE_REQUIRED",
                    "At least one Access Zone is required"
            ));
            return;
        }
        for (Object value : zones) {
            String zone = text(value);
            if (zone == null || !Set.of("PUBLIC", "INTERNAL").contains(zone)) {
                issues.add(issue(
                        "accessZones",
                        "ROUTE_ACCESS_ZONE_INVALID",
                        "Access Zone contains an unknown value"
                ));
                return;
            }
        }
    }

    private void validatePolicy(
            String method,
            Object raw,
            GatewayProtocol operationProtocol,
            GatewayResponseMode operationResponseMode,
            List<ValidationIssue> issues) {
        if (raw == null) {
            return;
        }
        if (!(raw instanceof Map<?, ?> policy)) {
            issues.add(issue(
                    "transportPolicy",
                    "TRANSPORT_POLICY_INVALID",
                    "transportPolicy must be an object"
            ));
            return;
        }
        validateEnums(policy, issues);
        validateNumbers(policy, issues);
        validateBooleans(policy, issues);
        validateCombinations(
                method,
                policy,
                operationProtocol,
                operationResponseMode,
                issues
        );
    }

    private void validateEnums(
            Map<?, ?> policy,
            List<ValidationIssue> issues) {
        for (String field : ENUM_FIELDS) {
            Object value = policy.get(field);
            if (value != null
                    && !ENUM_VALUES.get(field).contains(value.toString())) {
                issues.add(issue(
                        "transportPolicy." + field,
                        "TRANSPORT_ENUM_UNKNOWN",
                        field + " contains an unknown value"
                ));
            }
        }
    }

    private void validateNumbers(
            Map<?, ?> policy,
            List<ValidationIssue> issues) {
        NUMERIC_RANGES.forEach((field, range) -> {
            Object value = policy.get(field);
            if (value == null) {
                return;
            }
            Long number = integer(value);
            if (number == null
                    || number < range.minimum()
                    || number > range.maximum()) {
                issues.add(issue(
                        "transportPolicy." + field,
                        "TRANSPORT_VALUE_OUT_OF_RANGE",
                        field + " must be an integer from "
                                + range.minimum()
                                + " to "
                                + range.maximum()
                ));
            }
        });
    }

    private void validateBooleans(
            Map<?, ?> policy,
            List<ValidationIssue> issues) {
        for (String field : List.of("bodyLogEnabled", "retryEnabled")) {
            Object value = policy.get(field);
            if (value != null && !(value instanceof Boolean)) {
                issues.add(issue(
                        "transportPolicy." + field,
                        "TRANSPORT_BOOLEAN_INVALID",
                        field + " must be a boolean"
                ));
            }
        }
    }

    private void validateCombinations(
            String method,
            Map<?, ?> policy,
            GatewayProtocol operationProtocol,
            GatewayResponseMode operationResponseMode,
            List<ValidationIssue> issues) {
        String profile = value(policy, "profile");
        String transport = value(policy, "transportProtocol");
        String requestMode = value(policy, "requestBodyMode");
        String responseMode = value(policy, "responseMode");
        if ("WEBSOCKET".equals(transport) && !"GET".equals(method)) {
            issues.add(issue(
                    "httpMethod",
                    "WEBSOCKET_GET_REQUIRED",
                    "WebSocket Route must use GET"
            ));
        }
        if (operationProtocol == GatewayProtocol.RPC) {
            incompatible(
                    profile,
                    "OPENAI_HTTP",
                    "profile",
                    "RPC_TRANSPORT_UNSUPPORTED",
                    "RPC Operation cannot use OPENAI_HTTP",
                    issues
            );
            incompatible(
                    transport,
                    "WEBSOCKET",
                    "transportProtocol",
                    "RPC_TRANSPORT_UNSUPPORTED",
                    "RPC Operation cannot use WebSocket",
                    issues
            );
            incompatible(
                    requestMode,
                    "STREAMING",
                    "requestBodyMode",
                    "RPC_TRANSPORT_UNSUPPORTED",
                    "RPC Operation cannot stream the request body",
                    issues
            );
            if (responseMode != null
                    && Set.of("AUTO_STREAM", "SSE", "BINARY_STREAM")
                            .contains(responseMode)) {
                issues.add(issue(
                        "transportPolicy.responseMode",
                        "RPC_TRANSPORT_UNSUPPORTED",
                        "RPC Operation cannot stream the response body"
                ));
            }
        }
        if (operationResponseMode == GatewayResponseMode.WRAPPED) {
            incompatible(
                    profile,
                    "OPENAI_HTTP",
                    "profile",
                    "WRAPPED_TRANSPORT_UNSUPPORTED",
                    "WRAPPED Operation cannot use OPENAI_HTTP",
                    issues
            );
            incompatible(
                    transport,
                    "WEBSOCKET",
                    "transportProtocol",
                    "WRAPPED_TRANSPORT_UNSUPPORTED",
                    "WRAPPED Operation cannot use WebSocket",
                    issues
            );
            incompatible(
                    requestMode,
                    "STREAMING",
                    "requestBodyMode",
                    "WRAPPED_TRANSPORT_UNSUPPORTED",
                    "WRAPPED Operation cannot stream the request body",
                    issues
            );
            if (responseMode != null
                    && Set.of("AUTO_STREAM", "SSE", "BINARY_STREAM")
                            .contains(responseMode)) {
                issues.add(issue(
                        "transportPolicy.responseMode",
                        "WRAPPED_TRANSPORT_UNSUPPORTED",
                        "WRAPPED Operation cannot stream the response body"
                ));
            }
        }
    }

    private void incompatible(
            String actual,
            String rejected,
            String field,
            String code,
            String message,
            List<ValidationIssue> issues) {
        if (rejected.equals(actual)) {
            issues.add(issue(
                    "transportPolicy." + field,
                    code,
                    message
            ));
        }
    }

    private Map<String, Object> policy(GatewayRouteTransportPolicy policy) {
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, "profile", policy.profile());
        put(values, "transportProtocol", policy.transportProtocol());
        put(values, "requestBodyMode", policy.requestBodyMode());
        put(values, "responseMode", policy.responseMode());
        put(values, "maxRequestBodyBytes", policy.maxRequestBodyBytes());
        put(values, "connectTimeoutMs", policy.connectTimeoutMs());
        put(
                values,
                "responseHeaderTimeoutMs",
                policy.responseHeaderTimeoutMs()
        );
        put(values, "streamIdleTimeoutMs", policy.streamIdleTimeoutMs());
        put(values, "totalTimeoutMs", policy.totalTimeoutMs());
        put(
                values,
                "websocketIdleTimeoutMs",
                policy.websocketIdleTimeoutMs()
        );
        put(
                values,
                "websocketMaxFrameBytes",
                policy.websocketMaxFrameBytes()
        );
        put(values, "bodyLogEnabled", policy.bodyLogEnabled());
        put(values, "retryEnabled", policy.retryEnabled());
        return values;
    }

    private void put(Map<String, Object> values, String key, Object value) {
        if (value != null) {
            values.put(
                    key,
                    value instanceof Enum<?> enumeration
                            ? enumeration.name()
                            : value
            );
        }
    }

    private Long integer(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        try {
            BigDecimal decimal = new BigDecimal(number.toString());
            return decimal.longValueExact();
        } catch (ArithmeticException | NumberFormatException invalid) {
            return null;
        }
    }

    private String value(Map<?, ?> values, String field) {
        Object value = values.get(field);
        return value == null ? null : value.toString();
    }

    private String text(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString().trim();
    }

    private ValidationIssue issue(
            String path,
            String code,
            String message) {
        return new ValidationIssue(path, code, message);
    }

    private record Range(long minimum, long maximum) {
    }

    public record ValidationIssue(
            String path,
            String code,
            String message
    ) {
    }
}
