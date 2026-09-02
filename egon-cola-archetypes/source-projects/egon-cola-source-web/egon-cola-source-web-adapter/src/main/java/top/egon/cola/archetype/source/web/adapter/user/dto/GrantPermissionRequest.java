package top.egon.cola.archetype.source.web.adapter.user.dto;

import jakarta.validation.constraints.NotBlank;

public record GrantPermissionRequest(@NotBlank String permissionCode) {
}
