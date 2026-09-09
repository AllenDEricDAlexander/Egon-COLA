package top.egon.cola.component.yuheng.engine.rule.service;

import top.egon.cola.component.yuheng.engine.rpc.service.RpcMethodIndex;

import top.egon.cola.component.yuheng.runtime.provider.domain.RuntimeProviderPolicy;
import top.egon.cola.component.yuheng.engine.rule.domain.ApiRpcGatewayCompiledRulesDTO;

import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleContent;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.yuheng.core.provider.ProviderProtocolType;
import top.egon.cola.component.yuheng.core.provider.ProviderServiceKey;
import top.egon.cola.component.yuheng.core.route.GatewayResponseMode;
import top.egon.cola.component.yuheng.core.route.HttpRouteCompiler;
import top.egon.cola.component.yuheng.core.route.RuntimeHttpRoute;
import top.egon.cola.component.yuheng.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.yuheng.core.transport.GatewayRouteProfileResolver;
import top.egon.cola.component.yuheng.core.transport.GatewayTransportDefaults;
import top.egon.cola.component.yuheng.core.transport.GatewayTransportPolicyOverrides;
import top.egon.cola.component.yuheng.core.transport.GatewayTransportSafetyLimits;
import top.egon.cola.component.yuheng.engine.rpc.service.RpcMethodIndexCompiler;
import top.egon.cola.component.yuheng.engine.rpc.domain.RuntimeRpcRoute;
import top.egon.cola.component.yuheng.runtime.provider.service.GatewayProviderPolicyCompiler;
import top.egon.cola.component.yuheng.engine.http.cors.GatewayCorsPolicyCompiler;
import top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityPolicyCompiler;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayTrafficPolicyCompiler;
import top.egon.cola.component.yuheng.runtime.traffic.domain.RuntimeTrafficPolicy;
import top.egon.cola.component.yuheng.runtime.traffic.domain.TrafficPolicyType;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleCompilerStrategy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;

/**
 * 中文说明：{@code ApiRpcGatewayRuleCompilerStrategy} 是编译器，位于当前 Gateway 模块的相关包中，负责引擎网关规则Compiler相关的职责与边界。
 * English summary: {@code ApiRpcGatewayRuleCompilerStrategy} is a engine gateway rule compiler compiler in the current Gateway module; it owns the engine gateway rule compiler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Slf4j
@RequiredArgsConstructor
public final class ApiRpcGatewayRuleCompilerStrategy implements GatewayRuleCompilerStrategy<ApiRpcGatewayCompiledRulesDTO> {

    /**
     * 中文说明：保存 httpCompiler 对应的状态、依赖或配置值；字段类型为 {@code HttpRouteCompiler}，由 {@code ApiRpcGatewayRuleCompilerStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by http compiler; its type is {@code HttpRouteCompiler}, and {@code ApiRpcGatewayRuleCompilerStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ApiRpcGatewayRuleCompilerStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ApiRpcGatewayRuleCompilerStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final HttpRouteCompiler httpCompiler = new HttpRouteCompiler();

    /**
     * 中文说明：保存 rpcCompiler 对应的状态、依赖或配置值；字段类型为 {@code RpcMethodIndexCompiler}，由 {@code ApiRpcGatewayRuleCompilerStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rpc compiler; its type is {@code RpcMethodIndexCompiler}, and {@code ApiRpcGatewayRuleCompilerStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ApiRpcGatewayRuleCompilerStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ApiRpcGatewayRuleCompilerStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RpcMethodIndexCompiler rpcCompiler =
            new RpcMethodIndexCompiler();

    /**
     * 中文说明：保存 流量策略Compiler 对应的状态、依赖或配置值；字段类型为 {@code GatewayTrafficPolicyCompiler}，由 {@code ApiRpcGatewayRuleCompilerStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by traffic policy compiler; its type is {@code GatewayTrafficPolicyCompiler}, and {@code ApiRpcGatewayRuleCompilerStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ApiRpcGatewayRuleCompilerStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ApiRpcGatewayRuleCompilerStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTrafficPolicyCompiler trafficPolicyCompiler =
            new GatewayTrafficPolicyCompiler();

    /**
     * 中文说明：保存 提供方策略Compiler 对应的状态、依赖或配置值；字段类型为 {@code GatewayProviderPolicyCompiler}，由 {@code ApiRpcGatewayRuleCompilerStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by provider policy compiler; its type is {@code GatewayProviderPolicyCompiler}, and {@code ApiRpcGatewayRuleCompilerStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ApiRpcGatewayRuleCompilerStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ApiRpcGatewayRuleCompilerStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayProviderPolicyCompiler providerPolicyCompiler =
            new GatewayProviderPolicyCompiler();

    /**
     * 中文说明：保存 cors策略Compiler 对应的状态、依赖或配置值；字段类型为 {@code GatewayCorsPolicyCompiler}，由 {@code ApiRpcGatewayRuleCompilerStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by cors policy compiler; its type is {@code GatewayCorsPolicyCompiler}, and {@code ApiRpcGatewayRuleCompilerStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ApiRpcGatewayRuleCompilerStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ApiRpcGatewayRuleCompilerStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCorsPolicyCompiler corsPolicyCompiler =
            new GatewayCorsPolicyCompiler();

    /**
     * 中文说明：保存 安全策略Compiler 对应的状态、依赖或配置值；字段类型为 {@code GatewaySecurityPolicyCompiler}，由 {@code ApiRpcGatewayRuleCompilerStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security policy compiler; its type is {@code GatewaySecurityPolicyCompiler}, and {@code ApiRpcGatewayRuleCompilerStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ApiRpcGatewayRuleCompilerStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ApiRpcGatewayRuleCompilerStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    @NonNull
    @Qualifier("gatewaySecurityPolicyCompiler")
    private final GatewaySecurityPolicyCompiler securityPolicyCompiler;

    /**
     * 中文说明：保存 传输Resolver 对应的状态、依赖或配置值；字段类型为 {@code GatewayRouteProfileResolver}，由 {@code ApiRpcGatewayRuleCompilerStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transport resolver; its type is {@code GatewayRouteProfileResolver}, and {@code ApiRpcGatewayRuleCompilerStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ApiRpcGatewayRuleCompilerStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ApiRpcGatewayRuleCompilerStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRouteProfileResolver transportResolver =
            new GatewayRouteProfileResolver();

    /**
     * 中文说明：保存 传输Defaults 对应的状态、依赖或配置值；字段类型为 {@code GatewayTransportDefaults}，由 {@code ApiRpcGatewayRuleCompilerStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transport defaults; its type is {@code GatewayTransportDefaults}, and {@code ApiRpcGatewayRuleCompilerStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ApiRpcGatewayRuleCompilerStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ApiRpcGatewayRuleCompilerStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    @NonNull
    @Qualifier("gatewayTransportDefaults")
    private final GatewayTransportDefaults transportDefaults;

    /**
     * 中文说明：保存 传输SafetyLimits 对应的状态、依赖或配置值；字段类型为 {@code GatewayTransportSafetyLimits}，由 {@code ApiRpcGatewayRuleCompilerStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transport safety limits; its type is {@code GatewayTransportSafetyLimits}, and {@code ApiRpcGatewayRuleCompilerStrategy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ApiRpcGatewayRuleCompilerStrategy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ApiRpcGatewayRuleCompilerStrategy}; do not couple callers to its representation when the owning type exposes an API.
     */
    @NonNull
    @Qualifier("gatewayTransportSafetyLimits")
    private final GatewayTransportSafetyLimits transportSafetyLimits;


    /**
     * 中文说明：创建 {@code ApiRpcGatewayRuleCompilerStrategy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ApiRpcGatewayRuleCompilerStrategy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    public ApiRpcGatewayRuleCompilerStrategy() {
        this(
                GatewaySecurityCapabilityRegistry.empty(),
                GatewayTransportDefaults.legacy(),
                GatewayTransportSafetyLimits.specDefaults()
        );
    }

    /**
     * 中文说明：创建 {@code ApiRpcGatewayRuleCompilerStrategy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ApiRpcGatewayRuleCompilerStrategy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param capabilities 参数 capabilities；parameter capabilities。
     */
    public ApiRpcGatewayRuleCompilerStrategy(
            GatewaySecurityCapabilityRegistry capabilities) {
        this(
                capabilities,
                GatewayTransportDefaults.legacy(),
                GatewayTransportSafetyLimits.specDefaults()
        );
    }

    /**
     * 中文说明：创建 {@code ApiRpcGatewayRuleCompilerStrategy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ApiRpcGatewayRuleCompilerStrategy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param transportDefaults 参数 传输Defaults；parameter transport defaults。
     * @param transportSafetyLimits 参数 传输SafetyLimits；parameter transport safety limits。
     */
    public ApiRpcGatewayRuleCompilerStrategy(
            GatewaySecurityCapabilityRegistry capabilities,
            GatewayTransportDefaults transportDefaults,
            GatewayTransportSafetyLimits transportSafetyLimits) {
        this(
                new GatewaySecurityPolicyCompiler(Objects.requireNonNull(capabilities, "capabilities")),
                Objects.requireNonNull(transportDefaults, "transportDefaults"),
                Objects.requireNonNull(transportSafetyLimits, "transportSafetyLimits")
        );
    }

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code ApiRpcGatewayRuleCompilerStrategy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code ApiRpcGatewayRuleCompilerStrategy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ApiRpcGatewayRuleCompilerStrategy.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param snapshot 参数 snapshot；parameter snapshot。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
    @Override
    public ApiRpcGatewayCompiledRulesDTO compile(GatewayRuleSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        GatewayRuleContent content = snapshot.content();
        List<GatewayRuntimePolicy> runtimePolicies = new ArrayList<>();
        runtimePolicies.addAll(content.providerPolicies());
        runtimePolicies.addAll(content.trafficPolicies());
        runtimePolicies.addAll(content.securityPolicies());
        runtimePolicies.addAll(content.corsPolicies());
        Map<String, RuntimeTrafficPolicy> trafficPolicies =
                trafficPolicyCompiler.compile(runtimePolicies);
        Map<String, GatewayRuntimeOperation> operations = content.operations()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        GatewayRuntimeOperation::operationId,
                        Function.identity()
                ));
        List<RuntimeHttpRoute> httpRoutes = content.routes().stream()
                .filter(route -> route.enabled())
                .map(route -> {
                    GatewayRuntimeOperation operation = operations.get(
                            route.operationId()
                    );
                    if (operation == null) {
                        throw new IllegalArgumentException(
                                "GATEWAY_RULE_COMPILE_FAILED: unknown operation"
                        );
                    }
                    return new RuntimeHttpRoute(
                            route.routeId(),
                            operation.operationId(),
                            content.gatewayGroupId(),
                            route.accessZones(),
                            route.host(),
                            Set.of(route.httpMethod()),
                            route.pathPattern(),
                            operation.externalAccessible(),
                            serviceKey(operation.providerService()),
                            operation.policyRefs(),
                            route.priority(),
                            GatewayResponseMode.valueOf(
                                    operation.responseMode()
                            ),
                            routeMetadata(
                                    operation,
                                    snapshot.releaseId()
                            ),
                            transportResolver.resolve(
                                    route.transportPolicy(),
                                    transportDefaults,
                                    transportOverrides(
                                            operation.policyRefs(),
                                            trafficPolicies
                                    ),
                                    transportSafetyLimits
                            )
                    );
                })
                .toList();
        List<RuntimeRpcRoute> rpcRoutes = content.operations().stream()
                .filter(operation -> operation.protocol()
                        == GatewayProtocol.RPC)
                .filter(operation -> !operation.deprecated())
                .map(this::rpcRoute)
                .toList();
        Set<ProviderServiceKey> services = new LinkedHashSet<>();
        content.operations().stream()
                .filter(operation -> !operation.deprecated())
                .map(GatewayRuntimeOperation::providerService)
                .map(this::serviceKey)
                .forEach(services::add);
        Map<String, Set<GatewayProtocol>> policyProtocols =
                new LinkedHashMap<>();
        content.operations().stream()
                .filter(operation -> !operation.deprecated())
                .forEach(operation -> operation.policyRefs().forEach(
                        policyId -> policyProtocols.computeIfAbsent(
                                policyId,
                                ignored -> new LinkedHashSet<>()
                        ).add(operation.protocol())
                ));
        Map<String, GatewaySecurityPolicy> securityPolicies =
                securityPolicyCompiler.compile(
                        runtimePolicies,
                        policyProtocols
                );
        var providerPolicies = providerPolicyCompiler.compile(
                content.providerPolicies()
        );
        var corsPolicies = corsPolicyCompiler.compile(
                content.corsPolicies()
        );
        content.operations().stream()
                .filter(operation -> !operation.deprecated())
                .forEach(operation -> {
                    long securityReferences = operation.policyRefs().stream()
                            .filter(securityPolicies::containsKey)
                            .count();
                    if (securityReferences > 1) {
                        throw new IllegalArgumentException(
                                "GATEWAY_RULE_COMPILE_FAILED: operation "
                                        + operation.operationId()
                                        + " references multiple security "
                                        + "policies"
                        );
                    }
                    long loadBalanceReferences = operation.policyRefs()
                            .stream()
                            .map(providerPolicies::get)
                            .filter(java.util.Objects::nonNull)
                            .filter(policy -> policy.type()
                                    == RuntimeProviderPolicy.Type.LOAD_BALANCE)
                            .count();
                    if (loadBalanceReferences > 1) {
                        throw new IllegalArgumentException(
                                "GATEWAY_RULE_COMPILE_FAILED: operation "
                                        + operation.operationId()
                                        + " references multiple load balance "
                                        + "policies"
                        );
                    }
                    long corsReferences = operation.policyRefs().stream()
                            .filter(corsPolicies::containsKey)
                            .count();
                    if (corsReferences > 1) {
                        throw new IllegalArgumentException(
                                "GATEWAY_RULE_COMPILE_FAILED: operation "
                                        + operation.operationId()
                                        + " references multiple CORS policies"
                        );
                    }
                });
        return new ApiRpcGatewayCompiledRulesDTO(
                snapshot,
                httpCompiler.compile(httpRoutes),
                rpcCompiler.compile(rpcRoutes),
                services,
                providerPolicies,
                trafficPolicies,
                securityPolicies,
                corsPolicies
        );
    }

    /**
     * 中文说明：执行 传输Overrides 操作；该方法是 {@code ApiRpcGatewayRuleCompilerStrategy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transport overrides operation; this method is the invocation entry point on {@code ApiRpcGatewayRuleCompilerStrategy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ApiRpcGatewayRuleCompilerStrategy.transportOverrides(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @param policies 参数 policies；parameter policies。
     * @return 返回 传输Overrides 的处理结果；returns the result of the operation.
     */
    private GatewayTransportPolicyOverrides transportOverrides(
            Set<String> policyRefs,
            Map<String, RuntimeTrafficPolicy> policies) {
        OptionalLong requestLimit = OptionalLong.empty();
        OptionalLong responseLimit = OptionalLong.empty();
        Optional<Duration> totalTimeout = Optional.empty();
        for (String policyRef : policyRefs) {
            RuntimeTrafficPolicy policy = policies.get(policyRef);
            if (policy == null || !policy.enabled()) {
                continue;
            }
            if (policy.type() == TrafficPolicyType.REQUEST_SIZE) {
                requestLimit = minimum(
                        requestLimit,
                        longValue(policy.parameters().get("maxBytes"))
                );
            } else if (policy.type() == TrafficPolicyType.RESPONSE_SIZE) {
                responseLimit = minimum(
                        responseLimit,
                        longValue(policy.parameters().get("maxBytes"))
                );
            } else if (policy.type() == TrafficPolicyType.TIMEOUT) {
                Duration configured = timeout(policy.parameters());
                if (configured != null
                        && (totalTimeout.isEmpty()
                        || configured.compareTo(totalTimeout.orElseThrow())
                        < 0)) {
                    totalTimeout = Optional.of(configured);
                }
            }
        }
        return new GatewayTransportPolicyOverrides(
                requestLimit,
                responseLimit,
                totalTimeout
        );
    }

    /**
     * 中文说明：执行 minimum 操作；该方法是 {@code ApiRpcGatewayRuleCompilerStrategy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the minimum operation; this method is the invocation entry point on {@code ApiRpcGatewayRuleCompilerStrategy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ApiRpcGatewayRuleCompilerStrategy.minimum(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param current 参数 current；parameter current。
     * @param candidate 参数 candidate；parameter candidate。
     * @return 返回 minimum 的处理结果；returns the result of the operation.
     */
    private OptionalLong minimum(OptionalLong current, long candidate) {
        return current.isEmpty()
                ? OptionalLong.of(candidate)
                : OptionalLong.of(Math.min(current.getAsLong(), candidate));
    }

    /**
     * 中文说明：执行 超时 操作；该方法是 {@code ApiRpcGatewayRuleCompilerStrategy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the timeout operation; this method is the invocation entry point on {@code ApiRpcGatewayRuleCompilerStrategy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ApiRpcGatewayRuleCompilerStrategy.timeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param parameters 参数 parameters；parameter parameters。
     * @return 返回 超时 的处理结果；returns the result of the operation.
     */
    private Duration timeout(Map<String, Object> parameters) {
        Object value = parameters.get("timeout");
        if (value == null) {
            value = parameters.get("timeoutMillis");
        }
        if (value == null) {
            return null;
        }
        if (value instanceof Duration duration) {
            return duration;
        }
        if (value instanceof Number number) {
            return Duration.ofMillis(number.longValue());
        }
        String text = value.toString();
        return text.startsWith("P")
                ? Duration.parse(text)
                : Duration.ofMillis(Long.parseLong(text));
    }

    /**
     * 中文说明：执行 long值 操作；该方法是 {@code ApiRpcGatewayRuleCompilerStrategy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the long value operation; this method is the invocation entry point on {@code ApiRpcGatewayRuleCompilerStrategy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ApiRpcGatewayRuleCompilerStrategy.longValue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 long值 的处理结果；returns the result of the operation.
     */
    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(Objects.requireNonNull(value).toString());
    }

    /**
     * 中文说明：执行 rpc路由 操作；该方法是 {@code ApiRpcGatewayRuleCompilerStrategy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rpc route operation; this method is the invocation entry point on {@code ApiRpcGatewayRuleCompilerStrategy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ApiRpcGatewayRuleCompilerStrategy.rpcRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @return 返回 rpc路由 的处理结果；returns the result of the operation.
     */
    private RuntimeRpcRoute rpcRoute(GatewayRuntimeOperation operation) {
        String descriptorSha = operation.attributes().get(
                "descriptorSha256"
        );
        if (descriptorSha == null || descriptorSha.isBlank()) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_COMPILE_FAILED: RPC descriptor is missing"
            );
        }
        long timeoutMillis = Long.parseLong(
                operation.attributes().getOrDefault("timeoutMillis", "3000")
        );
        return new RuntimeRpcRoute(
                operation.operationId(),
                operation.operationId(),
                operation.methodIdentity(),
                serviceKey(operation.providerService()),
                operation.requestSchema(),
                operation.responseSchema(),
                descriptorSha,
                operation.policyRefs(),
                GatewayResponseMode.valueOf(operation.responseMode()),
                Boolean.parseBoolean(operation.attributes().getOrDefault(
                        "idempotent",
                        "false"
                )),
                Duration.ofMillis(timeoutMillis)
        );
    }

    /**
     * 中文说明：执行 路由元数据 操作；该方法是 {@code ApiRpcGatewayRuleCompilerStrategy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the route metadata operation; this method is the invocation entry point on {@code ApiRpcGatewayRuleCompilerStrategy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ApiRpcGatewayRuleCompilerStrategy.routeMetadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 路由元数据 的处理结果；returns the result of the operation.
     */
    private Map<String, String> routeMetadata(
            GatewayRuntimeOperation operation,
            String releaseId) {
        Map<String, String> metadata = new LinkedHashMap<>(
                operation.attributes()
        );
        metadata.put("releaseId", releaseId);
        metadata.put("protocol", operation.protocol().name());
        metadata.put("methodIdentity", operation.methodIdentity());
        if (operation.requestSchema() != null) {
            metadata.put("requestSchema", operation.requestSchema());
        }
        if (operation.responseSchema() != null) {
            metadata.put("responseSchema", operation.responseSchema());
        }
        return Map.copyOf(metadata);
    }

    /**
     * 中文说明：执行 服务键 操作；该方法是 {@code ApiRpcGatewayRuleCompilerStrategy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the service key operation; this method is the invocation entry point on {@code ApiRpcGatewayRuleCompilerStrategy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ApiRpcGatewayRuleCompilerStrategy.serviceKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param service 参数 服务；parameter service。
     * @return 返回 服务键 的处理结果；returns the result of the operation.
     */
    private ProviderServiceKey serviceKey(
            GatewayProviderServiceRef service) {
        return new ProviderServiceKey(
                service.bizCode(),
                service.appCode(),
                service.env(),
                service.namespace(),
                service.protocol() == GatewayProtocol.HTTP
                        ? ProviderProtocolType.HTTP
                        : ProviderProtocolType.RPC,
                service.serviceName(),
                service.group(),
                service.version(),
                service.transport()
        );
    }

}
