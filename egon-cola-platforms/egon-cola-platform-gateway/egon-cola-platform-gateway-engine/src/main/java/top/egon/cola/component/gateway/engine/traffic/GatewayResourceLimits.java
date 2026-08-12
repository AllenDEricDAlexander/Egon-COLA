package top.egon.cola.component.gateway.engine.traffic;

/**
 * 中文说明：{@code GatewayResourceLimits} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关资源Limits相关的职责与边界。
 * English summary: {@code GatewayResourceLimits} is an immutable data carrier in the current Gateway module; it owns the gateway resource limits-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param maximumQueryParameters 参数 maximumQueryParameters；parameter maximum query parameters。
 * @param maximumPathSegments 参数 maximumPathSegments；parameter maximum path segments。
 * @param maximumMetadataBytes 参数 maximum元数据Bytes；parameter maximum metadata bytes。
 * @param maximumBodyBytes 参数 maximumBodyBytes；parameter maximum body bytes。
 * @param maximumRpcMessageBytes 参数 maximumRpc消息Bytes；parameter maximum rpc message bytes。
 */
public record GatewayResourceLimits(
        /**
         * 中文说明：保存 maximumQueryParameters 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayResourceLimits} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum query parameters; its type is {@code int}, and {@code GatewayResourceLimits} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayResourceLimits} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayResourceLimits}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maximumQueryParameters,
        /**
         * 中文说明：保存 maximumPathSegments 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayResourceLimits} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum path segments; its type is {@code int}, and {@code GatewayResourceLimits} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayResourceLimits} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayResourceLimits}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maximumPathSegments,
        /**
         * 中文说明：保存 maximum元数据Bytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayResourceLimits} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum metadata bytes; its type is {@code int}, and {@code GatewayResourceLimits} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayResourceLimits} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayResourceLimits}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maximumMetadataBytes,
        /**
         * 中文说明：保存 maximumBodyBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayResourceLimits} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum body bytes; its type is {@code long}, and {@code GatewayResourceLimits} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayResourceLimits} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayResourceLimits}; do not couple callers to its representation when the owning type exposes an API.
         */
        long maximumBodyBytes,
        /**
         * 中文说明：保存 maximumRpc消息Bytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayResourceLimits} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum rpc message bytes; its type is {@code int}, and {@code GatewayResourceLimits} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayResourceLimits} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayResourceLimits}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maximumRpcMessageBytes
) {

    /**
     * 中文说明：创建 {@code GatewayResourceLimits} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayResourceLimits} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param maximumQueryParameters 参数 maximumQueryParameters；parameter maximum query parameters。
     * @param maximumPathSegments 参数 maximumPathSegments；parameter maximum path segments。
     * @param maximumMetadataBytes 参数 maximum元数据Bytes；parameter maximum metadata bytes。
     * @param maximumBodyBytes 参数 maximumBodyBytes；parameter maximum body bytes。
     * @param maximumRpcMessageBytes 参数 maximumRpc消息Bytes；parameter maximum rpc message bytes。
     */
    public GatewayResourceLimits {
        if (maximumQueryParameters < 1
                || maximumPathSegments < 1
                || maximumMetadataBytes < 1
                || maximumBodyBytes < 1
                || maximumRpcMessageBytes < 1) {
            throw new IllegalArgumentException(
                    "resource limits must be positive"
            );
        }
    }
}
