package top.egon.cola.platform.rbac3.admin.iam.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Command for changing only the local IdP subject binding. */
public record UpdateUserCommandDTO(
        @NotBlank String identitySub,
        @PositiveOrZero long expectedAuthVersion) {
}
