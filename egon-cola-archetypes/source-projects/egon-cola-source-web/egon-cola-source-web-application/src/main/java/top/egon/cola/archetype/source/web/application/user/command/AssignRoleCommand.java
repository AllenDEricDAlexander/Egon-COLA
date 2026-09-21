package top.egon.cola.archetype.source.web.application.user.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignRoleCommand(
        String requestId,
        @NotNull @Positive Long userId,
        @NotBlank String roleCode) {
}
