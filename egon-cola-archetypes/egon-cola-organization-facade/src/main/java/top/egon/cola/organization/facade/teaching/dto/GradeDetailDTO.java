package top.egon.cola.organization.facade.teaching.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GradeDetailDTO(@NotNull @Positive Long id, String code, String name, String status) {
}
