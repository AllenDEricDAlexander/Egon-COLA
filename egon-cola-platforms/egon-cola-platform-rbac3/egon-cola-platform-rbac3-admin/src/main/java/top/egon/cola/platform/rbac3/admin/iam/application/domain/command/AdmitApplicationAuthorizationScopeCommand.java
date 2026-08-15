package top.egon.cola.platform.rbac3.admin.iam.application.domain.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Request to admit a DDC Application as a tenant-local RBAC authorization scope. */
public record AdmitApplicationAuthorizationScopeCommand(
        @NotBlank String ddcApplicationId,
        @PositiveOrZero int displayPriority) {
}
