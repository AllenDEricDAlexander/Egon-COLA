package ${package}.adapter.teaching.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGradeRequest(@NotBlank String code, @NotBlank String name) {
}
