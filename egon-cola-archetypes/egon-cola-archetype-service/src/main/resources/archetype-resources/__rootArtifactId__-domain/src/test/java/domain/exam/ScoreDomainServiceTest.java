#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.entities.course.Course;
import ${package}.domain.service.exam.impl.ExamDomainServiceImpl;
import ${package}.domain.service.exam.impl.ScoreDomainServiceImpl;
import ${package}.domain.vos.course.CourseCode;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreDomainServiceTest {

    private final ExamDomainServiceImpl examService = new ExamDomainServiceImpl();
    private final ScoreDomainServiceImpl scoreService = new ScoreDomainServiceImpl();
    private ${package}.domain.entities.exam.Exam exam;
    private ${package}.domain.entities.exam.ExamPaper paper;

    @BeforeEach
    void setUp() {
        Course course = Course.create("course-1", new CourseCode("MATH-101"), "Math", 3);
        exam = examService.createExam(
                "exam-1", course, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        paper = examService.attachPaper("paper-1", exam, "Paper", 100);
        examService.publishExam(exam, paper);
    }

    @Test
    void shouldRejectDuplicateOrOutOfRangeScore() {
        assertThrows(EvaluationDomainException.class, () -> scoreService.recordScore(
                "score-1", exam, paper, "student-1", 101, false));
        assertThrows(EvaluationDomainException.class, () -> scoreService.recordScore(
                "score-2", exam, paper, "student-1", 90, true));
    }

    @Test
    void shouldRecordValidScore() {
        var score = scoreService.recordScore(
                "score-1", exam, paper, "student-1", 90, false);

        assertEquals(90, score.getPoints().value());
        assertEquals("RECORDED", score.getStatus().name());
    }
}
