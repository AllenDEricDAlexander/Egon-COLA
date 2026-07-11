package ${package}.facade.dto.user;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleDTO(
        @NotBlank String userId,
        @NotBlank String roleCode) {
}
