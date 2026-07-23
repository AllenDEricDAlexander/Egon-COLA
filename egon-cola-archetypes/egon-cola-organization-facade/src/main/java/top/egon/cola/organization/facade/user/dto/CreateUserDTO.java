package top.egon.cola.organization.facade.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 160) String email) {
}
