package top.egon.cola.platform.idp.admin.token.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 预发布签名密钥的输入数据。
 *
 * <p>Input data for pre-publishing a signing key.</p>
 */
public record PublishSigningKeyDTO(
        @NotBlank String kid,
        @NotBlank String encryptedPrivateKey,
        @NotBlank String publicJwk
) {
}
