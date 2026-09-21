package top.egon.cola.archetype.source.web.application.teaching.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignUserToClassCommand(
        String requestId,
        @NotNull @Positive Long gradeId,
        @NotNull @Positive Long schoolClassId,
        @NotNull @Positive Long userId) {
}
