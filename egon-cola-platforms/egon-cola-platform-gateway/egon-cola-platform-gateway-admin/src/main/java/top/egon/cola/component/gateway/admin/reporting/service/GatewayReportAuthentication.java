package top.egon.cola.component.gateway.admin.reporting.service;


/**
 * 中文说明：{@code GatewayReportAuthentication} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关报告Authentication相关的职责与边界。
 * English summary: {@code GatewayReportAuthentication} is an immutable data carrier in the current Gateway module; it owns the gateway report authentication-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param applicationId 参数 applicationId；parameter application id。
 * @param bizCode 参数 bizCode；parameter biz code。
 * @param applicationCode 参数 applicationCode；parameter application code。
 * @param env 参数 env；parameter env。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param accessKey 参数 access键；parameter access key。
 */
public record GatewayReportAuthentication(
        /**
         * 中文说明：保存 applicationId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReportAuthentication} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by application id; its type is {@code String}, and {@code GatewayReportAuthentication} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReportAuthentication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportAuthentication}; do not couple callers to its representation when the owning type exposes an API.
         */
        String applicationId,
        /**
         * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReportAuthentication} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code GatewayReportAuthentication} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReportAuthentication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportAuthentication}; do not couple callers to its representation when the owning type exposes an API.
         */
        String bizCode,
        /**
         * 中文说明：保存 applicationCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReportAuthentication} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by application code; its type is {@code String}, and {@code GatewayReportAuthentication} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReportAuthentication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportAuthentication}; do not couple callers to its representation when the owning type exposes an API.
         */
        String applicationCode,
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReportAuthentication} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayReportAuthentication} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReportAuthentication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportAuthentication}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReportAuthentication} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayReportAuthentication} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReportAuthentication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportAuthentication}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 access键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReportAuthentication} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by access key; its type is {@code String}, and {@code GatewayReportAuthentication} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayReportAuthentication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportAuthentication}; do not couple callers to its representation when the owning type exposes an API.
         */
        String accessKey
) {

    /**
     * 中文说明：表示 请求ATTRIBUTE 这一固定值；它属于 {@code GatewayReportAuthentication} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value request attribute; it is a state, type, or protocol value of {@code GatewayReportAuthentication} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportAuthentication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportAuthentication}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String REQUEST_ATTRIBUTE =
            GatewayReportAuthentication.class.getName();
}
