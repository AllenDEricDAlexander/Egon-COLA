package top.egon.cola.platform.idp.admin.oauth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 创建 OAuth 公共客户端的输入数据。
 *
 * <p>Input data for creating a public OAuth client.</p>
 */
public record CreateOAuthClientDTO(
        @NotBlank String clientId,
        @NotBlank String clientName,
        @Positive int accessTokenTtlSeconds,
        @Positive int refreshTokenTtlSeconds,
        @NotEmpty List<@NotBlank String> redirectUris,
        @NotEmpty List<@NotBlank String> audiences
) {
}
