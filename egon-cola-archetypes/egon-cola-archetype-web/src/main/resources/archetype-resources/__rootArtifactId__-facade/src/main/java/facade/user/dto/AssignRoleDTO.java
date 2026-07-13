package ${package}.facade.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleDTO(
        @NotBlank String userId,
        @NotBlank String roleCode) {
}
