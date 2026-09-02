package top.egon.cola.archetype.source.web.adapter.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(@NotBlank String roleCode) {
}
