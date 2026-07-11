package ${package}.adapter.dto.user;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(@NotBlank String roleCode) {
}
