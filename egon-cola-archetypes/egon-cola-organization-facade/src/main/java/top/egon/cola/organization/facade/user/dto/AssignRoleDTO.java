package top.egon.cola.organization.facade.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleDTO(
        @NotBlank String userId,
        @NotBlank String roleCode) {
}
