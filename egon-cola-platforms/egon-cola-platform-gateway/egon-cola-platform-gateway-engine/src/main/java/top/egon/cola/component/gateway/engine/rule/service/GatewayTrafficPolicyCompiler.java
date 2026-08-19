package top.egon.cola.component.gateway.engine.rule.service;

import top.egon.cola.component.gateway.engine.rule.service.GatewayPolicyKeyCompiler;

import top.egon.cola.component.gateway.engine.common.traffic.domain.RateLimitFailureMode;
import top.egon.cola.component.gateway.engine.common.traffic.domain.RuntimeTrafficPolicy;
import top.egon.cola.component.gateway.engine.common.traffic.domain.TrafficPolicyScope;
import top.egon.cola.component.gateway.engine.common.traffic.domain.TrafficPolicyType;

import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewayTrafficPolicyCompiler} 是编译器，位于当前 Gateway 模块的相关包中，负责网关流量策略Compiler相关的职责与边界。
 * English summary: {@code GatewayTrafficPolicyCompiler} is a gateway traffic policy compiler compiler in the current Gateway module; it owns the gateway traffic policy compiler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayTrafficPolicyCompiler {

    /**
     * 中文说明：保存 键Compiler 对应的状态、依赖或配置值；字段类型为 {@code GatewayPolicyKeyCompiler}，由 {@code GatewayTrafficPolicyCompiler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by key compiler; its type is {@code GatewayPolicyKeyCompiler}, and {@code GatewayTrafficPolicyCompiler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficPolicyCompiler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficPolicyCompiler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayPolicyKeyCompiler keyCompiler =
            new GatewayPolicyKeyCompiler();

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code GatewayTrafficPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code GatewayTrafficPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficPolicyCompiler.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policies 参数 policies；parameter policies。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
    public Map<String, RuntimeTrafficPolicy> compile(
            List<GatewayRuntimePolicy> policies) {
        Map<String, RuntimeTrafficPolicy> result = new LinkedHashMap<>();
        for (GatewayRuntimePolicy source : policies.stream()
                .filter(this::trafficPolicy)
                .toList()) {
            Map<String, Object> config = source.configuration();
            TrafficPolicyType type = TrafficPolicyType.valueOf(source.type());
            String keyExpression = string(config, "keyExpression", null);
            if (type == TrafficPolicyType.RATE_LIMIT) {
                keyCompiler.compile(keyExpression);
            }
            if ((type == TrafficPolicyType.REQUEST_SIZE
                    || type == TrafficPolicyType.RESPONSE_SIZE)
                    && longValue(config, "maxBytes", 0) <= 0) {
                throw new IllegalArgumentException(
                        type + " maxBytes must be positive"
                );
            }
            RuntimeTrafficPolicy policy = new RuntimeTrafficPolicy(
                    source.policyId(),
                    type,
                    TrafficPolicyScope.valueOf(source.scope()),
                    bool(config, "enabled", true),
                    integer(config, "priority", 0),
                    keyExpression,
                    RateLimitFailureMode.valueOf(string(
                            config,
                            "failureMode",
                            "LOCAL_FALLBACK"
                    )),
                    config,
                    longValue(config, "stateEpoch", 0),
                    longValue(config, "policyVersion", 1)
            );
            if (result.putIfAbsent(policy.policyId(), policy) != null) {
                throw new IllegalArgumentException(
                        "duplicate traffic policy " + policy.policyId()
                );
            }
        }
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 流量策略 操作；该方法是 {@code GatewayTrafficPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the traffic policy operation; this method is the invocation entry point on {@code GatewayTrafficPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficPolicyCompiler.trafficPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @return 返回 流量策略 的处理结果；returns the result of the operation.
     */
    private boolean trafficPolicy(GatewayRuntimePolicy policy) {
        try {
            TrafficPolicyType.valueOf(policy.type());
            return true;
        } catch (IllegalArgumentException unsupported) {
            return false;
        }
    }

    /**
     * 中文说明：执行 string 操作；该方法是 {@code GatewayTrafficPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string operation; this method is the invocation entry point on {@code GatewayTrafficPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficPolicyCompiler.string(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 string 的处理结果；returns the result of the operation.
     */
    private String string(
            Map<String, Object> source,
            String key,
            String defaultValue) {
        Object value = source.get(key);
        return value == null ? defaultValue : value.toString();
    }

    /**
     * 中文说明：执行 integer 操作；该方法是 {@code GatewayTrafficPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the integer operation; this method is the invocation entry point on {@code GatewayTrafficPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficPolicyCompiler.integer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 integer 的处理结果；returns the result of the operation.
     */
    private int integer(
            Map<String, Object> source,
            String key,
            int defaultValue) {
        return Math.toIntExact(longValue(source, key, defaultValue));
    }

    /**
     * 中文说明：执行 long值 操作；该方法是 {@code GatewayTrafficPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the long value operation; this method is the invocation entry point on {@code GatewayTrafficPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficPolicyCompiler.longValue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 long值 的处理结果；returns the result of the operation.
     */
    private long longValue(
            Map<String, Object> source,
            String key,
            long defaultValue) {
        Object value = source.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * 中文说明：执行 bool 操作；该方法是 {@code GatewayTrafficPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bool operation; this method is the invocation entry point on {@code GatewayTrafficPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficPolicyCompiler.bool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 bool 的处理结果；returns the result of the operation.
     */
    private boolean bool(
            Map<String, Object> source,
            String key,
            boolean defaultValue) {
        Object value = source.get(key);
        return value == null
                ? defaultValue
                : Boolean.parseBoolean(value.toString());
    }
}
