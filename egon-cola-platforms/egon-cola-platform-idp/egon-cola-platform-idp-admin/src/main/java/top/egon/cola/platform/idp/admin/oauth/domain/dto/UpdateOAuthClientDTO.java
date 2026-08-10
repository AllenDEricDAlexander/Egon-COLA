package top.egon.cola.platform.idp.admin.oauth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;

/**
 * 更新 OAuth 客户端的输入数据。
 *
 * <p>Input data for updating an OAuth client.</p>
 */
public record UpdateOAuthClientDTO(
        @NotBlank String clientName,
        @NotNull IdentityClientEntity.Status status,
        @Positive int accessTokenTtlSeconds,
        @Positive int refreshTokenTtlSeconds,
        @PositiveOrZero long expectedVersion
) {
}
