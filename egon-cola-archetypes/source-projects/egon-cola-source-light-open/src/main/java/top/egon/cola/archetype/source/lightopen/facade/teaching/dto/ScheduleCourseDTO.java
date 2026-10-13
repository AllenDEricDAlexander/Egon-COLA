package top.egon.cola.archetype.source.lightopen.facade.teaching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.component.common.core.pojo.BasePojo;

import java.time.Instant;

public record ScheduleCourseDTO(
        @NotNull @Positive Long schoolClassId,
        @NotNull @Positive Long courseId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotBlank String operatorId,
        @NotBlank String requestId) implements BasePojo {
}
