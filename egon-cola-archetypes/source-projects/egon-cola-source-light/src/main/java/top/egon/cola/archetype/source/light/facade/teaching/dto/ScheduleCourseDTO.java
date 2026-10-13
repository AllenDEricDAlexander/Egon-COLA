package top.egon.cola.archetype.source.light.facade.teaching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.archetype.source.light.facade.validation.NativeRpcValidationGroup;

import top.egon.cola.component.common.core.pojo.BasePojo;
import java.time.Instant;

public record ScheduleCourseDTO(
        @NotNull(groups = NativeRpcValidationGroup.class) @Positive(groups = NativeRpcValidationGroup.class) Long schoolClassId,
        @NotNull(groups = NativeRpcValidationGroup.class) @Positive(groups = NativeRpcValidationGroup.class) Long courseId,
        @NotNull(groups = NativeRpcValidationGroup.class) Instant startsAt,
        @NotNull(groups = NativeRpcValidationGroup.class) Instant endsAt,
        @NotBlank(groups = NativeRpcValidationGroup.class) String operatorId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String requestId) implements BasePojo {
}
