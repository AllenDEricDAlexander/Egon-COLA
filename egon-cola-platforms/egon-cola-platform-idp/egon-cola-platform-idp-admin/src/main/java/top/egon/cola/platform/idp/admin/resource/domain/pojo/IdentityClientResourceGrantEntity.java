package top.egon.cola.platform.idp.admin.resource.domain.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

/**
 * OAuth Client 访问一个 Resource Server 的持久化授权。
 *
 * <p>Persisted authorization for an OAuth Client to access one Resource Server.</p>
 */
@Entity
@Table(name = "identity_client_resource_grant")
public class IdentityClientResourceGrantEntity {

    /** 数据库主键；database primary key. */
    @Id
    @Column(length = 64)
    private String id;

    /** 被授权 Client；authorized Client. */
    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId;

    /** 目标 Resource Server；target Resource Server. */
    @Column(name = "resource_server_id", nullable = false, length = 128)
    private String resourceServerId;

    /** 授权类型；grant type. */
    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type", nullable = false, length = 32)
    private GrantType grantType;

    /** CLIENT_CREDENTIALS 绑定的租户；tenant bound to CLIENT_CREDENTIALS. */
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    /** 服务访问许可 Scope 的 JSON 数组；JSON array of service scopes allowed by IdP. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_scopes", nullable = false, columnDefinition = "jsonb")
    private String allowedScopes;

    /** 授权状态；grant status. */
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
    protected IdentityClientResourceGrantEntity() {
    }

    /**
     * 创建不携带租户和 Scope 的用户委托授权。
     *
     * <p>Creates a user-delegation grant without tenant or scopes.</p>
     *
     * @param id 数据库主键；database primary key
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param now 创建时间；creation instant
     * @return 用户委托授权；user-delegation grant
     */
    public static IdentityClientResourceGrantEntity userDelegation(
            String id,
            String clientId,
            String resourceServerId,
            Instant now
    ) {
        return create(
                id,
                clientId,
                resourceServerId,
                GrantType.USER_DELEGATION,
                null,
                "[]",
                now
        );
    }

    /**
     * 创建绑定单租户和非空 Scope 的服务授权。
     *
     * <p>Creates a service grant bound to one tenant and a non-empty scope array.</p>
     *
     * @param id 数据库主键；database primary key
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param tenantId 租户标识；tenant identifier
     * @param allowedScopes Scope JSON 数组；scope JSON array
     * @param now 创建时间；creation instant
     * @return 服务访问授权；service-access grant
     */
    public static IdentityClientResourceGrantEntity clientCredentials(
            String id,
            String clientId,
            String resourceServerId,
            String tenantId,
            String allowedScopes,
            Instant now
    ) {
        return create(
                id,
                clientId,
                resourceServerId,
                GrantType.CLIENT_CREDENTIALS,
                required(tenantId, "tenantId"),
                nonEmptyJsonArray(allowedScopes),
                now
        );
    }

    /**
     * 创建持久化授权。
     *
     * <p>Creates a persisted grant.</p>
     *
     * @param id 数据库主键；database primary key
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param grantType 授权类型；grant type
     * @param tenantId 可选租户；optional tenant
     * @param allowedScopes Scope JSON；scope JSON
     * @param now 创建时间；creation instant
     * @return 新授权；new grant
     */
    private static IdentityClientResourceGrantEntity create(
            String id,
            String clientId,
            String resourceServerId,
            GrantType grantType,
            String tenantId,
            String allowedScopes,
            Instant now
    ) {
        IdentityClientResourceGrantEntity entity =
                new IdentityClientResourceGrantEntity();
        entity.id = required(id, "id");
        entity.clientId = required(clientId, "clientId");
        entity.resourceServerId = required(
                resourceServerId,
                "resourceServerId"
        );
        entity.grantType = Objects.requireNonNull(grantType, "grantType");
        entity.tenantId = tenantId;
        entity.allowedScopes = jsonArray(allowedScopes);
        entity.status = Status.ACTIVE;
        entity.version = 0L;
        entity.createdAt = Objects.requireNonNull(now, "now");
        entity.updatedAt = now;
        return entity;
    }

    /** @return 数据库主键；database primary key */
    public String getId() {
        return id;
    }

    /** @return Client 标识；Client identifier */
    public String getClientId() {
        return clientId;
    }

    /** @return Resource Server 标识；Resource Server identifier */
    public String getResourceServerId() {
        return resourceServerId;
    }

    /** @return 授权类型；grant type */
    public GrantType getGrantType() {
        return grantType;
    }

    /** @return 租户标识或空；tenant identifier or {@code null} */
    public String getTenantId() {
        return tenantId;
    }

    /** @return Scope JSON 数组；scope JSON array */
    public String getAllowedScopes() {
        return allowedScopes;
    }

    /** @return 授权状态；grant status */
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
     * 校验非空 JSON 数组文本。
     *
     * <p>Validates non-empty JSON-array text.</p>
     *
     * @param value JSON 文本；JSON text
     * @return 已校验文本；validated text
     */
    private static String nonEmptyJsonArray(String value) {
        String array = jsonArray(value);
        if ("[]".equals(array.replaceAll("\\s+", ""))) {
            throw new IllegalArgumentException(
                    "allowedScopes must not be empty"
            );
        }
        return array;
    }

    /**
     * 校验 JSON 数组外形。
     *
     * <p>Validates the JSON-array shape.</p>
     *
     * @param value JSON 文本；JSON text
     * @return 已校验文本；validated text
     */
    private static String jsonArray(String value) {
        String array = required(value, "allowedScopes");
        if (!array.startsWith("[") || !array.endsWith("]")) {
            throw new IllegalArgumentException(
                    "allowedScopes must be a JSON array"
            );
        }
        return array;
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
     * Client Resource Grant 类型。
     *
     * <p>Client Resource Grant type.</p>
     */
    public enum GrantType {

        /** 用户授权码和刷新令牌委托；user authorization-code and refresh delegation. */
        USER_DELEGATION,

        /** 服务端 Client Credentials 授权；service client-credentials authorization. */
        CLIENT_CREDENTIALS
    }

    /**
     * Client Resource Grant 状态。
     *
     * <p>Client Resource Grant status.</p>
     */
    public enum Status {

        /** 授权生效；grant is active. */
        ACTIVE,

        /** 授权禁用；grant is disabled. */
        DISABLED
    }
}
