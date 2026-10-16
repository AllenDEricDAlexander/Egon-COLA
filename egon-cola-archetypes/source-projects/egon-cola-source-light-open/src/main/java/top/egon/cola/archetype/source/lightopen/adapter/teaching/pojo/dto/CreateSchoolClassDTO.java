package top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassDTO(@NotBlank String name, @NotBlank String semester) {
}
