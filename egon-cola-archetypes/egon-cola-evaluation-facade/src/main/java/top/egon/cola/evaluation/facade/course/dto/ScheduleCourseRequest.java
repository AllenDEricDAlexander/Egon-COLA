package top.egon.cola.evaluation.facade.course.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.time.Instant;

public record ScheduleCourseRequest(
        @NotNull @Positive Long courseId,
        @NotNull @Positive Long classId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) implements Serializable {
}
