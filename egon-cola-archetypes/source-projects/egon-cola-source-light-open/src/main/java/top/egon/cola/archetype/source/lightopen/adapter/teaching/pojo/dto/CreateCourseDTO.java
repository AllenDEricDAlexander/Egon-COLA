package top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseDTO(@NotBlank String code, @NotBlank String name) {
}
