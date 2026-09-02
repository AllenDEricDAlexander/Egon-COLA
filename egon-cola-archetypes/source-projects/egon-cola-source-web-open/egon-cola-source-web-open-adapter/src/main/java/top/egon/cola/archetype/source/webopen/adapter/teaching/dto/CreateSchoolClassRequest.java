package top.egon.cola.archetype.source.webopen.adapter.teaching.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassRequest(@NotBlank String name, @NotBlank String gradeCode) {
}
