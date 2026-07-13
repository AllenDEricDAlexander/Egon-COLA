package ${package}.facade.user.dto;

import jakarta.validation.constraints.NotBlank;

public record GrantPermissionDTO(
        @NotBlank String roleCode,
        @NotBlank String permissionCode) {
}
