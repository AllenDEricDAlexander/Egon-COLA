package ${package}.domain.teaching.vos;

import ${package}.domain.teaching.exceptions.TeachingDomainException;

import java.time.LocalDateTime;
import java.util.Objects;

public record CourseSchedule(CourseCode courseCode, LocalDateTime startsAt, LocalDateTime endsAt) {
    public CourseSchedule {
        Objects.requireNonNull(courseCode);
        Objects.requireNonNull(startsAt);
        Objects.requireNonNull(endsAt);
        if (!startsAt.isBefore(endsAt)) {
            throw new TeachingDomainException("INVALID_SCHEDULE", "startsAt must be before endsAt");
        }
    }

    public boolean overlaps(CourseSchedule other) {
        return startsAt.isBefore(other.endsAt()) && other.startsAt().isBefore(endsAt);
    }
}
