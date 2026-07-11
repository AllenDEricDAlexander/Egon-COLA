package ${package}.facade.dto.user;

import jakarta.validation.constraints.NotBlank;

public record GrantPermissionDTO(
        @NotBlank String roleCode,
        @NotBlank String permissionCode) {
}
