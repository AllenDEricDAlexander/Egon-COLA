package top.egon.cola.platform.rbac3.admin.iam.permission.domain.dto;

import jakarta.validation.constraints.NotBlank;

/** Global permission catalog create command. */
public record CreatePermissionRequestDTO(
        @NotBlank String applicationId,
        @NotBlank String permissionCode,
        @NotBlank String permissionName,
        @NotBlank String riskLevel,
        String description) {
}
