package top.egon.light.facade.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterStudentRequest(
        @NotBlank String name,
        @Email @NotBlank String email
) {
}
