package top.egon.cola.platform.idp.admin.identity.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建统一身份用户的输入数据。
 *
 * <p>Input data for creating an identity user.</p>
 */
public record CreateIdentityUserDTO(
        @NotBlank String username,
        @NotBlank String displayName
) {
}
