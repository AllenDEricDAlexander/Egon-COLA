package top.egon.cola.archetype.source.webopen.application.user.pojo.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record AssignRoleCommand(
        String requestId,
        @NotNull @Positive Long userId,
        @NotBlank String roleCode) implements BasePojo {
}
