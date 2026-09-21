package top.egon.cola.archetype.source.webopen.application.user.pojo.command;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record GrantPermissionCommand(
        String requestId,
        @NotBlank String roleCode,
        @NotBlank String permissionCode) implements BasePojo {
}
