#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course.validators;

import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.entities.CourseSchedule;
import java.time.Instant;
import java.util.List;

public final class CourseDomainValidator {

    public void validateSchedule(
            Course course,
            String classId,
            Instant startsAt,
            Instant endsAt,
            List<CourseSchedule> overlaps) {
        if (course == null || !course.isActive()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.COURSE_INACTIVE, "only active courses can be scheduled");
        }
        if (classId == null || classId.isBlank() || startsAt == null || endsAt == null
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
