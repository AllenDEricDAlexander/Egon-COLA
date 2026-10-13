package top.egon.cola.archetype.source.service.application.course.pojo.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record ScheduleCourseCommand(
        @NotNull @Positive Long courseId,
        @NotNull @Positive Long classId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {
    public ScheduleCourseCommand {
        startsAt = startsAt == null ? null : startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt == null ? null : endsAt.truncatedTo(ChronoUnit.MICROS);
    }
}
