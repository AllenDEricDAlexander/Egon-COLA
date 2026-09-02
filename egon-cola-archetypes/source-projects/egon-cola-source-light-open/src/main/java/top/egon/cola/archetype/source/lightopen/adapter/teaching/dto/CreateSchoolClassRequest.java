package top.egon.cola.archetype.source.lightopen.adapter.teaching.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassRequest(@NotBlank String name, @NotBlank String semester) {
}
