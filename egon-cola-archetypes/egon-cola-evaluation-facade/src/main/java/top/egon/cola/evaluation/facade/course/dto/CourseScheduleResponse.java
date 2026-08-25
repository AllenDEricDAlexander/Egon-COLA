package top.egon.cola.evaluation.facade.course.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.time.Instant;

public record CourseScheduleResponse(
        @NotNull @Positive Long id,
        @NotNull @Positive Long courseId,
        @NotNull @Positive Long classId,
        Instant startsAt,
        Instant endsAt,
        String status) implements Serializable {
}
