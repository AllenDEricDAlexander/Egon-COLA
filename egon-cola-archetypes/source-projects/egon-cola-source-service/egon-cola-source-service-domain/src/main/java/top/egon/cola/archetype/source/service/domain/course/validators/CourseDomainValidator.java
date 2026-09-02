package top.egon.cola.archetype.source.service.domain.course.validators;

import top.egon.cola.archetype.source.service.domain.common.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.service.domain.common.EvaluationDomainException;
import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import java.time.Instant;
import java.util.List;

public final class CourseDomainValidator {

    public void validateSchedule(
            Course course,
            Long classId,
            Instant startsAt,
            Instant endsAt,
            List<CourseSchedule> overlaps) {
        if (course == null || !course.isActive()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.COURSE_INACTIVE, "only active courses can be scheduled");
        }
        if (classId == null || classId <= 0 || startsAt == null || endsAt == null
                || !startsAt.isBefore(endsAt)) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "invalid course schedule");
        }
        if (overlaps != null && overlaps.stream().anyMatch(it -> it.overlaps(startsAt, endsAt))) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.SCHEDULE_CONFLICT, "course schedule overlaps");
        }
    }
}
