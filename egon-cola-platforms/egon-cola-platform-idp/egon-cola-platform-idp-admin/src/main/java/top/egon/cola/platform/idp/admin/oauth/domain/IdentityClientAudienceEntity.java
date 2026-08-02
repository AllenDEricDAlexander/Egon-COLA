package top.egon.cola.platform.idp.admin.oauth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "identity_client_audience")
public class IdentityClientAudienceEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId;

    @Column(nullable = false, length = 256)
    private String audience;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdentityClientAudienceEntity() {
    }

    public static IdentityClientAudienceEntity create(
            String id,
            String clientId,
            String audience,
            Instant now
    ) {
        IdentityClientAudienceEntity entity =
                new IdentityClientAudienceEntity();
        entity.id = required(id, "id");
        entity.clientId = required(clientId, "clientId");
        entity.audience = required(audience, "audience");
        entity.createdAt = Objects.requireNonNull(now, "now");
        return entity;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAudience() {
        return audience;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
