package top.egon.cola.archetype.source.webopen.adapter.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(@NotBlank String roleCode) {
}
