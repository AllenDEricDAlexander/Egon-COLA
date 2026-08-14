package top.egon.cola.component.gateway.admin.observability.domain.vo;


import java.time.Instant;

/**
 * 中文说明：{@code GatewayAuditVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审计Summary相关的职责与边界。
 * English summary: {@code GatewayAuditVO} is an immutable data carrier in the current Gateway module; it owns the audit summary-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param actorId 参数 actorId；parameter actor id。
 * @param actorType 参数 actorType；parameter actor type。
 * @param source 参数 source；parameter source。
 * @param traceId 参数 traceId；parameter trace id。
 * @param resourceType 参数 资源Type；parameter resource type。
 * @param resourceId 参数 资源Id；parameter resource id。
 * @param action 参数 action；parameter action。
 * @param beforeSummary 参数 beforeSummary；parameter before summary。
 * @param afterSummary 参数 afterSummary；parameter after summary。
 * @param draftRevision 参数 草稿Revision；parameter draft revision。
 * @param releaseId 参数 发布Id；parameter release id。
 * @param successful 参数 successful；parameter successful。
 * @param errorCode 参数 errorCode；parameter error code。
 * @param occurredAt 参数 occurredAt；parameter occurred at。
 */
public record GatewayAuditVO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 actorId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by actor id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String actorId,
        /**
         * 中文说明：保存 actorType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by actor type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String actorType,
        /**
         * 中文说明：保存 source 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by source; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String source,
        /**
         * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String traceId,
        /**
         * 中文说明：保存 资源Type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String resourceType,
        /**
         * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String resourceId,
        /**
         * 中文说明：保存 action 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by action; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String action,
        /**
         * 中文说明：保存 beforeSummary 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by before summary; its type is {@code Object}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Object beforeSummary,
        /**
         * 中文说明：保存 afterSummary 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by after summary; its type is {@code Object}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Object afterSummary,
        /**
         * 中文说明：保存 草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by draft revision; its type is {@code Long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Long draftRevision,
        /**
         * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String releaseId,
        /**
         * 中文说明：保存 successful 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by successful; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean successful,
        /**
         * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String errorCode,
        /**
         * 中文说明：保存 occurredAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by occurred at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant occurredAt
) {
}
