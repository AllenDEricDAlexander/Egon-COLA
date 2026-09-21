package top.egon.cola.archetype.source.light.adapter.user.pojo.dto;

import jakarta.validation.constraints.NotBlank;

public record GrantPermissionRequest(@NotBlank String permissionCode) {
}
