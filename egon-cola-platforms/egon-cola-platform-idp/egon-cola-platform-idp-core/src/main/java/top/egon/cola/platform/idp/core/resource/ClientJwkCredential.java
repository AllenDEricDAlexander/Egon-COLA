package top.egon.cola.platform.idp.core.resource;

import java.time.Instant;
import java.util.Objects;

/**
 * IdP 保存的 OAuth Client 公开 JWK 凭证。
 *
 * <p>Public JWK credential stored by IdP for an OAuth Client.</p>
 *
 * @param clientId    Client 标识；Client identifier
 * @param keyId       JWK kid；JWK kid
 * @param algorithm   允许的签名算法；allowed signature algorithm
 * @param publicJwk   公开 JWK JSON；public JWK JSON
 * @param validFrom   生效时间；valid-from instant
 * @param validUntil  失效时间；valid-until instant
 * @param status      凭证状态；credential status
 * @param lastUsedAt  最近成功使用时间；most recent successful use
 * @param version     乐观锁版本；optimistic-lock version
 */
public record ClientJwkCredential(
        String clientId,
        String keyId,
        String algorithm,
        String publicJwk,
        Instant validFrom,
        Instant validUntil,
        Status status,
        Instant lastUsedAt,
        long version
) {

    /**
     * 校验公开凭证。
     *
     * <p>Validates the public credential.</p>
     */
    public ClientJwkCredential {
        clientId = required(clientId, "clientId");
        keyId = required(keyId, "keyId");
        algorithm = required(algorithm, "algorithm");
        if (!"RS256".equals(algorithm)) {
            throw new IllegalArgumentException(
                    "only RS256 credentials are supported"
            );
        }
        publicJwk = required(publicJwk, "publicJwk");
        if (!publicJwk.startsWith("{") || !publicJwk.endsWith("}")) {
            throw new IllegalArgumentException("publicJwk must be JSON");
        }
        validFrom = Objects.requireNonNull(validFrom, "validFrom");
        validUntil = Objects.requireNonNull(validUntil, "validUntil");
        if (!validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException(
                    "validUntil must be after validFrom"
            );
        }
        status = Objects.requireNonNull(status, "status");
        if (lastUsedAt != null && lastUsedAt.isBefore(validFrom)) {
            throw new IllegalArgumentException(
                    "lastUsedAt must not precede validFrom"
            );
        }
        if (version < 0L) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    /**
     * 判断凭证在指定时刻是否可用于验证。
     *
     * <p>Determines whether the credential may verify assertions at the given instant.</p>
     *
     * @param instant 校验时刻；verification instant
     * @return 状态和时间窗均有效时为 {@code true}；{@code true} when status and validity window allow use
     */
    public boolean activeAt(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return status == Status.ACTIVE
                && !instant.isBefore(validFrom)
                && instant.isBefore(validUntil);
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验文本；text to validate
     * @param field 字段名；field name
     * @return 已校验文本；validated text
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

        /**
         * 凭证可用。
         *
         * <p>The credential is active.</p>
         */
        ACTIVE,

        /**
         * 凭证禁用。
         *
         * <p>The credential is disabled.</p>
         */
        DISABLED
    }
}
