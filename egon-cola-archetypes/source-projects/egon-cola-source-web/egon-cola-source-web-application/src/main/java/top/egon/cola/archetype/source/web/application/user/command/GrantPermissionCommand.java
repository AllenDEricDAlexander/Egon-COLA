package top.egon.cola.archetype.source.web.application.user.command;

import jakarta.validation.constraints.NotBlank;

public record GrantPermissionCommand(
        String requestId,
        @NotBlank String roleCode,
        @NotBlank String permissionCode) {
}
