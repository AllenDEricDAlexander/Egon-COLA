#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.entities.course.Course;
import ${package}.domain.service.exam.impl.ExamDomainServiceImpl;
import ${package}.domain.vos.course.CourseCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamAggregateTest {

    @Test
    void shouldRejectInvalidExamWindowAndPaperPoints() {
        var service = new ExamDomainServiceImpl();
        Course course = Course.create("course-1", new CourseCode("MATH-101"), "Math", 3);

        assertThrows(EvaluationDomainException.class, () -> service.createExam(
                "exam-1", course, "Midterm", Instant.EPOCH.plusSeconds(1), Instant.EPOCH));
        var exam = service.createExam(
                "exam-1", course, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        assertThrows(EvaluationDomainException.class,
                () -> service.attachPaper("paper-1", exam, "Paper", 0));
    }
}
