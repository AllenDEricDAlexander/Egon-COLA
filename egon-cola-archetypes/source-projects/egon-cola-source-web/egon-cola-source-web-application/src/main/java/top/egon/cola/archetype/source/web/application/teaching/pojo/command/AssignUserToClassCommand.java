package top.egon.cola.archetype.source.web.application.teaching.pojo.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record AssignUserToClassCommand(
        String requestId,
        @NotNull @Positive Long gradeId,
        @NotNull @Positive Long schoolClassId,
        @NotNull @Positive Long userId) implements BasePojo {
}
