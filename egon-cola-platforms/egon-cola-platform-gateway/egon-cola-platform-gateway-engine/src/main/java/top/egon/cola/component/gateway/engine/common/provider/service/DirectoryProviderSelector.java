package top.egon.cola.component.gateway.engine.common.provider.service;

import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderCandidateFilterResult;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderSelectionPolicy;
import top.egon.cola.component.gateway.engine.common.provider.domain.RuntimeProviderPolicy;

import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.common.provider.service.ProviderLoadBalancer;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.common.provider.domain.LoadBalancerType;
import top.egon.cola.component.gateway.engine.common.provider.service.ProviderLoadBalancers;
import top.egon.cola.component.gateway.engine.common.provider.service.ProviderSelector;

import java.time.Clock;
import java.util.Objects;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 中文说明：{@code DirectoryProviderSelector} 是类型，位于当前 Gateway 模块的相关包中，负责Directory提供方Selector相关的职责与边界。
 * English summary: {@code DirectoryProviderSelector} is a type in the current Gateway module; it owns the directory provider selector-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class DirectoryProviderSelector implements ProviderSelector {

    /**
     * 中文说明：保存 directory 对应的状态、依赖或配置值；字段类型为 {@code ProviderDirectory}，由 {@code DirectoryProviderSelector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by directory; its type is {@code ProviderDirectory}, and {@code DirectoryProviderSelector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DirectoryProviderSelector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DirectoryProviderSelector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderDirectory directory;

    /**
     * 中文说明：保存 loadBalancers 对应的状态、依赖或配置值；字段类型为 {@code Map<LoadBalancerType, ProviderLoadBalancer>}，由 {@code DirectoryProviderSelector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by load balancers; its type is {@code Map<LoadBalancerType, ProviderLoadBalancer>}, and {@code DirectoryProviderSelector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DirectoryProviderSelector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DirectoryProviderSelector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<LoadBalancerType, ProviderLoadBalancer> loadBalancers;

    /**
     * 中文说明：保存 candidate过滤器 对应的状态、依赖或配置值；字段类型为 {@code ProviderCandidateFilter}，由 {@code DirectoryProviderSelector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by candidate filter; its type is {@code ProviderCandidateFilter}, and {@code DirectoryProviderSelector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DirectoryProviderSelector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DirectoryProviderSelector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderCandidateFilter candidateFilter;

    /**
     * 中文说明：保存 policies 对应的状态、依赖或配置值；字段类型为 {@code Function<ProviderServiceKey, ProviderSelectionPolicy>}，由 {@code DirectoryProviderSelector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by policies; its type is {@code Function<ProviderServiceKey, ProviderSelectionPolicy>}, and {@code DirectoryProviderSelector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DirectoryProviderSelector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DirectoryProviderSelector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Function<ProviderServiceKey, ProviderSelectionPolicy> policies;

    /**
     * 中文说明：保存 运行时Policies 对应的状态、依赖或配置值；字段类型为 {@code Supplier<Map<String, RuntimeProviderPolicy>>}，由 {@code DirectoryProviderSelector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by runtime policies; its type is {@code Supplier<Map<String, RuntimeProviderPolicy>>}, and {@code DirectoryProviderSelector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DirectoryProviderSelector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DirectoryProviderSelector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<Map<String, RuntimeProviderPolicy>> runtimePolicies;

    /**
     * 中文说明：创建 {@code DirectoryProviderSelector} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DirectoryProviderSelector} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param directory 参数 directory；parameter directory。
     * @param loadBalancer 参数 loadBalancer；parameter load balancer。
     */
    public DirectoryProviderSelector(
            ProviderDirectory directory,
            ProviderLoadBalancer loadBalancer) {
        this(
                directory,
                Map.of(LoadBalancerType.ROUND_ROBIN, loadBalancer),
                new ProviderCandidateFilter(Clock.systemUTC(), ignored -> true),
                key -> ProviderSelectionPolicy.defaults(
                        key.transport().equals("https")
                ),
                Map::of
        );
    }

    /**
     * 中文说明：创建 {@code DirectoryProviderSelector} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DirectoryProviderSelector} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param directory 参数 directory；parameter directory。
     * @param loadBalancers 参数 loadBalancers；parameter load balancers。
     * @param candidateFilter 参数 candidate过滤器；parameter candidate filter。
     * @param policies 参数 policies；parameter policies。
     * @param runtimePolicies 参数 运行时Policies；parameter runtime policies。
     */
    public DirectoryProviderSelector(
            ProviderDirectory directory,
            Map<LoadBalancerType, ProviderLoadBalancer> loadBalancers,
            ProviderCandidateFilter candidateFilter,
            Function<ProviderServiceKey, ProviderSelectionPolicy> policies,
            Supplier<Map<String, RuntimeProviderPolicy>> runtimePolicies) {
        this.directory = Objects.requireNonNull(directory, "directory");
        EnumMap<LoadBalancerType, ProviderLoadBalancer> copy =
                new EnumMap<>(LoadBalancerType.class);
        copy.putAll(Objects.requireNonNull(loadBalancers, "loadBalancers"));
        this.loadBalancers = Map.copyOf(copy);
        this.candidateFilter = Objects.requireNonNull(
                candidateFilter,
                "candidateFilter"
        );
        this.policies = Objects.requireNonNull(policies, "policies");
        this.runtimePolicies = Objects.requireNonNull(
                runtimePolicies,
                "runtimePolicies"
        );
    }

    /**
     * 中文说明：执行 select 操作；该方法是 {@code DirectoryProviderSelector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the select operation; this method is the invocation entry point on {@code DirectoryProviderSelector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DirectoryProviderSelector.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKey 参数 服务键；parameter service key。
     * @return 返回 select 的处理结果；returns the result of the operation.
     */
    @Override
    public ProviderSelectionHandle select(ProviderServiceKey serviceKey) {
        return select(serviceKey, Set.of());
    }

    /**
     * 中文说明：执行 select 操作；该方法是 {@code DirectoryProviderSelector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the select operation; this method is the invocation entry point on {@code DirectoryProviderSelector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DirectoryProviderSelector.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKey 参数 服务键；parameter service key。
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @return 返回 select 的处理结果；returns the result of the operation.
     */
    @Override
    public ProviderSelectionHandle select(
            ProviderServiceKey serviceKey,
            Set<String> policyRefs) {
        return select(serviceKey, policyRefs, Set.of());
    }

    /**
     * 中文说明：执行 select 操作；该方法是 {@code DirectoryProviderSelector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the select operation; this method is the invocation entry point on {@code DirectoryProviderSelector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DirectoryProviderSelector.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKey 参数 服务键；parameter service key。
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @param excludedRuntimeIdentities 参数 excluded运行时Identities；parameter excluded runtime identities。
     * @return 返回 select 的处理结果；returns the result of the operation.
     */
    @Override
    public ProviderSelectionHandle select(
            ProviderServiceKey serviceKey,
            Set<String> policyRefs,
            Set<String> excludedRuntimeIdentities) {
        ResolvedPolicies resolved = resolve(serviceKey, policyRefs);
        ProviderCandidateFilterResult result = candidateFilter.filter(
                serviceKey,
                directory.instances(serviceKey),
                resolved.selectionPolicy()
        );
        var preferred = result.candidates().stream()
                .filter(instance -> !excludedRuntimeIdentities.contains(
                        instance.runtimeIdentity()
                ))
                .toList();
        var candidates = preferred.isEmpty()
                ? result.candidates()
                : preferred;
        ProviderLoadBalancer loadBalancer = loadBalancers.get(
                resolved.loadBalancer()
        );
        if (loadBalancer == null) {
            throw new IllegalStateException(
                    "GATEWAY_LOAD_BALANCER_UNAVAILABLE: "
                            + resolved.loadBalancer()
            );
        }
        return loadBalancer.select(serviceKey, candidates);
    }

    /**
     * 中文说明：执行 resolve 操作；该方法是 {@code DirectoryProviderSelector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve operation; this method is the invocation entry point on {@code DirectoryProviderSelector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DirectoryProviderSelector.resolve(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKey 参数 服务键；parameter service key。
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @return 返回 resolve 的处理结果；returns the result of the operation.
     */
    private ResolvedPolicies resolve(
            ProviderServiceKey serviceKey,
            Set<String> policyRefs) {
        LoadBalancerType loadBalancer = LoadBalancerType.ROUND_ROBIN;
        ProviderSelectionPolicy selection = policies.apply(serviceKey);
        for (String policyRef : policyRefs) {
            RuntimeProviderPolicy policy = runtimePolicies.get().get(
                    policyRef
            );
            if (policy == null) {
                continue;
            }
            if (policy.type() == RuntimeProviderPolicy.Type.LOAD_BALANCE) {
                loadBalancer = policy.loadBalancer();
            } else {
                selection = policy.selectionPolicy();
            }
        }
        return new ResolvedPolicies(loadBalancer, selection);
    }

    /**
     * 中文说明：执行 defaultLoadBalancers 操作；该方法是 {@code DirectoryProviderSelector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the default load balancers operation; this method is the invocation entry point on {@code DirectoryProviderSelector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DirectoryProviderSelector.defaultLoadBalancers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 defaultLoadBalancers 的处理结果；returns the result of the operation.
     */
    public static Map<LoadBalancerType, ProviderLoadBalancer>
            defaultLoadBalancers() {
        EnumMap<LoadBalancerType, ProviderLoadBalancer> result =
                new EnumMap<>(LoadBalancerType.class);
        for (LoadBalancerType type : LoadBalancerType.values()) {
            result.put(type, ProviderLoadBalancers.create(type));
        }
        return Map.copyOf(result);
    }

    /**
     * 中文说明：{@code ResolvedPolicies} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责ResolvedPolicies相关的职责与边界。
     * English summary: {@code ResolvedPolicies} is an immutable data carrier in the current Gateway module; it owns the resolved policies-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param loadBalancer 参数 loadBalancer；parameter load balancer。
     * @param selectionPolicy 参数 selection策略；parameter selection policy。
     */
    private record ResolvedPolicies(
            /**
             * 中文说明：保存 loadBalancer 对应的状态、依赖或配置值；字段类型为 {@code LoadBalancerType}，由 {@code DirectoryProviderSelector.ResolvedPolicies} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by load balancer; its type is {@code LoadBalancerType}, and {@code DirectoryProviderSelector.ResolvedPolicies} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code DirectoryProviderSelector.ResolvedPolicies} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DirectoryProviderSelector.ResolvedPolicies}; do not couple callers to its representation when the owning type exposes an API.
             */
            LoadBalancerType loadBalancer,
            /**
             * 中文说明：保存 selection策略 对应的状态、依赖或配置值；字段类型为 {@code ProviderSelectionPolicy}，由 {@code DirectoryProviderSelector.ResolvedPolicies} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by selection policy; its type is {@code ProviderSelectionPolicy}, and {@code DirectoryProviderSelector.ResolvedPolicies} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code DirectoryProviderSelector.ResolvedPolicies} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DirectoryProviderSelector.ResolvedPolicies}; do not couple callers to its representation when the owning type exposes an API.
             */
            ProviderSelectionPolicy selectionPolicy
    ) {
    }
}
