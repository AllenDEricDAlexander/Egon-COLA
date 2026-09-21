package top.egon.cola.archetype.source.webopen.adapter.user.pojo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 160) String email) implements BasePojo {
}
