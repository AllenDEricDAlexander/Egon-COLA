package top.egon.cola.platform.idp.admin.oauth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "identity_client_redirect_uri")
public class IdentityClientRedirectUriEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId;

    @Column(name = "redirect_uri", nullable = false, length = 2048)
    private String redirectUri;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdentityClientRedirectUriEntity() {
    }

    public static IdentityClientRedirectUriEntity create(
            String id,
            String clientId,
            String redirectUri,
            Instant now
    ) {
        IdentityClientRedirectUriEntity entity =
                new IdentityClientRedirectUriEntity();
        entity.id = required(id, "id");
        entity.clientId = required(clientId, "clientId");
        entity.redirectUri = validRedirectUri(redirectUri);
        entity.createdAt = Objects.requireNonNull(now, "now");
        return entity;
    }

    public String getClientId() {
        return clientId;
    }

    public String getId() {
        return id;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    private static String validRedirectUri(String value) {
        String exact = required(value, "redirectUri");
        URI uri = URI.create(exact);
        if (!uri.isAbsolute()
                || uri.getFragment() != null
                || uri.getUserInfo() != null
                || !("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("invalid redirectUri");
        }
        return exact;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
