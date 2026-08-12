package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.engine.balance.LoadBalancerType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：{@code GatewayProviderPolicyCompiler} 是编译器，位于当前 Gateway 模块的相关包中，负责网关提供方策略Compiler相关的职责与边界。
 * English summary: {@code GatewayProviderPolicyCompiler} is a gateway provider policy compiler compiler in the current Gateway module; it owns the gateway provider policy compiler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayProviderPolicyCompiler {

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code GatewayProviderPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code GatewayProviderPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProviderPolicyCompiler.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policies 参数 policies；parameter policies。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
    public Map<String, RuntimeProviderPolicy> compile(
            List<GatewayRuntimePolicy> policies) {
        Map<String, RuntimeProviderPolicy> compiled = new LinkedHashMap<>();
        for (GatewayRuntimePolicy policy : policies) {
            RuntimeProviderPolicy runtime = compile(policy);
            if (runtime != null
                    && compiled.putIfAbsent(
                    runtime.policyId(),
                    runtime
            ) != null) {
                throw new IllegalArgumentException(
                        "duplicate provider policy " + runtime.policyId()
                );
            }
        }
        return Map.copyOf(compiled);
    }

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code GatewayProviderPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code GatewayProviderPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProviderPolicyCompiler.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
    private RuntimeProviderPolicy compile(GatewayRuntimePolicy source) {
        if ("LOAD_BALANCE".equals(source.type())) {
            String algorithm = string(
                    source.configuration(),
                    "algorithm",
                    "ROUND_ROBIN"
            );
            return new RuntimeProviderPolicy(
                    source.policyId(),
                    RuntimeProviderPolicy.Type.LOAD_BALANCE,
                    LoadBalancerType.valueOf(algorithm),
                    null
            );
        }
        if (!"PROVIDER_OVERRIDE".equals(source.type())) {
            return null;
        }
        Map<String, Object> config = source.configuration();
        return new RuntimeProviderPolicy(
                source.policyId(),
                RuntimeProviderPolicy.Type.PROVIDER_OVERRIDE,
                null,
                new ProviderSelectionPolicy(
                        bool(config, "serviceEnabled", true),
                        nullableBoolean(config.get("secureRequired")),
                        string(config, "requiredZone", null),
                        string(config, "requiredRegion", null),
                        strings(config.get("requiredTags")),
                        override(map(config.get("serviceOverride"))),
                        instanceOverrides(map(config.get("instanceOverrides")))
                )
        );
    }

    /**
     * 中文说明：执行 instanceOverrides 操作；该方法是 {@code GatewayProviderPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instance overrides operation; this method is the invocation entry point on {@code GatewayProviderPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProviderPolicyCompiler.instanceOverrides(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 instanceOverrides 的处理结果；returns the result of the operation.
     */
    private Map<String, ProviderPolicyOverride> instanceOverrides(
            Map<String, Object> source) {
        Map<String, ProviderPolicyOverride> result = new LinkedHashMap<>();
        source.forEach((instanceId, value) ->
                result.put(instanceId, override(map(value))));
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 override 操作；该方法是 {@code GatewayProviderPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the override operation; this method is the invocation entry point on {@code GatewayProviderPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProviderPolicyCompiler.override(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 override 的处理结果；returns the result of the operation.
     */
    private ProviderPolicyOverride override(Map<String, Object> source) {
        return new ProviderPolicyOverride(
                nullableBoolean(source.get("enabled")),
                nullableInteger(source.get("weight")),
                string(source, "zone", null),
                string(source, "region", null),
                source.containsKey("tags")
                        ? strings(source.get("tags"))
                        : null
        );
    }

    /**
     * 中文说明：执行 map 操作；该方法是 {@code GatewayProviderPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code GatewayProviderPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProviderPolicyCompiler.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> map(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> values)) {
            throw new IllegalArgumentException(
                    "provider override must be an object"
            );
        }
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, item) -> result.put(key.toString(), item));
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 strings 操作；该方法是 {@code GatewayProviderPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the strings operation; this method is the invocation entry point on {@code GatewayProviderPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProviderPolicyCompiler.strings(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 strings 的处理结果；returns the result of the operation.
     */
    private Set<String> strings(Object value) {
        if (value == null) {
            return Set.of();
        }
        if (!(value instanceof Iterable<?> values)) {
            throw new IllegalArgumentException(
                    "provider tags must be an array"
            );
        }
        Set<String> result = new LinkedHashSet<>();
        values.forEach(item -> result.add(item.toString()));
        return Set.copyOf(result);
    }

    /**
     * 中文说明：执行 string 操作；该方法是 {@code GatewayProviderPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string operation; this method is the invocation entry point on {@code GatewayProviderPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProviderPolicyCompiler.string(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 bool 操作；该方法是 {@code GatewayProviderPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bool operation; this method is the invocation entry point on {@code GatewayProviderPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProviderPolicyCompiler.bool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 bool 的处理结果；returns the result of the operation.
     */
    private boolean bool(
            Map<String, Object> source,
            String key,
            boolean defaultValue) {
        Boolean value = nullableBoolean(source.get(key));
        return value == null ? defaultValue : value;
    }

    /**
     * 中文说明：执行 nullableBoolean 操作；该方法是 {@code GatewayProviderPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the nullable boolean operation; this method is the invocation entry point on {@code GatewayProviderPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProviderPolicyCompiler.nullableBoolean(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 nullableBoolean 的处理结果；returns the result of the operation.
     */
    private Boolean nullableBoolean(Object value) {
        return value == null
                ? null
                : value instanceof Boolean bool
                ? bool
                : Boolean.valueOf(value.toString());
    }

    /**
     * 中文说明：执行 nullableInteger 操作；该方法是 {@code GatewayProviderPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the nullable integer operation; this method is the invocation entry point on {@code GatewayProviderPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProviderPolicyCompiler.nullableInteger(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 nullableInteger 的处理结果；returns the result of the operation.
     */
    private Integer nullableInteger(Object value) {
        return value == null
                ? null
                : value instanceof Number number
                ? number.intValue()
                : Integer.valueOf(value.toString());
    }
}
