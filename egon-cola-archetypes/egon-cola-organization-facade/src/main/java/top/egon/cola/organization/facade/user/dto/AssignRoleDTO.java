package top.egon.cola.organization.facade.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignRoleDTO(
        @NotNull @Positive Long userId,
        @NotBlank String roleCode) {
}
