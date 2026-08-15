package top.egon.cola.platform.rbac3.admin.iam.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum;

/** Command for creating an RBAC membership bound to an IdP subject. */
public record CreateUserCommandDTO(
        @NotBlank String identitySub,
        @NotNull UserStatusEnum status) {
}
