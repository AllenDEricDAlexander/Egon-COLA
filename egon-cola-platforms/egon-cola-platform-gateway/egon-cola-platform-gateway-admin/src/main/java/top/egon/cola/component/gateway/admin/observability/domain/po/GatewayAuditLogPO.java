package top.egon.cola.component.gateway.admin.observability.domain.po;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 中文说明：{@code GatewayAuditLogPO} 是类型，位于当前 Gateway 模块的相关包中，负责网关审计LogEntity相关的职责与边界。
 * English summary: {@code GatewayAuditLogPO} is a type in the current Gateway module; it owns the gateway audit log entity-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Entity
@Table(name = "gateway_audit_log")
public class GatewayAuditLogPO {

    /**
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Id
    private String id;

    /**
     * 中文说明：保存 actorId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by actor id; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "actor_id", nullable = false)
    private String actorId;

    /**
     * 中文说明：保存 actorType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by actor type; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "actor_type", nullable = false)
    private String actorType;

    /**
     * 中文说明：保存 source 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by source; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private String source;

    /**
     * 中文说明：保存 请求Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by request id; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "request_id")
    private String requestId;

    /**
     * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "trace_id")
    private String traceId;

    /**
     * 中文说明：保存 资源Type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by resource type; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    /**
     * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    /**
     * 中文说明：保存 action 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by action; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private String action;

    /**
     * 中文说明：保存 beforeSummary 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by before summary; its type is {@code Map<String, Object>}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_summary", columnDefinition = "jsonb")
    private Map<String, Object> beforeSummary;

    /**
     * 中文说明：保存 afterSummary 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by after summary; its type is {@code Map<String, Object>}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_summary", columnDefinition = "jsonb")
    private Map<String, Object> afterSummary;

    /**
     * 中文说明：保存 草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by draft revision; its type is {@code Long}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "draft_revision")
    private Long draftRevision;

    /**
     * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "release_id")
    private String releaseId;

    /**
     * 中文说明：保存 successful 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by successful; its type is {@code boolean}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(nullable = false)
    private boolean successful;

    /**
     * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "error_code")
    private String errorCode;

    /**
     * 中文说明：保存 occurredAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayAuditLogPO} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by occurred at; its type is {@code Instant}, and {@code GatewayAuditLogPO} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuditLogPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuditLogPO}; do not couple callers to its representation when the owning type exposes an API.
     */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /**
     * 中文说明：创建 {@code GatewayAuditLogPO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayAuditLogPO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    protected GatewayAuditLogPO() {
    }

    /**
     * 中文说明：创建 {@code GatewayAuditLogPO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayAuditLogPO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param id 参数 id；parameter id。
     * @param actorId 参数 actorId；parameter actor id。
     * @param actorType 参数 actorType；parameter actor type。
     * @param source 参数 source；parameter source。
     * @param requestId 参数 请求Id；parameter request id。
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
    public GatewayAuditLogPO(
            String id,
            String actorId,
            String actorType,
            String source,
            String requestId,
            String traceId,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> beforeSummary,
            Map<String, Object> afterSummary,
            Long draftRevision,
            String releaseId,
            boolean successful,
            String errorCode,
            Instant occurredAt) {
        this.id = id;
        this.actorId = actorId;
        this.actorType = actorType;
        this.source = source;
        this.requestId = requestId;
        this.traceId = traceId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.action = action;
        this.beforeSummary = sanitized(beforeSummary);
        this.afterSummary = sanitized(afterSummary);
        this.draftRevision = draftRevision;
        this.releaseId = releaseId;
        this.successful = successful;
        this.errorCode = errorCode;
        this.occurredAt = occurredAt;
    }

    /**
     * 中文说明：执行 sanitized 操作；该方法是 {@code GatewayAuditLogPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sanitized operation; this method is the invocation entry point on {@code GatewayAuditLogPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAuditLogPO.sanitized(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 sanitized 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> sanitized(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        source.forEach((key, value) -> {
            String lower = key.toLowerCase(java.util.Locale.ROOT);
            if (!lower.contains("secret")
                    && !lower.contains("token")
                    && !lower.contains("authorization")
                    && !lower.contains("cookie")) {
                result.put(key, value);
            }
        });
        return Map.copyOf(result);
    }
}
