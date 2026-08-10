package top.egon.cola.platform.idp.admin.identity.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;

/**
 * 更新统一身份用户的输入数据。
 *
 * <p>Input data for updating an identity user.</p>
 */
public record UpdateIdentityUserDTO(
        @NotBlank String displayName,
        @NotNull IdentityUserStatus status,
        @PositiveOrZero long expectedVersion
) {
}
