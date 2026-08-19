package top.egon.cola.component.gateway.engine.rule.domain;

import top.egon.cola.component.gateway.engine.rpc.service.RpcMethodIndex;

import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.CompiledHttpRouteIndex;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.engine.http.cors.RuntimeCorsPolicy;
import top.egon.cola.component.gateway.engine.common.provider.domain.RuntimeProviderPolicy;
import top.egon.cola.component.gateway.engine.common.traffic.domain.RuntimeTrafficPolicy;
import top.egon.cola.component.gateway.mcp.rule.domain.CompiledMcpRules;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code CompiledGatewayRules} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Compiled网关Rules相关的职责与边界。
 * English summary: {@code CompiledGatewayRules} is an immutable data carrier in the current Gateway module; it owns the compiled gateway rules-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param snapshot 参数 snapshot；parameter snapshot。
 * @param httpRoutes 参数 httpRoutes；parameter http routes。
 * @param rpcMethods 参数 rpcMethods；parameter rpc methods。
 * @param providerServices 参数 提供方Services；parameter provider services。
 * @param providerPolicies 参数 提供方Policies；parameter provider policies。
 * @param trafficPolicies 参数 流量Policies；parameter traffic policies。
 * @param securityPolicies 参数 安全Policies；parameter security policies。
 * @param corsPolicies 参数 corsPolicies；parameter cors policies。
 * @param mcpRules 参数 MCPRules；parameter mcp rules。
 */
public record CompiledGatewayRules(
        /**
         * 中文说明：保存 snapshot 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleSnapshot}，由 {@code CompiledGatewayRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by snapshot; its type is {@code GatewayRuleSnapshot}, and {@code CompiledGatewayRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayRuleSnapshot snapshot,
        /**
         * 中文说明：保存 httpRoutes 对应的状态、依赖或配置值；字段类型为 {@code CompiledHttpRouteIndex}，由 {@code CompiledGatewayRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by http routes; its type is {@code CompiledHttpRouteIndex}, and {@code CompiledGatewayRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        CompiledHttpRouteIndex httpRoutes,
        /**
         * 中文说明：保存 rpcMethods 对应的状态、依赖或配置值；字段类型为 {@code RpcMethodIndex}，由 {@code CompiledGatewayRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by rpc methods; its type is {@code RpcMethodIndex}, and {@code CompiledGatewayRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        RpcMethodIndex rpcMethods,
        /**
         * 中文说明：保存 提供方Services 对应的状态、依赖或配置值；字段类型为 {@code Set<ProviderServiceKey>}，由 {@code CompiledGatewayRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider services; its type is {@code Set<ProviderServiceKey>}, and {@code CompiledGatewayRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<ProviderServiceKey> providerServices,
        /**
         * 中文说明：保存 提供方Policies 对应的状态、依赖或配置值；字段类型为 {@code Map<String, RuntimeProviderPolicy>}，由 {@code CompiledGatewayRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider policies; its type is {@code Map<String, RuntimeProviderPolicy>}, and {@code CompiledGatewayRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, RuntimeProviderPolicy> providerPolicies,
        /**
         * 中文说明：保存 流量Policies 对应的状态、依赖或配置值；字段类型为 {@code Map<String, RuntimeTrafficPolicy>}，由 {@code CompiledGatewayRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by traffic policies; its type is {@code Map<String, RuntimeTrafficPolicy>}, and {@code CompiledGatewayRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, RuntimeTrafficPolicy> trafficPolicies,
        /**
         * 中文说明：保存 安全Policies 对应的状态、依赖或配置值；字段类型为 {@code Map<String, GatewaySecurityPolicy>}，由 {@code CompiledGatewayRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by security policies; its type is {@code Map<String, GatewaySecurityPolicy>}, and {@code CompiledGatewayRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, GatewaySecurityPolicy> securityPolicies,
        /**
         * 中文说明：保存 corsPolicies 对应的状态、依赖或配置值；字段类型为 {@code Map<String, RuntimeCorsPolicy>}，由 {@code CompiledGatewayRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by cors policies; its type is {@code Map<String, RuntimeCorsPolicy>}, and {@code CompiledGatewayRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, RuntimeCorsPolicy> corsPolicies,
        /**
         * 中文说明：保存 MCPRules 对应的状态、依赖或配置值；字段类型为 {@code CompiledMcpRules}，由 {@code CompiledGatewayRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by mcp rules; its type is {@code CompiledMcpRules}, and {@code CompiledGatewayRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        CompiledMcpRules mcpRules
) {

    /**
     * 中文说明：创建 {@code CompiledGatewayRules} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code CompiledGatewayRules} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param snapshot 参数 snapshot；parameter snapshot。
     * @param httpRoutes 参数 httpRoutes；parameter http routes。
     * @param rpcMethods 参数 rpcMethods；parameter rpc methods。
     * @param providerServices 参数 提供方Services；parameter provider services。
     * @param providerPolicies 参数 提供方Policies；parameter provider policies。
     * @param trafficPolicies 参数 流量Policies；parameter traffic policies。
     * @param securityPolicies 参数 安全Policies；parameter security policies。
     * @param corsPolicies 参数 corsPolicies；parameter cors policies。
     * @param mcpRules 参数 MCPRules；parameter mcp rules。
     */
    public CompiledGatewayRules {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        httpRoutes = Objects.requireNonNull(httpRoutes, "httpRoutes");
        rpcMethods = Objects.requireNonNull(rpcMethods, "rpcMethods");
        providerServices = Set.copyOf(Objects.requireNonNull(
                providerServices,
                "providerServices"
        ));
        providerPolicies = Map.copyOf(Objects.requireNonNull(
                providerPolicies,
                "providerPolicies"
        ));
        trafficPolicies = Map.copyOf(Objects.requireNonNull(
                trafficPolicies,
                "trafficPolicies"
        ));
        securityPolicies = Map.copyOf(Objects.requireNonNull(
                securityPolicies,
                "securityPolicies"
        ));
        corsPolicies = Map.copyOf(Objects.requireNonNull(
                corsPolicies,
                "corsPolicies"
        ));
        mcpRules = Objects.requireNonNull(mcpRules, "mcpRules");
    }

    /**
     * 中文说明：创建 {@code CompiledGatewayRules} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code CompiledGatewayRules} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param snapshot 参数 snapshot；parameter snapshot。
     * @param httpRoutes 参数 httpRoutes；parameter http routes。
     * @param rpcMethods 参数 rpcMethods；parameter rpc methods。
     * @param providerServices 参数 提供方Services；parameter provider services。
     * @param providerPolicies 参数 提供方Policies；parameter provider policies。
     * @param trafficPolicies 参数 流量Policies；parameter traffic policies。
     * @param securityPolicies 参数 安全Policies；parameter security policies。
     * @param corsPolicies 参数 corsPolicies；parameter cors policies。
     */
    public CompiledGatewayRules(
            GatewayRuleSnapshot snapshot,
            CompiledHttpRouteIndex httpRoutes,
            RpcMethodIndex rpcMethods,
            Set<ProviderServiceKey> providerServices,
            Map<String, RuntimeProviderPolicy> providerPolicies,
            Map<String, RuntimeTrafficPolicy> trafficPolicies,
            Map<String, GatewaySecurityPolicy> securityPolicies,
            Map<String, RuntimeCorsPolicy> corsPolicies) {
        this(
                snapshot,
                httpRoutes,
                rpcMethods,
                providerServices,
                providerPolicies,
                trafficPolicies,
                securityPolicies,
                corsPolicies,
                CompiledMcpRules.empty()
        );
    }
}
