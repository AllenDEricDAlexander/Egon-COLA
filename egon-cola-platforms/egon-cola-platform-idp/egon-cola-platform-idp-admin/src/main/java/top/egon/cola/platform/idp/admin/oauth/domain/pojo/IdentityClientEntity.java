package top.egon.cola.platform.idp.admin.oauth.domain.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * OAuth Client 主记录持久化对象。
 *
 * <p>Persistence object for the OAuth Client master record.</p>
 *
 * <p>PUBLIC Client 用于浏览器授权码 + PKCE；CONFIDENTIAL Client 用于机器
 * Client Credentials，不保存 Client Secret 明文。</p>
 *
 * <p>PUBLIC Clients use authorization code plus PKCE; CONFIDENTIAL Clients use machine
 * Client Credentials and store no plaintext Client Secret.</p>
 */
@Entity
@Table(name = "identity_client")
public class IdentityClientEntity {

    /** OAuth Client 稳定标识；stable OAuth Client identifier. */
    @Id
    @Column(name = "client_id", length = 128)
    private String clientId;

    /** 稳定业务应用身份；stable business application identity. */
    @Column(name = "app_id", length = 128)
    private String appId;

    /** Client 展示名称；Client display name. */
    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    /** Client 协议类型；Client protocol type. */
    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 32)
    private ClientType clientType;

    /** Client 可用状态；Client availability status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /** 是否强制 PKCE；whether PKCE is required. */
    @Column(name = "pkce_required", nullable = false)
    private boolean pkceRequired;

    /** Access Token 最大有效秒数；maximum access-token lifetime in seconds. */
    @Column(name = "access_token_ttl_seconds", nullable = false)
    private int accessTokenTtlSeconds;

    /** Refresh Token 配置有效秒数；configured refresh-token lifetime in seconds. */
    @Column(name = "refresh_token_ttl_seconds", nullable = false)
    private int refreshTokenTtlSeconds;

    /** 乐观锁业务版本；optimistic-lock business version. */
    @Column(nullable = false)
    private long version;

    /** 创建时间；creation instant. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 最后更新时间；last update instant. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 供 JPA 反射构造。
     *
     * <p>Creates an empty instance for JPA reflection.</p>
     */
    protected IdentityClientEntity() {
    }

    /**
     * 创建强制 PKCE 的 ACTIVE Public Client。
     *
     * <p>Creates an ACTIVE Public Client that requires PKCE.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param clientName Client 展示名称；Client display name
     * @param accessTokenTtlSeconds Access Token 有效秒数；access-token lifetime in seconds
     * @param refreshTokenTtlSeconds Refresh Token 有效秒数；refresh-token lifetime in seconds
     * @param now 创建时间；creation instant
     * @return 新 Public Client；new Public Client
     */
    public static IdentityClientEntity createPublic(
            String clientId,
            String clientName,
            int accessTokenTtlSeconds,
            int refreshTokenTtlSeconds,
            Instant now
    ) {
        requireRange(accessTokenTtlSeconds, 300, 1_800, "access token TTL");
        requireRange(
                refreshTokenTtlSeconds,
                86_400,
                2_592_000,
                "refresh token TTL"
        );
        IdentityClientEntity entity = new IdentityClientEntity();
        entity.clientId = required(clientId, "clientId");
        entity.clientName = required(clientName, "clientName");
        entity.clientType = ClientType.PUBLIC;
        entity.status = Status.ACTIVE;
        entity.pkceRequired = true;
        entity.accessTokenTtlSeconds = accessTokenTtlSeconds;
        entity.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        entity.version = 0L;
        entity.createdAt = Objects.requireNonNull(now, "now");
        entity.updatedAt = now;
        return entity;
    }

    /**
     * 创建仅允许密钥认证的 ACTIVE 机器 Confidential Client。
     *
     * <p>Creates an ACTIVE machine Confidential Client that authenticates only with keys.</p>
     *
     * @param appId 稳定业务应用身份；stable business application identity
     * @param clientId Client 标识；Client identifier
     * @param clientName Client 展示名称；Client display name
     * @param accessTokenTtlSeconds Access Token 最大有效秒数；maximum access-token lifetime
     * @param refreshTokenTtlSeconds 数据库兼容的 Refresh 配置，Client Credentials 不签发
     * Refresh Token；database-compatible refresh setting while Client Credentials issues no
     * refresh token
     * @param now 创建时间；creation instant
     * @return 新 Confidential Client；new Confidential Client
     */
    public static IdentityClientEntity createConfidential(
            String appId,
            String clientId,
            String clientName,
            int accessTokenTtlSeconds,
            int refreshTokenTtlSeconds,
            Instant now
    ) {
        requireRange(accessTokenTtlSeconds, 300, 1_800, "access token TTL");
        requireRange(
                refreshTokenTtlSeconds,
                86_400,
                2_592_000,
                "refresh token TTL"
        );
        IdentityClientEntity entity = new IdentityClientEntity();
        entity.appId = appId(appId);
        entity.clientId = required(clientId, "clientId");
        entity.clientName = required(clientName, "clientName");
        entity.clientType = ClientType.CONFIDENTIAL;
        entity.status = Status.ACTIVE;
        entity.pkceRequired = false;
        entity.accessTokenTtlSeconds = accessTokenTtlSeconds;
        entity.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        entity.version = 0L;
        entity.createdAt = Objects.requireNonNull(now, "now");
        entity.updatedAt = now;
        return entity;
    }

    /**
     * 创建兼容旧开发 Bootstrap 的 Confidential Client。
     *
     * <p>Creates a Confidential Client for legacy development bootstrap callers by using the
     * client id as the temporary stable application identity.</p>
     */
    public static IdentityClientEntity createConfidential(
            String clientId,
            String clientName,
            int accessTokenTtlSeconds,
            int refreshTokenTtlSeconds,
            Instant now
    ) {
        return createConfidential(
                clientId,
                clientId,
                clientName,
                accessTokenTtlSeconds,
                refreshTokenTtlSeconds,
                now
        );
    }

    /** @return Client 标识；Client identifier */
    public String getClientId() {
        return clientId;
    }

    /** @return 稳定业务应用身份；stable business application identity */
    public String getAppId() {
        return appId;
    }

    /** @return Client 展示名称；Client display name */
    public String getClientName() {
        return clientName;
    }

    /** @return Client 状态；Client status */
    public Status getStatus() {
        return status;
    }

    /** @return Client 类型；Client type */
    public ClientType getClientType() {
        return clientType;
    }

    /** @return 是否强制 PKCE；whether PKCE is required */
    public boolean isPkceRequired() {
        return pkceRequired;
    }

    /** @return Access Token 有效秒数；access-token lifetime in seconds */
    public int getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    /** @return Refresh Token 配置秒数；refresh-token configuration in seconds */
    public int getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    /** @return 业务版本；business version */
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
     * 使用乐观版本更新 Client 名称、状态和 Token TTL。
     *
     * <p>Updates Client name, status, and token lifetimes with optimistic version checking.</p>
     *
     * @param newClientName 新展示名称；new display name
     * @param newStatus 新状态；new status
     * @param newAccessTokenTtlSeconds 新 Access Token 有效秒数；new access-token lifetime
     * @param newRefreshTokenTtlSeconds 新 Refresh Token 配置秒数；new refresh-token setting
     * @param expectedVersion 期望版本；expected version
     * @param now 更新时间；update instant
     */
    public void update(
            String newClientName,
            Status newStatus,
            int newAccessTokenTtlSeconds,
            int newRefreshTokenTtlSeconds,
            long expectedVersion,
            Instant now
    ) {
        if (version != expectedVersion) {
            throw new IllegalStateException("stale OAuth client version");
        }
        requireRange(
                newAccessTokenTtlSeconds,
                300,
                1_800,
                "access token TTL"
        );
        requireRange(
                newRefreshTokenTtlSeconds,
                86_400,
                2_592_000,
                "refresh token TTL"
        );
        clientName = required(newClientName, "clientName");
        status = Objects.requireNonNull(newStatus, "status");
        accessTokenTtlSeconds = newAccessTokenTtlSeconds;
        refreshTokenTtlSeconds = newRefreshTokenTtlSeconds;
        version = Math.addExact(version, 1L);
        updatedAt = Objects.requireNonNull(now, "now");
    }

    /**
     * 为 Secret 轮换执行受锁保护的版本递增。
     *
     * <p>Advances the Client version for a lock-protected Secret rotation.</p>
     *
     * @param expectedVersion 期望版本；expected version
     * @param now 更新时间；update instant
     */
    public void rotateSecret(long expectedVersion, Instant now) {
        if (version != expectedVersion) {
            throw new IllegalStateException("stale OAuth client version");
        }
        version = Math.addExact(version, 1L);
        updatedAt = Objects.requireNonNull(now, "now");
    }

    /**
     * 校验必填且无首尾空白的文本。
     *
     * <p>Validates required text without surrounding whitespace.</p>
     *
     * @param value 待校验值；value to validate
     * @param fieldName 字段名；field name
     * @return 已校验值；validated value
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(
                    fieldName + " must not contain surrounding whitespace"
            );
        }
        return value;
    }

    /** 校验稳定业务应用身份格式；validates the stable application identity format. */
    private static String appId(String value) {
        String validated = required(value, "appId");
        if (!validated.matches("[a-z][a-z0-9-]{2,127}")) {
            throw new IllegalArgumentException("appId has invalid format");
        }
        return validated;
    }

    /**
     * 校验整数范围。
     *
     * <p>Validates an integer range.</p>
     *
     * @param value 待校验值；value to validate
     * @param minimum 最小值；minimum
     * @param maximum 最大值；maximum
     * @param fieldName 字段名；field name
     */
    private static void requireRange(
            int value,
            int minimum,
            int maximum,
            String fieldName
    ) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(fieldName + " is out of range");
        }
    }

    /** OAuth Client 协议类型；OAuth Client protocol type. */
    public enum ClientType {
        /** 浏览器或本地应用 Public Client；browser or native Public Client. */
        PUBLIC,
        /** 能安全保存应用 Secret 的机器 Confidential Client；machine Confidential Client. */
        CONFIDENTIAL
    }

    /** OAuth Client 状态；OAuth Client status. */
    public enum Status {
        /** Client 可参与协议流程；Client may participate in protocol flows. */
        ACTIVE,
        /** Client 禁用；Client is disabled. */
        DISABLED
    }
}
