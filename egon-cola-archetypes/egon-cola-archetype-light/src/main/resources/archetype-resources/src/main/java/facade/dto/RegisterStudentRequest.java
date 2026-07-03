package ${package}.facade.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterStudentRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 64, message = "name length must be less than or equal to 64")
        String name,
        @NotBlank(message = "email must not be blank")
        @Email(message = "email format is invalid")
        String email
) {
}
