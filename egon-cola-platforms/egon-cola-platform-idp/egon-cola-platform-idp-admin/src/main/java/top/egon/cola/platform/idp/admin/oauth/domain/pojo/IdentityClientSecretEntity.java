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
 * OAuth Client Secret 历史凭证持久化对象。
 *
 * <p>Persistence object for the OAuth Client Secret credential history.</p>
 *
 * <p>Only the encoded hash, display hint and lifecycle metadata are persisted. Plaintext
 * credentials never enter this entity.</p>
 */
@Entity
@Table(name = "identity_client_secret")
public class IdentityClientSecretEntity {

    /** 凭证稳定标识；stable credential identifier. */
    @Id
    @Column(name = "id", length = 64)
    private String id;

    /** 所属 OAuth Client；owning OAuth Client identifier. */
    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId;

    /** Argon2id 编码哈希；Argon2id encoded hash. */
    @Column(name = "secret_hash", nullable = false, length = 512)
    private String secretHash;

    /** 明文 Secret 最后四位提示；last-four display hint. */
    @Column(name = "secret_hint", nullable = false, length = 8)
    private String secretHint;

    /** 凭证生命周期状态；credential lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    /** 业务乐观版本；business optimistic version. */
    @Column(nullable = false)
    private long version;

    /** 创建时间；creation instant. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 最后更新时间；last update instant. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** 撤销时间；revocation instant. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Creates an empty instance for JPA reflection. */
    protected IdentityClientSecretEntity() {
    }

    /**
     * 创建 ACTIVE hash-only Secret 凭证。
     *
     * <p>Creates an ACTIVE hash-only Secret credential.</p>
     *
     * @param id 凭证标识；credential identifier
     * @param clientId Client 标识；Client identifier
     * @param secretHash 编码哈希；encoded hash
     * @param secretHint 最后四位提示；last-four hint
     * @param now 创建时间；creation instant
     * @return 新凭证；new credential
     */
    public static IdentityClientSecretEntity create(
            String id,
            String clientId,
            String secretHash,
            String secretHint,
            Instant now
    ) {
        IdentityClientSecretEntity entity = new IdentityClientSecretEntity();
        entity.id = required(id, "id");
        entity.clientId = required(clientId, "clientId");
        entity.secretHash = required(secretHash, "secretHash");
        entity.secretHint = hint(secretHint);
        entity.status = Status.ACTIVE;
        entity.version = 0L;
        entity.createdAt = Objects.requireNonNull(now, "now");
        entity.updatedAt = now;
        return entity;
    }

    /** @return 凭证标识；credential identifier */
    public String getId() {
        return id;
    }

    /** @return Client 标识；Client identifier */
    public String getClientId() {
        return clientId;
    }

    /** @return 编码哈希；encoded hash */
    public String getSecretHash() {
        return secretHash;
    }

    /** @return 明文提示；display hint */
    public String getSecretHint() {
        return secretHint;
    }

    /** @return 凭证状态；credential status */
    public Status getStatus() {
        return status;
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

    /** @return 撤销时间；revocation instant */
    public Instant getRevokedAt() {
        return revokedAt;
    }

    /**
     * 将 ACTIVE 凭证立即撤销。
     *
     * <p>Immediately revokes an ACTIVE credential.</p>
     *
     * @param now 撤销时间；revocation instant
     */
    public void revoke(Instant now) {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("client secret is not active");
        }
        status = Status.REVOKED;
        revokedAt = Objects.requireNonNull(now, "now");
        updatedAt = now;
        version = Math.addExact(version, 1L);
    }

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

    private static String hint(String value) {
        String validated = required(value, "secretHint");
        if (validated.length() < 4 || validated.length() > 8) {
            throw new IllegalArgumentException("secretHint has invalid length");
        }
        return validated;
    }

    /** Secret lifecycle states; Secret 生命周期状态。 */
    public enum Status {
        /** 当前可用于认证；currently usable for authentication. */
        ACTIVE,
        /** 已立即撤销；immediately revoked. */
        REVOKED
    }
}
