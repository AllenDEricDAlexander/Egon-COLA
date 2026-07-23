package top.egon.cola.organization.facade.teaching.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassDTO(@NotBlank String name, @NotBlank String gradeCode) {
}
