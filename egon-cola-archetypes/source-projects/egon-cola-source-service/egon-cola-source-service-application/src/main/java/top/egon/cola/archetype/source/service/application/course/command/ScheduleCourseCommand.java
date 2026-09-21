package top.egon.cola.archetype.source.service.application.course.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record ScheduleCourseCommand(
        @NotNull @Positive Long courseId,
        @NotNull @Positive Long classId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {
}
