package top.egon.cola.archetype.source.service.application.course.result;

import java.time.Instant;

public record CourseScheduleResult(
        Long id,
        Long courseId,
        Long classId,
        Instant startsAt,
        Instant endsAt,
        String status) {
}
