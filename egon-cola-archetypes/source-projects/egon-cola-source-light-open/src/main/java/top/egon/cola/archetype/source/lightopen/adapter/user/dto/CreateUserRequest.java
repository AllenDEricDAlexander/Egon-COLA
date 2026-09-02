package top.egon.cola.archetype.source.lightopen.adapter.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String externalId,
        @NotBlank String name,
        @NotBlank @Email String email) {
}
