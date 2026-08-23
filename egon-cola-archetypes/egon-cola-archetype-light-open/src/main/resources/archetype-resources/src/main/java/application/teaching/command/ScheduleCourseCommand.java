package ${package}.application.teaching.command;

import java.time.LocalDateTime;

public record ScheduleCourseCommand(
        long schoolClassId,
        long courseId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String operatorId,
        String idempotencyKey) {
}
