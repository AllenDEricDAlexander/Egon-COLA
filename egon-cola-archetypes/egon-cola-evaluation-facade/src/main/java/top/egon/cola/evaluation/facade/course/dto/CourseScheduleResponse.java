package top.egon.cola.evaluation.facade.course.dto;

import java.io.Serializable;
import java.time.Instant;

public record CourseScheduleResponse(
        String id,
        String courseId,
        String classId,
        Instant startsAt,
        Instant endsAt,
        String status) implements Serializable {
}
