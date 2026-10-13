package top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ScheduleCourseCommand(
        @NotNull @Positive Long schoolClassId,
        @NotNull @Positive Long courseId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        String operatorId,
        String idempotencyKey) {
    public ScheduleCourseCommand {
        startsAt = startsAt == null ? null : startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt == null ? null : endsAt.truncatedTo(ChronoUnit.MICROS);
        if (schoolClassId == null || schoolClassId <= 0
                || courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("course and school class ids must be positive");
        }
    }
}
