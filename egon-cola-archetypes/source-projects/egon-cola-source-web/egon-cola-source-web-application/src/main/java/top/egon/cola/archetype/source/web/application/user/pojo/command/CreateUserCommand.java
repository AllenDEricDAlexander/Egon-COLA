package top.egon.cola.archetype.source.web.application.user.pojo.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateUserCommand(
        String requestId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 160) String email) implements BasePojo {
}
