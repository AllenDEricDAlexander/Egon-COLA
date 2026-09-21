package top.egon.cola.archetype.source.web.application.user.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserCommand(
        String requestId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 160) String email) {
}
