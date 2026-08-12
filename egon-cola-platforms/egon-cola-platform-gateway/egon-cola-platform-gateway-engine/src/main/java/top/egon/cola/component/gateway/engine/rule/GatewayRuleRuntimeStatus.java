package top.egon.cola.component.gateway.engine.rule;

import java.time.Instant;

/**
 * 中文说明：{@code GatewayRuleRuntimeStatus} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关规则运行时Status相关的职责与边界。
 * English summary: {@code GatewayRuleRuntimeStatus} is an immutable data carrier in the current Gateway module; it owns the gateway rule runtime status-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param activeReleaseId 参数 active发布Id；parameter active release id。
 * @param activeDdcVersion 参数 activeDdcVersion；parameter active ddc version。
 * @param ruleSchemaVersion 参数 规则模式Version；parameter rule schema version。
 * @param ruleContentSha256 参数 规则ContentSha256；parameter rule content sha256。
 * @param artifactSha256 参数 制品Sha256；parameter artifact sha256。
 * @param lastStage 参数 lastStage；parameter last stage。
 * @param lastError 参数 lastError；parameter last error。
 * @param routeCount 参数 路由Count；parameter route count。
 * @param operationCount 参数 操作Count；parameter operation count。
 * @param providerServiceCount 参数 提供方服务Count；parameter provider service count。
 * @param stagingChunkCount 参数 stagingChunkCount；parameter staging chunk count。
 * @param ready 参数 ready；parameter ready。
 * @param degraded 参数 degraded；parameter degraded。
 * @param updatedAt 参数 updatedAt；parameter updated at。
 */
public record GatewayRuleRuntimeStatus(
        /**
         * 中文说明：保存 active发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by active release id; its type is {@code String}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        String activeReleaseId,
        /**
         * 中文说明：保存 activeDdcVersion 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by active ddc version; its type is {@code long}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        long activeDdcVersion,
        /**
         * 中文说明：保存 规则模式Version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by rule schema version; its type is {@code String}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        String ruleSchemaVersion,
        /**
         * 中文说明：保存 规则ContentSha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by rule content sha256; its type is {@code String}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        String ruleContentSha256,
        /**
         * 中文说明：保存 制品Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by artifact sha256; its type is {@code String}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        String artifactSha256,
        /**
         * 中文说明：保存 lastStage 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleApplyStage}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by last stage; its type is {@code GatewayRuleApplyStage}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayRuleApplyStage lastStage,
        /**
         * 中文说明：保存 lastError 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by last error; its type is {@code String}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        String lastError,
        /**
         * 中文说明：保存 路由Count 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by route count; its type is {@code int}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        int routeCount,
        /**
         * 中文说明：保存 操作Count 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation count; its type is {@code int}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        int operationCount,
        /**
         * 中文说明：保存 提供方服务Count 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider service count; its type is {@code int}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        int providerServiceCount,
        /**
         * 中文说明：保存 stagingChunkCount 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by staging chunk count; its type is {@code int}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        int stagingChunkCount,
        /**
         * 中文说明：保存 ready 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by ready; its type is {@code boolean}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean ready,
        /**
         * 中文说明：保存 degraded 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by degraded; its type is {@code boolean}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean degraded,
        /**
         * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayRuleRuntimeStatus} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayRuleRuntimeStatus} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRuleRuntimeStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleRuntimeStatus}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant updatedAt
) {

    /**
     * 中文说明：执行 empty 操作；该方法是 {@code GatewayRuleRuntimeStatus} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the empty operation; this method is the invocation entry point on {@code GatewayRuleRuntimeStatus} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleRuntimeStatus.empty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 empty 的处理结果；returns the result of the operation.
     */
    public static GatewayRuleRuntimeStatus empty() {
        return new GatewayRuleRuntimeStatus(
                null,
                0,
                null,
                null,
                null,
                GatewayRuleApplyStage.NEVER_APPLIED,
                null,
                0,
                0,
                0,
                0,
                false,
                false,
                Instant.EPOCH
        );
    }
}
