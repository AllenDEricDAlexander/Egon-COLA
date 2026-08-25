package ${package}.facade.teaching.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ScheduleCourseDTO(
        Long schoolClassId,
        Long courseId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String operatorId,
        String requestId) implements Serializable {
}
