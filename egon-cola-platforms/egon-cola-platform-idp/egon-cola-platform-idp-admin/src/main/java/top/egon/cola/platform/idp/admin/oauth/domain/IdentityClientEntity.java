package top.egon.cola.platform.idp.admin.oauth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "identity_client")
public class IdentityClientEntity {

    @Id
    @Column(name = "client_id", length = 128)
    private String clientId;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 32)
    private ClientType clientType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "pkce_required", nullable = false)
    private boolean pkceRequired;

    @Column(name = "access_token_ttl_seconds", nullable = false)
    private int accessTokenTtlSeconds;

    @Column(name = "refresh_token_ttl_seconds", nullable = false)
    private int refreshTokenTtlSeconds;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityClientEntity() {
    }

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

    public String getClientId() {
        return clientId;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isPkceRequired() {
        return pkceRequired;
    }

    public int getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public int getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
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

    public enum ClientType {
        PUBLIC,
        CONFIDENTIAL
    }

    public enum Status {
        ACTIVE,
        DISABLED
    }
}
