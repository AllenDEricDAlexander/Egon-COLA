package top.egon.cola.component.gateway.engine.http.cors;

import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：{@code GatewayCorsPolicyCompiler} 是编译器，位于当前 Gateway 模块的相关包中，负责网关Cors策略Compiler相关的职责与边界。
 * English summary: {@code GatewayCorsPolicyCompiler} is a gateway cors policy compiler compiler in the current Gateway module; it owns the gateway cors policy compiler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCorsPolicyCompiler {

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code GatewayCorsPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code GatewayCorsPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsPolicyCompiler.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policies 参数 policies；parameter policies。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
    public Map<String, RuntimeCorsPolicy> compile(
            List<GatewayRuntimePolicy> policies) {
        Map<String, RuntimeCorsPolicy> result = new LinkedHashMap<>();
        policies.stream()
                .filter(policy -> "CORS".equals(policy.type()))
                .forEach(source -> {
                    Map<String, Object> config = source.configuration();
                    RuntimeCorsPolicy policy = new RuntimeCorsPolicy(
                            source.policyId(),
                            values(config, "allowedOrigins", false),
                            values(config, "allowedMethods", true),
                            values(config, "allowedHeaders", false),
                            values(config, "exposedHeaders", false),
                            bool(config, "allowCredentials", false),
                            number(config, "maxAgeSeconds", 0),
                            bool(config, "enabled", true)
                    );
                    if (result.putIfAbsent(
                            policy.policyId(),
                            policy
                    ) != null) {
                        throw new IllegalArgumentException(
                                "duplicate CORS policy " + policy.policyId()
                        );
                    }
                });
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 values 操作；该方法是 {@code GatewayCorsPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the values operation; this method is the invocation entry point on {@code GatewayCorsPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsPolicyCompiler.values(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param upperCase 参数 upperCase；parameter upper case。
     * @return 返回 values 的处理结果；returns the result of the operation.
     */
    private Set<String> values(
            Map<String, Object> source,
            String key,
            boolean upperCase) {
        Object raw = source.get(key);
        if (raw == null) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (raw instanceof Collection<?> collection) {
            collection.forEach(value -> add(
                    result,
                    value.toString(),
                    upperCase
            ));
        } else {
            for (String value : raw.toString().split(",")) {
                add(result, value, upperCase);
            }
        }
        return Set.copyOf(result);
    }

    /**
     * 中文说明：执行 add 操作；该方法是 {@code GatewayCorsPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the add operation; this method is the invocation entry point on {@code GatewayCorsPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsPolicyCompiler.add(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param value 参数 值；parameter value。
     * @param upperCase 参数 upperCase；parameter upper case。
     */
    private void add(
            Set<String> values,
            String value,
            boolean upperCase) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "CORS list must not contain blank values"
            );
        }
        values.add(upperCase
                ? normalized.toUpperCase(Locale.ROOT)
                : normalized);
    }

    /**
     * 中文说明：执行 bool 操作；该方法是 {@code GatewayCorsPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bool operation; this method is the invocation entry point on {@code GatewayCorsPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsPolicyCompiler.bool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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

    /**
     * 中文说明：执行 number 操作；该方法是 {@code GatewayCorsPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the number operation; this method is the invocation entry point on {@code GatewayCorsPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCorsPolicyCompiler.number(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 number 的处理结果；returns the result of the operation.
     */
    private long number(
            Map<String, Object> source,
            String key,
            long defaultValue) {
        Object value = source.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value instanceof Number number
                ? number.longValue()
                : Long.parseLong(value.toString());
    }
}
