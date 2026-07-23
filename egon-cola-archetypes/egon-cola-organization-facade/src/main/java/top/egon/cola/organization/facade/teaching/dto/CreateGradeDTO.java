package top.egon.cola.organization.facade.teaching.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGradeDTO(@NotBlank String code, @NotBlank String name) {
}
