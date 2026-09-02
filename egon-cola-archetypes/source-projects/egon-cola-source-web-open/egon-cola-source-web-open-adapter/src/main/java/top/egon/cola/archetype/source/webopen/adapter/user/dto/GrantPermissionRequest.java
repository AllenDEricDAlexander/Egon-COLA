package top.egon.cola.archetype.source.webopen.adapter.user.dto;

import jakarta.validation.constraints.NotBlank;

public record GrantPermissionRequest(@NotBlank String permissionCode) {
}
