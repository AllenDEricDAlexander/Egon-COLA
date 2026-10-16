package top.egon.cola.archetype.source.lightopen.adapter.user.pojo.dto;

import jakarta.validation.constraints.NotBlank;

public record GrantPermissionDTO(@NotBlank String permissionCode) {
}
