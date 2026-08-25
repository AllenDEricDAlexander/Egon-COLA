#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.validators.ExamDomainValidator;
import ${package}.domain.exam.vos.ExamId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamAggregateTest {

    @Test
    void shouldRejectInvalidExamWindowAndPaperPoints() {
        var validator = new ExamDomainValidator();
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);

        assertThrows(EvaluationDomainException.class, () -> validator.validateExam(
                course, "Midterm", Instant.EPOCH.plusSeconds(1), Instant.EPOCH));
        assertThrows(EvaluationDomainException.class, () -> validator.validatePaper("Paper", 0));
    }
}
