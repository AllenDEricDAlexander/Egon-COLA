package top.egon.cola.platform.idp.admin.resource.domain.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.idp.core.resource.ResourceServer;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 应用环境级 Resource Server 的持久化对象。
 *
 * <p>Persistence object for an application-and-environment scoped Resource Server.</p>
 */
@Entity
@Table(name = "identity_resource_server")
public class IdentityResourceServerEntity {

    /** 数据库主键；database primary key. */
    @Id
    @Column(length = 64)
    private String id;

    /** 稳定 Resource Server 标识；stable Resource Server identifier. */
    @Column(name = "resource_server_id", nullable = false, length = 128)
    private String resourceServerId;

    /** RFC 8707 Resource URI；RFC 8707 Resource URI. */
    @Column(name = "resource_uri", nullable = false, length = 2048)
    private String resourceUri;

    /** 业务域编码；business-domain code. */
    @Column(name = "biz_code", nullable = false, length = 128)
    private String bizCode;

    /** 应用编码；application code. */
    @Column(name = "app_code", nullable = false, length = 128)
    private String appCode;

    /** 部署环境；deployment environment. */
    @Column(nullable = false, length = 128)
    private String environment;

    /** 管理展示名称；administrative display name. */
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    /** 管理和机器身份 Client；management and machine-identity Client. */
    @Column(name = "management_client_id", nullable = false, length = 128)
    private String managementClientId;

    /** USER 入口权限所属 RBAC3 应用；RBAC3 application for USER entry permission. */
    @Column(name = "rbac_application_code", nullable = false, length = 128)
    private String rbacApplicationCode;

    /** USER 进入 Resource 所需权限；permission required for USER entry. */
    @Column(name = "entry_permission_code", nullable = false, length = 256)
    private String entryPermissionCode;

    /** 准入票据有效秒数；admission-ticket lifetime in seconds. */
    @Column(name = "admission_ticket_ttl_seconds", nullable = false)
    private int admissionTicketTtlSeconds;

    /** Resource Server 状态；Resource Server status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /** 乐观锁和投影版本；optimistic-lock and projection version. */
    @Column(nullable = false)
    private long version;

    /** 创建时间；creation instant. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 最后更新时间；last update instant. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 供 JPA 使用的构造方法。
     *
     * <p>Constructor used by JPA.</p>
     */
    protected IdentityResourceServerEntity() {
    }

    /**
     * 创建并校验 Resource Server 持久化对象。
     *
     * <p>Creates and validates a Resource Server persistence object.</p>
     *
     * @param id 数据库主键；database primary key
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param resourceUri Resource URI；Resource URI
     * @param bizCode 业务域编码；business-domain code
     * @param appCode 应用编码；application code
     * @param environment 部署环境；deployment environment
     * @param displayName 展示名称；display name
     * @param managementClientId 管理 Client；management Client
     * @param rbacApplicationCode RBAC3 应用编码；RBAC3 application code
     * @param entryPermissionCode 入口权限编码；entry-permission code
     * @param admissionTicketTtlSeconds 准入票据有效秒数；admission-ticket lifetime in seconds
     * @param status 初始状态；initial status
     * @param now 创建时间；creation instant
     * @return 新持久化对象；new persistence object
     */
    public static IdentityResourceServerEntity create(
            String id,
            String resourceServerId,
            String resourceUri,
            String bizCode,
            String appCode,
            String environment,
            String displayName,
            String managementClientId,
            String rbacApplicationCode,
            String entryPermissionCode,
            int admissionTicketTtlSeconds,
            Status status,
            Instant now
    ) {
        Objects.requireNonNull(status, "status");
        new ResourceServer(
                resourceServerId,
                URI.create(resourceUri),
                bizCode,
                appCode,
                environment,
                managementClientId,
                rbacApplicationCode,
                entryPermissionCode,
                Duration.ofSeconds(admissionTicketTtlSeconds),
                ResourceServerStatus.valueOf(status.name()),
                0L
        );
        IdentityResourceServerEntity entity =
                new IdentityResourceServerEntity();
        entity.id = required(id, "id");
        entity.resourceServerId = resourceServerId;
        entity.resourceUri = resourceUri;
        entity.bizCode = bizCode;
        entity.appCode = appCode;
        entity.environment = environment;
        entity.displayName = required(displayName, "displayName");
        entity.managementClientId = managementClientId;
        entity.rbacApplicationCode = rbacApplicationCode;
        entity.entryPermissionCode = entryPermissionCode;
        entity.admissionTicketTtlSeconds = admissionTicketTtlSeconds;
        entity.status = status;
        entity.version = 0L;
        entity.createdAt = Objects.requireNonNull(now, "now");
        entity.updatedAt = now;
        return entity;
    }

    /** @return 数据库主键；database primary key */
    public String getId() {
        return id;
    }

    /** @return Resource Server 标识；Resource Server identifier */
    public String getResourceServerId() {
        return resourceServerId;
    }

    /** @return Resource URI；Resource URI */
    public String getResourceUri() {
        return resourceUri;
    }

    /** @return 业务域编码；business-domain code */
    public String getBizCode() {
        return bizCode;
    }

    /** @return 应用编码；application code */
    public String getAppCode() {
        return appCode;
    }

    /** @return 部署环境；deployment environment */
    public String getEnvironment() {
        return environment;
    }

    /** @return 展示名称；display name */
    public String getDisplayName() {
        return displayName;
    }

    /** @return 管理 Client 标识；management Client identifier */
    public String getManagementClientId() {
        return managementClientId;
    }

    /** @return RBAC3 应用编码；RBAC3 application code */
    public String getRbacApplicationCode() {
        return rbacApplicationCode;
    }

    /** @return 入口权限编码；entry-permission code */
    public String getEntryPermissionCode() {
        return entryPermissionCode;
    }

    /** @return 准入票据有效秒数；admission-ticket lifetime in seconds */
    public int getAdmissionTicketTtlSeconds() {
        return admissionTicketTtlSeconds;
    }

    /** @return Resource Server 状态；Resource Server status */
    public Status getStatus() {
        return status;
    }

    /** @return 乐观锁和投影版本；optimistic-lock and projection version */
    public long getVersion() {
        return version;
    }

    /** @return 创建时间；creation instant */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** @return 最后更新时间；last update instant */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验值；validated value
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * Resource Server 状态。
     *
     * <p>Resource Server status.</p>
     */
    public enum Status {

        /** 可签发 Token 和准入票据；tokens and admission tickets may be issued. */
        ACTIVE,

        /** 禁止签发新的 Token 和准入票据；new tokens and admission tickets are denied. */
        DISABLED
    }
}
