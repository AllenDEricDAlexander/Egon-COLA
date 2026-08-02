package top.egon.cola.platform.idp.core.oauth;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record OAuthClient(
        String clientId,
        ClientType clientType,
        Status status,
        boolean pkceRequired,
        List<String> redirectUris,
        List<String> audiences,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {

    private static final Duration DEFAULT_ACCESS_TOKEN_TTL =
            Duration.ofMinutes(15);
    private static final Duration DEFAULT_REFRESH_TOKEN_TTL =
            Duration.ofDays(7);

    public OAuthClient {
        clientId = required(clientId, "clientId");
        clientType = Objects.requireNonNull(clientType, "clientType");
        status = Objects.requireNonNull(status, "status");
        redirectUris = normalizedDistinct(redirectUris, "redirectUris");
        audiences = normalizedDistinct(audiences, "audiences");
        accessTokenTtl = durationInRange(
                accessTokenTtl,
                Duration.ofMinutes(5),
                Duration.ofMinutes(30),
                "accessTokenTtl"
        );
        refreshTokenTtl = durationInRange(
                refreshTokenTtl,
                Duration.ofDays(1),
                Duration.ofDays(30),
                "refreshTokenTtl"
        );
        if (redirectUris.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one redirect URI is required"
            );
        }
        if (audiences.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one audience is required"
            );
        }
    }

    public OAuthClient(
            String clientId,
            ClientType clientType,
            Status status,
            boolean pkceRequired,
            List<String> redirectUris,
            List<String> audiences
    ) {
        this(
                clientId,
                clientType,
                status,
                pkceRequired,
                redirectUris,
                audiences,
                DEFAULT_ACCESS_TOKEN_TTL,
                DEFAULT_REFRESH_TOKEN_TTL
        );
    }

    public boolean acceptsRedirectUri(String redirectUri) {
        return redirectUris.contains(redirectUri);
    }

    public boolean acceptsAudience(String audience) {
        return audiences.contains(audience);
    }

    public OAuthClient withStatus(Status value) {
        return new OAuthClient(
                clientId,
                clientType,
                value,
                pkceRequired,
                redirectUris,
                audiences,
                accessTokenTtl,
                refreshTokenTtl
        );
    }

    private static Duration durationInRange(
            Duration value,
            Duration minimum,
            Duration maximum,
            String field
    ) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(field + " is out of range");
        }
        return value;
    }

    private static List<String> normalizedDistinct(
            List<String> values,
            String field
    ) {
        Objects.requireNonNull(values, field);
        List<String> normalized = values.stream()
                .map(value -> required(value, field))
                .sorted()
                .distinct()
                .toList();
        if (normalized.size() != values.size()) {
            throw new IllegalArgumentException(field + " contains duplicates");
        }
        return normalized;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
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
