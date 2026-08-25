package top.egon.cola.organization.facade.teaching.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignUserToClassDTO(
        @NotNull @Positive Long gradeId,
        @NotNull @Positive Long userId,
        @NotNull @Positive Long schoolClassId) {
}
