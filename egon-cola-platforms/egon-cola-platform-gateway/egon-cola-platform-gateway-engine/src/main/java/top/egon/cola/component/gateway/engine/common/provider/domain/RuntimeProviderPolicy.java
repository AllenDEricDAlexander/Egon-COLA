package top.egon.cola.component.gateway.engine.common.provider.domain;

import top.egon.cola.component.gateway.engine.common.provider.domain.LoadBalancerType;

import java.util.Objects;

/**
 * 中文说明：{@code RuntimeProviderPolicy} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责运行时提供方策略相关的职责与边界。
 * English summary: {@code RuntimeProviderPolicy} is an immutable data carrier in the current Gateway module; it owns the runtime provider policy-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param policyId 参数 策略Id；parameter policy id。
 * @param type 参数 type；parameter type。
 * @param loadBalancer 参数 loadBalancer；parameter load balancer。
 * @param selectionPolicy 参数 selection策略；parameter selection policy。
 */
public record RuntimeProviderPolicy(
        /**
         * 中文说明：保存 策略Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuntimeProviderPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy id; its type is {@code String}, and {@code RuntimeProviderPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeProviderPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeProviderPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        String policyId,
        /**
         * 中文说明：保存 type 对应的状态、依赖或配置值；字段类型为 {@code Type}，由 {@code RuntimeProviderPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by type; its type is {@code Type}, and {@code RuntimeProviderPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeProviderPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeProviderPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Type type,
        /**
         * 中文说明：保存 loadBalancer 对应的状态、依赖或配置值；字段类型为 {@code LoadBalancerType}，由 {@code RuntimeProviderPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by load balancer; its type is {@code LoadBalancerType}, and {@code RuntimeProviderPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeProviderPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeProviderPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        LoadBalancerType loadBalancer,
        /**
         * 中文说明：保存 selection策略 对应的状态、依赖或配置值；字段类型为 {@code ProviderSelectionPolicy}，由 {@code RuntimeProviderPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by selection policy; its type is {@code ProviderSelectionPolicy}, and {@code RuntimeProviderPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeProviderPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeProviderPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        ProviderSelectionPolicy selectionPolicy
) {

    /**
     * 中文说明：创建 {@code RuntimeProviderPolicy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RuntimeProviderPolicy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param policyId 参数 策略Id；parameter policy id。
     * @param type 参数 type；parameter type。
     * @param loadBalancer 参数 loadBalancer；parameter load balancer。
     * @param selectionPolicy 参数 selection策略；parameter selection policy。
     */
    public RuntimeProviderPolicy {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        type = Objects.requireNonNull(type, "type");
        if (type == Type.LOAD_BALANCE && loadBalancer == null) {
            throw new IllegalArgumentException(
                    "load balance policy requires an algorithm"
            );
        }
        if (type == Type.PROVIDER_OVERRIDE && selectionPolicy == null) {
            throw new IllegalArgumentException(
                    "provider override requires selection policy"
            );
        }
    }

    /**
     * 中文说明：{@code Type} 是枚举类型，位于当前 Gateway 模块的相关包中，负责Type相关的职责与边界。
     * English summary: {@code Type} is an enumeration in the current Gateway module; it owns the type-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public enum Type {
        /**
         * 中文说明：表示 LOAD负载均衡 这一固定值；它属于 {@code RuntimeProviderPolicy.Type} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value load balance; it is a state, type, or protocol value of {@code RuntimeProviderPolicy.Type} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeProviderPolicy.Type} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeProviderPolicy.Type}; do not couple callers to its representation when the owning type exposes an API.
         */
        LOAD_BALANCE,
        /**
         * 中文说明：表示 提供方OVERRIDE 这一固定值；它属于 {@code RuntimeProviderPolicy.Type} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value provider override; it is a state, type, or protocol value of {@code RuntimeProviderPolicy.Type} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeProviderPolicy.Type} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeProviderPolicy.Type}; do not couple callers to its representation when the owning type exposes an API.
         */
        PROVIDER_OVERRIDE
    }
}
