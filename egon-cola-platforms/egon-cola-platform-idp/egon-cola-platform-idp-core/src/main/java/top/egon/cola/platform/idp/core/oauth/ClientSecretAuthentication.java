package top.egon.cola.platform.idp.core.oauth;

import java.util.Objects;

/**
 * 已通过 OAuth {@code client_secret_basic} 验证的 Client 身份。
 *
 * <p>OAuth Client identity authenticated through {@code client_secret_basic}.</p>
 *
 * @param clientId OAuth Client 标识；OAuth Client identifier
 * @param credentialId active Secret 凭证标识；active Secret credential identifier
 */
public record ClientSecretAuthentication(
        String clientId,
        String credentialId
) {

    /** Validates the trusted Client Secret authentication result. */
    public ClientSecretAuthentication {
        clientId = required(clientId, "clientId");
        credentialId = required(credentialId, "credentialId");
    }

    private static String required(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
