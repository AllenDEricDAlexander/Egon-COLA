package top.egon.cola.platform.idp.admin.resource.domain.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import top.egon.cola.platform.idp.core.resource.ClientJwkCredential;

import java.time.Instant;
import java.util.Objects;

/**
 * OAuth Client 的公开 JWK 持久化对象。
 *
 * <p>Persistence object for an OAuth Client public JWK.</p>
 */
@Entity
@Table(name = "identity_client_jwk")
public class IdentityClientJwkEntity {

    /** 数据库主键；database primary key. */
    @Id
    @Column(length = 64)
    private String id;

    /** OAuth Client 标识；OAuth Client identifier. */
    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId;

    /** JWK kid；JWK kid. */
    @Column(nullable = false, length = 128)
    private String kid;

    /** 签名算法；signature algorithm. */
    @Column(nullable = false, length = 32)
    private String algorithm;

    /** 公开 JWK JSON，不含私钥；public JWK JSON without private material. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "public_jwk", nullable = false, columnDefinition = "jsonb")
    private String publicJwk;

    /** 凭证生效时间；credential valid-from instant. */
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    /** 凭证失效时间；credential valid-to instant. */
    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    /** 凭证状态；credential status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /** 最近成功使用时间；most recent successful use instant. */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /** 乐观锁版本；optimistic-lock version. */
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
    protected IdentityClientJwkEntity() {
    }

    /**
     * 创建 ACTIVE RS256 公开凭证。
     *
     * <p>Creates an ACTIVE RS256 public credential.</p>
     *
     * @param id 数据库主键；database primary key
     * @param clientId Client 标识；Client identifier
     * @param kid JWK kid；JWK kid
     * @param publicJwk 公开 JWK JSON；public JWK JSON
     * @param validFrom 生效时间；valid-from instant
     * @param validTo 失效时间；valid-to instant
     * @param now 创建时间；creation instant
     * @return 新凭证；new credential
     */
    public static IdentityClientJwkEntity create(
            String id,
            String clientId,
            String kid,
            String publicJwk,
            Instant validFrom,
            Instant validTo,
            Instant now
    ) {
        new ClientJwkCredential(
                clientId,
                kid,
                "RS256",
                publicJwk,
                validFrom,
                validTo,
                ClientJwkCredential.Status.ACTIVE,
                null,
                0L
        );
        IdentityClientJwkEntity entity = new IdentityClientJwkEntity();
        entity.id = required(id, "id");
        entity.clientId = clientId;
        entity.kid = kid;
        entity.algorithm = "RS256";
        entity.publicJwk = publicJwk;
        entity.validFrom = validFrom;
        entity.validTo = validTo;
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

    /** @return JWK kid；JWK kid */
    public String getKid() {
        return kid;
    }

    /** @return 签名算法；signature algorithm */
    public String getAlgorithm() {
        return algorithm;
    }

    /** @return 公开 JWK JSON；public JWK JSON */
    public String getPublicJwk() {
        return publicJwk;
    }

    /** @return 生效时间；valid-from instant */
    public Instant getValidFrom() {
        return validFrom;
    }

    /** @return 失效时间；valid-to instant */
    public Instant getValidTo() {
        return validTo;
    }

    /** @return 凭证状态；credential status */
    public Status getStatus() {
        return status;
    }

    /** @return 最近使用时间；last-used instant */
    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    /** @return 乐观锁版本；optimistic-lock version */
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
     * Client JWK 状态。
     *
     * <p>Client JWK status.</p>
     */
    public enum Status {

        /** 凭证可用于校验；credential may verify assertions. */
        ACTIVE,

        /** 凭证不可用于校验；credential may not verify assertions. */
        DISABLED
    }
}
