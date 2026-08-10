package top.egon.cola.platform.idp.admin.resource.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * 登记 Client 公开 JWK 的输入。
 *
 * <p>Input for registering a Client public JWK.</p>
 *
 * @param kid JWK kid；JWK kid
 * @param algorithm 签名算法；signature algorithm
 * @param publicJwk 只含公开材料的 JWK JSON；JWK JSON containing public material only
 * @param validFrom 生效时间；valid-from instant
 * @param validTo 失效时间；valid-to instant
 * @param expectedResourceVersion Resource Server 期望版本；expected Resource Server version
 */
public record CreateClientJwkDTO(
        @NotBlank String kid,
        @NotBlank String algorithm,
        @NotBlank String publicJwk,
        @NotNull Instant validFrom,
        @NotNull Instant validTo,
        @PositiveOrZero long expectedResourceVersion
) {
}
