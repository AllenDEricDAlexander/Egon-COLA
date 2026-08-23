#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.course.entities.Course;
import ${package}.domain.exam.service.impl.ExamDomainServiceImpl;
import ${package}.domain.exam.service.impl.ScoreDomainServiceImpl;
import ${package}.domain.course.vos.CourseCode;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreDomainServiceTest {

    private final ExamDomainServiceImpl examService = new ExamDomainServiceImpl();
    private final ScoreDomainServiceImpl scoreService = new ScoreDomainServiceImpl();
    private ${package}.domain.exam.entities.Exam exam;
    private ${package}.domain.exam.entities.ExamPaper paper;

    @BeforeEach
    void setUp() {
        Course course = Course.create(1L, new CourseCode("MATH-101"), "Math", 3);
        exam = examService.createExam(
                1L, course, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        paper = examService.attachPaper(1L, exam, "Paper", 100);
        examService.publishExam(exam, paper);
    }

    @Test
    void shouldRejectDuplicateOrOutOfRangeScore() {
        assertThrows(EvaluationDomainException.class, () -> scoreService.recordScore(
                1L, exam, paper, 1L, 101, false));
        assertThrows(EvaluationDomainException.class, () -> scoreService.recordScore(
                2L, exam, paper, 1L, 90, true));
    }

    @Test
    void shouldRecordValidScore() {
        var score = scoreService.recordScore(
                1L, exam, paper, 1L, 90, false);

        assertEquals(90, score.getPoints().value());
        assertEquals("RECORDED", score.getStatus().name());
    }
}
