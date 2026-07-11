#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.validators.exam;

import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.entities.course.Course;
import java.time.Instant;

public final class ExamDomainValidator {

    public void validateExam(Course course, String title, Instant startsAt, Instant endsAt) {
        if (course == null || !course.isActive()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.COURSE_INACTIVE, "exam requires an active course");
        }
        if (title == null || title.isBlank() || startsAt == null || endsAt == null
                || !startsAt.isBefore(endsAt)) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "invalid exam definition");
        }
    }

    public void validatePaper(String title, int totalPoints) {
        if (title == null || title.isBlank() || totalPoints <= 0) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "exam paper requires positive points");
        }
    }
}
