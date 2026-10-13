package top.egon.cola.archetype.source.lightopen.domain.teaching.vos;

import top.egon.cola.archetype.source.lightopen.common.exception.TeachingDomainException;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record CourseScheduleBO(@NotNull CourseCode courseCode, @NotNull Instant startsAt, @NotNull Instant endsAt) {
    public CourseScheduleBO {
        Objects.requireNonNull(courseCode);
        Objects.requireNonNull(startsAt);
        Objects.requireNonNull(endsAt);
        startsAt = startsAt.truncatedTo(ChronoUnit.MICROS);
        endsAt = endsAt.truncatedTo(ChronoUnit.MICROS);
        if (!startsAt.isBefore(endsAt)) {
            throw new TeachingDomainException("INVALID_SCHEDULE", "startsAt must be before endsAt");
        }
    }

    public boolean overlaps(CourseScheduleBO other) {
        return startsAt.isBefore(other.endsAt()) && other.startsAt().isBefore(endsAt);
    }
}
