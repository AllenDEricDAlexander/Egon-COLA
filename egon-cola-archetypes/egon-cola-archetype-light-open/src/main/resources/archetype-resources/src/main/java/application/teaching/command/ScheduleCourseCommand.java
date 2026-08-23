package ${package}.application.teaching.command;

import java.time.LocalDateTime;

public record ScheduleCourseCommand(
        String schoolClassId,
        String courseId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String operatorId,
        String idempotencyKey) {
}
