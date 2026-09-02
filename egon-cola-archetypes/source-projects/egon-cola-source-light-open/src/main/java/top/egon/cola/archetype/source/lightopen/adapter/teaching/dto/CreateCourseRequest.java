package top.egon.cola.archetype.source.lightopen.adapter.teaching.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(@NotBlank String code, @NotBlank String name) {
}
