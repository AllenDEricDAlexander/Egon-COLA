package ${package}.facade.dto.teaching;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSchoolClassRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 64, message = "name length must be less than or equal to 64")
        String name,
        @NotBlank(message = "gradeName must not be blank")
        @Size(max = 64, message = "gradeName length must be less than or equal to 64")
        String gradeName
) {
}
