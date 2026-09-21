package top.egon.cola.archetype.source.light.facade.teaching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.archetype.source.light.facade.validation.NativeRpcValidationGroup;

import top.egon.cola.component.common.core.pojo.BasePojo;
import java.time.LocalDateTime;

public record ScheduleCourseDTO(
        @NotNull(groups = NativeRpcValidationGroup.class) @Positive(groups = NativeRpcValidationGroup.class) Long schoolClassId,
        @NotNull(groups = NativeRpcValidationGroup.class) @Positive(groups = NativeRpcValidationGroup.class) Long courseId,
        @NotNull(groups = NativeRpcValidationGroup.class) LocalDateTime startsAt,
        @NotNull(groups = NativeRpcValidationGroup.class) LocalDateTime endsAt,
        @NotBlank(groups = NativeRpcValidationGroup.class) String operatorId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String requestId) implements BasePojo {
}
