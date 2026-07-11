package ${package}.adapter.dto.teaching;

import jakarta.validation.constraints.NotBlank;

public record CreateGradeRequest(@NotBlank String code, @NotBlank String name) {
}
