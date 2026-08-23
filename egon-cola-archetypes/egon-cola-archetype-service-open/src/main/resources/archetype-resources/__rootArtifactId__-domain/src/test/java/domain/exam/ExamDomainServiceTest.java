#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.course.entities.Course;
import ${package}.domain.exam.service.impl.ExamDomainServiceImpl;
import ${package}.domain.course.vos.CourseCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamDomainServiceTest {

    private final ExamDomainServiceImpl service = new ExamDomainServiceImpl();

    @Test
    void shouldRequirePaperBeforePublishingExam() {
        Course course = Course.create("course-1", new CourseCode("MATH-101"), "Math", 3);
        var exam = service.createExam(
                "exam-1", course, "Midterm",
                Instant.parse("2026-10-01T01:00:00Z"),
                Instant.parse("2026-10-01T03:00:00Z"));

        assertThrows(EvaluationDomainException.class, () -> service.publishExam(exam, null));
    }

    @Test
    void shouldPublishExamAndPaperTogether() {
        Course course = Course.create("course-1", new CourseCode("MATH-101"), "Math", 3);
        var exam = service.createExam(
                "exam-1", course, "Midterm",
                Instant.parse("2026-10-01T01:00:00Z"),
                Instant.parse("2026-10-01T03:00:00Z"));
        var paper = service.attachPaper("paper-1", exam, "Midterm paper", 100);

        service.publishExam(exam, paper);

        assertEquals("PUBLISHED", exam.getStatus().name());
        assertEquals("PUBLISHED", paper.getStatus().name());
    }
}
