package top.egon.cola.archetype.source.light.adapter.user.pojo.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(@NotBlank String roleCode) {
}
