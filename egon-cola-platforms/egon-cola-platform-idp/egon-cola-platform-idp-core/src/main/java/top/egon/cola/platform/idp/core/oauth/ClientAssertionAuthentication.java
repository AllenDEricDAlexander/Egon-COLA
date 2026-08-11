package top.egon.cola.platform.idp.core.oauth;

import java.time.Instant;
import java.util.Objects;

/**
 * 已通过 {@code private_key_jwt} 验证的 OAuth Client 身份。
 *
 * <p>OAuth Client identity authenticated through {@code private_key_jwt}.</p>
 *
 * @param clientId Client 标识；Client identifier
 * @param credentialId 验签使用的 JWK kid；JWK kid used for verification
 * @param assertionId Assertion 的 jti；assertion jti
 * @param issuedAt Assertion 签发时间；assertion issuance instant
 * @param expiresAt Assertion 过期时间；assertion expiration instant
 */
public record ClientAssertionAuthentication(
        String clientId,
        String credentialId,
        String assertionId,
        Instant issuedAt,
        Instant expiresAt
) {

    /**
     * 校验已认证 Client 上下文的完整性。
     *
     * <p>Validates the integrity of the authenticated Client context.</p>
     */
    public ClientAssertionAuthentication {
        clientId = required(clientId, "clientId");
        credentialId = required(credentialId, "credentialId");
        assertionId = required(assertionId, "assertionId");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "assertion expiration must be after issuance"
            );
        }
    }

    /**
     * 校验必填且不带首尾空白的文本。
     *
     * <p>Validates required text without surrounding whitespace.</p>
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
}
