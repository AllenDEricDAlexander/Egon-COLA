package top.egon.cola.platform.idp.contract;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * 由 IdP USER Access Token 解析出的用户身份。
 *
 * <p>User identity parsed from an IdP USER Access Token.</p>
 *
 * @param subject      用户身份标识；user identity subject
 * @param tenantId     当前租户；current tenant
 * @param tokenId      Access Token jti；Access Token jti
 * @param audience     Token Resource Audience；Token Resource Audience
 * @param issuedAt     签发时间；issued-at instant
 * @param expiresAt    过期时间；expiration instant
 */
public record IdentityPrincipal(
        String subject,
        String tenantId,
        String tokenId,
        Set<String> audience,
        Instant issuedAt,
        Instant expiresAt,
        AuthenticationContext authenticationContext
) implements IdpPrincipal {

    /**
     * 校验并规范化 USER 身份。
     *
     * <p>Validates and normalizes the USER identity.</p>
     */
    public IdentityPrincipal {
        subject = required(subject, "subject");
        tenantId = required(tenantId, "tenantId");
        tokenId = required(tokenId, "tokenId");
        audience = Set.copyOf(Objects.requireNonNull(
                audience,
                "audience"
        ));
        if (audience.isEmpty() || audience.stream().anyMatch(
                value -> value == null || value.isBlank()
        )) {
            throw new IllegalArgumentException(
                    "audience must contain only non-blank values"
            );
        }
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        authenticationContext = Objects.requireNonNull(
                authenticationContext,
                "authenticationContext"
        );
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt"
            );
        }
    }

    /**
     * 返回 USER 主体类型。
     *
     * <p>Returns the USER principal type.</p>
     *
     * @return USER
     */
    @Override
    public PrincipalType principalType() {
        return PrincipalType.USER;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value     待校验值；value to validate
     * @param fieldName 字段名；field name
     * @return 已校验文本；validated text
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
