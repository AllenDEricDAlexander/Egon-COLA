package ${package}.facade.teaching.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ScheduleCourseDTO(
        String schoolClassId,
        String courseId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String operatorId,
        String requestId) implements Serializable {
}
