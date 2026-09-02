package top.egon.cola.archetype.source.light.application.teaching.command;

import java.time.LocalDateTime;

public record ScheduleCourseCommand(
        Long schoolClassId,
        Long courseId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String operatorId,
        String idempotencyKey) {
    public ScheduleCourseCommand {
        if (schoolClassId == null || schoolClassId <= 0
                || courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("course and school class ids must be positive");
        }
    }
}
