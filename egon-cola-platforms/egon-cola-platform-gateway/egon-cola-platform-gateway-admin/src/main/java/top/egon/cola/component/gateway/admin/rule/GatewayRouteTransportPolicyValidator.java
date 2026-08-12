package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.math.BigDecimal;
import java.math.BigInteger;
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
 * 补充说明 / Supplementary summary: {@code GatewayRouteTransportPolicyValidator} 是校验器，位于当前 Gateway 模块的相关包中，负责网关路由传输策略校验器相关的职责与边界。
 * English supplement: {@code GatewayRouteTransportPolicyValidator} is a gateway route transport policy validator validator in the current Gateway module; it owns the gateway route transport policy validator-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRouteTransportPolicyValidator {

    /**
     * 中文说明：表示 ENUMVALUES 这一固定值；它属于 {@code GatewayRouteTransportPolicyValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value enum values; it is a state, type, or protocol value of {@code GatewayRouteTransportPolicyValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRouteTransportPolicyValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRouteTransportPolicyValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
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

    /**
     * 中文说明：表示 ENUMFIELDS 这一固定值；它属于 {@code GatewayRouteTransportPolicyValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value enum fields; it is a state, type, or protocol value of {@code GatewayRouteTransportPolicyValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRouteTransportPolicyValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRouteTransportPolicyValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final List<String> ENUM_FIELDS = List.of(
            "profile",
            "transportProtocol",
            "requestBodyMode",
            "responseMode"
    );

    /**
     * 中文说明：表示 NUMERICRANGES 这一固定值；它属于 {@code GatewayRouteTransportPolicyValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value numeric ranges; it is a state, type, or protocol value of {@code GatewayRouteTransportPolicyValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRouteTransportPolicyValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRouteTransportPolicyValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
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

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param routeContent 参数 路由Content；parameter route content。
     * @param operationProtocol 参数 操作Protocol；parameter operation protocol。
     * @param operationResponseMode 参数 操作响应Mode；parameter operation response mode。
     * @return 返回 validate 的处理结果；returns the result of the operation.
     */
    public List<ValidationIssue> validate(
            Map<String, Object> routeContent,
            GatewayProtocol operationProtocol,
            GatewayResponseMode operationResponseMode) {
        List<ValidationIssue> issues = new ArrayList<>();
        Object rawHost = routeContent.get("host");
        Object rawMethod = routeContent.get("httpMethod");
        Object rawPathPattern = routeContent.get("pathPattern");
        String host = text(rawHost);
        String method = text(rawMethod);
        String pathPattern = text(rawPathPattern);
        if (rawHost != null && !(rawHost instanceof String)) {
            issues.add(issue(
                    "host",
                    "ROUTE_HOST_INVALID",
                    "Host must be a string"
            ));
        } else if (host == null) {
            issues.add(issue(
                    "host",
                    "ROUTE_HOST_REQUIRED",
                    "Host is required"
            ));
        }
        if (rawMethod != null && !(rawMethod instanceof String)) {
            issues.add(issue(
                    "httpMethod",
                    "ROUTE_METHOD_INVALID",
                    "HTTP Method must be a string"
            ));
        } else if (method == null) {
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

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     * @param operation 参数 操作；parameter operation。
     * @return 返回 validate 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 validateAccessZones 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate access zones operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.validateAccessZones(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param raw 参数 raw；parameter raw。
     * @param issues 参数 issues；parameter issues。
     */
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

    /**
     * 中文说明：执行 validate策略 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate policy operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.validatePolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param method 参数 方法；parameter method。
     * @param raw 参数 raw；parameter raw。
     * @param operationProtocol 参数 操作Protocol；parameter operation protocol。
     * @param operationResponseMode 参数 操作响应Mode；parameter operation response mode。
     * @param issues 参数 issues；parameter issues。
     */
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

    /**
     * 中文说明：执行 validateEnums 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate enums operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.validateEnums(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param issues 参数 issues；parameter issues。
     */
    private void validateEnums(
            Map<?, ?> policy,
            List<ValidationIssue> issues) {
        for (String field : ENUM_FIELDS) {
            Object value = policy.get(field);
            if (value != null
                    && (!(value instanceof String text)
                    || !ENUM_VALUES.get(field).contains(text))) {
                issues.add(issue(
                        "transportPolicy." + field,
                        "TRANSPORT_ENUM_UNKNOWN",
                        field + " contains an unknown value"
                ));
            }
        }
    }

    /**
     * 中文说明：执行 validateNumbers 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate numbers operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.validateNumbers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param issues 参数 issues；parameter issues。
     */
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

    /**
     * 中文说明：执行 validateBooleans 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate booleans operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.validateBooleans(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param issues 参数 issues；parameter issues。
     */
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

    /**
     * 中文说明：执行 validateCombinations 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate combinations operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.validateCombinations(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param method 参数 方法；parameter method。
     * @param policy 参数 策略；parameter policy。
     * @param operationProtocol 参数 操作Protocol；parameter operation protocol。
     * @param operationResponseMode 参数 操作响应Mode；parameter operation response mode。
     * @param issues 参数 issues；parameter issues。
     */
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

    /**
     * 中文说明：执行 incompatible 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the incompatible operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.incompatible(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actual 参数 actual；parameter actual。
     * @param rejected 参数 rejected；parameter rejected。
     * @param field 参数 field；parameter field。
     * @param code 参数 code；parameter code。
     * @param message 参数 消息；parameter message。
     * @param issues 参数 issues；parameter issues。
     */
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

    /**
     * 中文说明：执行 策略 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the policy operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.policy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @return 返回 策略 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 put 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.put(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param key 参数 键；parameter key。
     * @param value 参数 值；parameter value。
     */
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

    /**
     * 中文说明：执行 integer 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the integer operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.integer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 integer 的处理结果；returns the result of the operation.
     */
    private Long integer(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        try {
            if (number instanceof BigDecimal decimal) {
                return decimal.longValueExact();
            }
            if (number instanceof BigInteger integer) {
                return integer.longValueExact();
            }
            if (number instanceof Byte
                    || number instanceof Short
                    || number instanceof Integer
                    || number instanceof Long) {
                return number.longValue();
            }
            double decimal = number.doubleValue();
            if (!Double.isFinite(decimal)
                    || decimal != Math.rint(decimal)
                    || decimal < Long.MIN_VALUE
                    || decimal > Long.MAX_VALUE) {
                return null;
            }
            return (long) decimal;
        } catch (ArithmeticException invalid) {
            return null;
        }
    }

    /**
     * 中文说明：执行 值 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the value operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.value(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param field 参数 field；parameter field。
     * @return 返回 值 的处理结果；returns the result of the operation.
     */
    private String value(Map<?, ?> values, String field) {
        Object value = values.get(field);
        return value instanceof String text ? text : null;
    }

    /**
     * 中文说明：执行 text 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the text operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.text(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 text 的处理结果；returns the result of the operation.
     */
    private String text(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    /**
     * 中文说明：执行 issue 操作；该方法是 {@code GatewayRouteTransportPolicyValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the issue operation; this method is the invocation entry point on {@code GatewayRouteTransportPolicyValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRouteTransportPolicyValidator.issue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param path 参数 path；parameter path。
     * @param code 参数 code；parameter code。
     * @param message 参数 消息；parameter message。
     * @return 返回 issue 的处理结果；returns the result of the operation.
     */
    private ValidationIssue issue(
            String path,
            String code,
            String message) {
        return new ValidationIssue(path, code, message);
    }

    /**
     * 中文说明：{@code Range} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Range相关的职责与边界。
     * English summary: {@code Range} is an immutable data carrier in the current Gateway module; it owns the range-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param minimum 参数 minimum；parameter minimum。
     * @param maximum 参数 maximum；parameter maximum。
     */
    private record Range(
    /**
     * 中文说明：保存 minimum 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayRouteTransportPolicyValidator.Range} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by minimum; its type is {@code long}, and {@code GatewayRouteTransportPolicyValidator.Range} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRouteTransportPolicyValidator.Range} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRouteTransportPolicyValidator.Range}; do not couple callers to its representation when the owning type exposes an API.
     */
    long minimum,
    /**
     * 中文说明：保存 maximum 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayRouteTransportPolicyValidator.Range} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum; its type is {@code long}, and {@code GatewayRouteTransportPolicyValidator.Range} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRouteTransportPolicyValidator.Range} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRouteTransportPolicyValidator.Range}; do not couple callers to its representation when the owning type exposes an API.
     */
    long maximum) {
    }

    /**
     * 中文说明：{@code ValidationIssue} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责ValidationIssue相关的职责与边界。
     * English summary: {@code ValidationIssue} is an immutable data carrier in the current Gateway module; it owns the validation issue-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param path 参数 path；parameter path。
     * @param code 参数 code；parameter code。
     * @param message 参数 消息；parameter message。
     */
    public record ValidationIssue(
            /**
             * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayRouteTransportPolicyValidator.ValidationIssue} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code GatewayRouteTransportPolicyValidator.ValidationIssue} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayRouteTransportPolicyValidator.ValidationIssue} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRouteTransportPolicyValidator.ValidationIssue}; do not couple callers to its representation when the owning type exposes an API.
             */
            String path,
            /**
             * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayRouteTransportPolicyValidator.ValidationIssue} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code GatewayRouteTransportPolicyValidator.ValidationIssue} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayRouteTransportPolicyValidator.ValidationIssue} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRouteTransportPolicyValidator.ValidationIssue}; do not couple callers to its representation when the owning type exposes an API.
             */
            String code,
            /**
             * 中文说明：保存 消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayRouteTransportPolicyValidator.ValidationIssue} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by message; its type is {@code String}, and {@code GatewayRouteTransportPolicyValidator.ValidationIssue} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayRouteTransportPolicyValidator.ValidationIssue} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRouteTransportPolicyValidator.ValidationIssue}; do not couple callers to its representation when the owning type exposes an API.
             */
            String message
    ) {
    }
}
