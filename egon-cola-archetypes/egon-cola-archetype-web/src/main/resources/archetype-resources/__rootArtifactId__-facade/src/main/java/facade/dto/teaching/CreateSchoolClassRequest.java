package ${package}.facade.dto.teaching;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassRequest(@NotBlank String name, @NotBlank String gradeName) {
}
