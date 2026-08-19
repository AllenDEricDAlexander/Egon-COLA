package top.egon.cola.component.gateway.engine.common.traffic.domain;

import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code RuntimeTrafficPolicy} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责运行时流量策略相关的职责与边界。
 * English summary: {@code RuntimeTrafficPolicy} is an immutable data carrier in the current Gateway module; it owns the runtime traffic policy-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param policyId 参数 策略Id；parameter policy id。
 * @param type 参数 type；parameter type。
 * @param scope 参数 scope；parameter scope。
 * @param enabled 参数 enabled；parameter enabled。
 * @param priority 参数 priority；parameter priority。
 * @param keyExpression 参数 键Expression；parameter key expression。
 * @param failureMode 参数 failureMode；parameter failure mode。
 * @param parameters 参数 parameters；parameter parameters。
 * @param stateEpoch 参数 stateEpoch；parameter state epoch。
 * @param policyVersion 参数 策略Version；parameter policy version。
 */
public record RuntimeTrafficPolicy(
        /**
         * 中文说明：保存 策略Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuntimeTrafficPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy id; its type is {@code String}, and {@code RuntimeTrafficPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeTrafficPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeTrafficPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        String policyId,
        /**
         * 中文说明：保存 type 对应的状态、依赖或配置值；字段类型为 {@code TrafficPolicyType}，由 {@code RuntimeTrafficPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by type; its type is {@code TrafficPolicyType}, and {@code RuntimeTrafficPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeTrafficPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeTrafficPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        TrafficPolicyType type,
        /**
         * 中文说明：保存 scope 对应的状态、依赖或配置值；字段类型为 {@code TrafficPolicyScope}，由 {@code RuntimeTrafficPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by scope; its type is {@code TrafficPolicyScope}, and {@code RuntimeTrafficPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeTrafficPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeTrafficPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        TrafficPolicyScope scope,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RuntimeTrafficPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code RuntimeTrafficPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeTrafficPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeTrafficPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 priority 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code RuntimeTrafficPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by priority; its type is {@code int}, and {@code RuntimeTrafficPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeTrafficPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeTrafficPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        int priority,
        /**
         * 中文说明：保存 键Expression 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuntimeTrafficPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by key expression; its type is {@code String}, and {@code RuntimeTrafficPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeTrafficPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeTrafficPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        String keyExpression,
        /**
         * 中文说明：保存 failureMode 对应的状态、依赖或配置值；字段类型为 {@code RateLimitFailureMode}，由 {@code RuntimeTrafficPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failure mode; its type is {@code RateLimitFailureMode}, and {@code RuntimeTrafficPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeTrafficPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeTrafficPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        RateLimitFailureMode failureMode,
        /**
         * 中文说明：保存 parameters 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code RuntimeTrafficPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by parameters; its type is {@code Map<String, Object>}, and {@code RuntimeTrafficPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeTrafficPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeTrafficPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> parameters,
        /**
         * 中文说明：保存 stateEpoch 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code RuntimeTrafficPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by state epoch; its type is {@code long}, and {@code RuntimeTrafficPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeTrafficPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeTrafficPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        long stateEpoch,
        /**
         * 中文说明：保存 策略Version 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code RuntimeTrafficPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy version; its type is {@code long}, and {@code RuntimeTrafficPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeTrafficPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeTrafficPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        long policyVersion
) {

    /**
     * 中文说明：创建 {@code RuntimeTrafficPolicy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RuntimeTrafficPolicy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param policyId 参数 策略Id；parameter policy id。
     * @param type 参数 type；parameter type。
     * @param scope 参数 scope；parameter scope。
     * @param enabled 参数 enabled；parameter enabled。
     * @param priority 参数 priority；parameter priority。
     * @param keyExpression 参数 键Expression；parameter key expression。
     * @param failureMode 参数 failureMode；parameter failure mode。
     * @param parameters 参数 parameters；parameter parameters。
     * @param stateEpoch 参数 stateEpoch；parameter state epoch。
     * @param policyVersion 参数 策略Version；parameter policy version。
     */
    public RuntimeTrafficPolicy {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        type = Objects.requireNonNull(type, "type");
        scope = Objects.requireNonNull(scope, "scope");
        failureMode = Objects.requireNonNull(failureMode, "failureMode");
        parameters = Map.copyOf(Objects.requireNonNull(
                parameters,
                "parameters"
        ));
        if (stateEpoch < 0 || policyVersion < 1) {
            throw new IllegalArgumentException(
                    "stateEpoch and policyVersion are invalid"
            );
        }
    }
}
