package top.egon.cola.archetype.source.webopen.adapter.teaching.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGradeRequest(@NotBlank String code, @NotBlank String name) {
}
