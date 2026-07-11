package ${package}.facade.dto.teaching;

import jakarta.validation.constraints.NotBlank;

public record CreateGradeDTO(@NotBlank String code, @NotBlank String name) {
}
