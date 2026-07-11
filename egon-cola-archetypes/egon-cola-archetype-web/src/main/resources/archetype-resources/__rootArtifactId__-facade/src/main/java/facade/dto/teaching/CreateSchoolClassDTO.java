package ${package}.facade.dto.teaching;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassDTO(@NotBlank String name, @NotBlank String gradeCode) {
}
