package top.egon.cola.archetype.source.light.adapter.teaching.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassRequest(@NotBlank String name, @NotBlank String semester) {
}
