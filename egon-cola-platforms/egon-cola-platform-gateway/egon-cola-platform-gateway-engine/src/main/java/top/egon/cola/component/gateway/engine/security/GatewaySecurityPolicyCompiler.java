package top.egon.cola.component.gateway.engine.security;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.core.security.AuthenticationMode;
import top.egon.cola.component.gateway.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.gateway.core.security.CredentialForwardingMode;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityFailureMode;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：{@code GatewaySecurityPolicyCompiler} 是编译器，位于当前 Gateway 模块的相关包中，负责网关安全策略Compiler相关的职责与边界。
 * English summary: {@code GatewaySecurityPolicyCompiler} is a gateway security policy compiler compiler in the current Gateway module; it owns the gateway security policy compiler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewaySecurityPolicyCompiler {

    /**
     * 中文说明：保存 capabilities 对应的状态、依赖或配置值；字段类型为 {@code GatewaySecurityCapabilityRegistry}，由 {@code GatewaySecurityPolicyCompiler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by capabilities; its type is {@code GatewaySecurityCapabilityRegistry}, and {@code GatewaySecurityPolicyCompiler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecurityPolicyCompiler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityPolicyCompiler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewaySecurityCapabilityRegistry capabilities;

    /**
     * 中文说明：创建 {@code GatewaySecurityPolicyCompiler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewaySecurityPolicyCompiler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param capabilities 参数 capabilities；parameter capabilities。
     */
    public GatewaySecurityPolicyCompiler(
            GatewaySecurityCapabilityRegistry capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code GatewaySecurityPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code GatewaySecurityPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityPolicyCompiler.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policies 参数 policies；parameter policies。
     * @param policyProtocols 参数 策略Protocols；parameter policy protocols。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
    public Map<String, GatewaySecurityPolicy> compile(
            List<GatewayRuntimePolicy> policies,
            Map<String, Set<GatewayProtocol>> policyProtocols) {
        Map<String, GatewaySecurityPolicy> result = new LinkedHashMap<>();
        policies.stream()
                .filter(policy -> "SECURITY".equals(policy.type()))
                .forEach(source -> {
                    GatewaySecurityPolicy policy = policy(source);
                    Set<GatewayProtocol> protocols = policyProtocols.getOrDefault(
                            policy.policyId(),
                            Set.of()
                    );
                    capabilities.validate(policy, protocols);
                    if (result.putIfAbsent(
                            policy.policyId(),
                            policy
                    ) != null) {
                        throw new IllegalArgumentException(
                                "duplicate security policy "
                                        + policy.policyId()
                        );
                    }
                });
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 策略 操作；该方法是 {@code GatewaySecurityPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the policy operation; this method is the invocation entry point on {@code GatewaySecurityPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityPolicyCompiler.policy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 策略 的处理结果；returns the result of the operation.
     */
    private GatewaySecurityPolicy policy(GatewayRuntimePolicy source) {
        Map<String, Object> config = source.configuration();
        return new GatewaySecurityPolicy(
                source.policyId(),
                AuthenticationMode.valueOf(string(
                        config,
                        "authenticationMode",
                        "NONE"
                )),
                strings(config.get("credentialExtractorIds")),
                strings(config.get("authenticationProviderIds")),
                strings(config.get("authorizationProviderIds")),
                AuthorizationDecisionMode.valueOf(string(
                        config,
                        "decisionMode",
                        "ALL_ALLOW"
                )),
                string(config, "identityMapperId", null),
                Duration.ofMillis(number(
                        config,
                        "providerTimeoutMs",
                        1000
                )),
                SecurityFailureMode.valueOf(string(
                        config,
                        "failureMode",
                        "FAIL_CLOSED"
                )),
                CredentialForwardingMode.valueOf(string(
                        config,
                        "credentialForwardingMode",
                        "NONE"
                ))
        );
    }

    /**
     * 中文说明：执行 strings 操作；该方法是 {@code GatewaySecurityPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the strings operation; this method is the invocation entry point on {@code GatewaySecurityPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityPolicyCompiler.strings(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 strings 的处理结果；returns the result of the operation.
     */
    private List<String> strings(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).toList();
        }
        String text = value.toString();
        return text.isBlank()
                ? List.of()
                : java.util.Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    /**
     * 中文说明：执行 number 操作；该方法是 {@code GatewaySecurityPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the number operation; this method is the invocation entry point on {@code GatewaySecurityPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityPolicyCompiler.number(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param field 参数 field；parameter field。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 number 的处理结果；returns the result of the operation.
     */
    private long number(
            Map<String, Object> source,
            String field,
            long defaultValue) {
        Object value = source.get(field);
        if (value == null) {
            return defaultValue;
        }
        return value instanceof Number number
                ? number.longValue()
                : Long.parseLong(value.toString());
    }

    /**
     * 中文说明：执行 string 操作；该方法是 {@code GatewaySecurityPolicyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string operation; this method is the invocation entry point on {@code GatewaySecurityPolicyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityPolicyCompiler.string(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param field 参数 field；parameter field。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 string 的处理结果；returns the result of the operation.
     */
    private String string(
            Map<String, Object> source,
            String field,
            String defaultValue) {
        Object value = source.get(field);
        return value == null ? defaultValue : value.toString();
    }
}
