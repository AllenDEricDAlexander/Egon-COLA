package top.egon.cola.platform.idp.admin.token.domain.vo;

import java.time.Instant;

/**
 * 管理端展示的签名密钥元数据，不包含私钥明文。
 *
 * <p>Signing-key metadata returned by administration APIs without private-key material.</p>
 */
public record SigningKeyVO(
        String kid,
        String algorithm,
        String publicJwk,
        String status,
        boolean runtimeServing,
        Instant activatedAt,
        Instant retiredAt,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
