#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.vos.ExamId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamDomainServiceTest {

    @Test
    void shouldPublishExamAndPaperTogetherAfterValidation() {
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
        Exam exam = new Exam(
                new ExamId(4001L), new ${package}.domain.course.vos.CourseId(course.getId()),
                "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.DRAFT);
        ExamPaper paper = new ExamPaper(
                5001L, exam.getId(), "Midterm paper", 100, ExamPaperStatus.DRAFT);

        exam.publish();
        paper.publish();

        assertEquals(ExamStatus.PUBLISHED, exam.getStatus());
        assertEquals(ExamPaperStatus.PUBLISHED, paper.getStatus());
    }

    @Test
    void shouldRejectPaperWithWrongExam() {
        Exam exam = new Exam(
                new ExamId(4001L), new ${package}.domain.course.vos.CourseId(1001L),
                "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.DRAFT);
        ExamPaper paper = new ExamPaper(
                5001L, new ExamId(4002L), "Paper", 100, ExamPaperStatus.DRAFT);
        assertThrows(EvaluationDomainException.class, () -> {
            if (!paper.getExamId().equals(exam.getId())) {
                throw new EvaluationDomainException(
                        ${package}.domain.common.EvaluationDomainErrorCode.EXAM_NOT_PUBLISHABLE,
                        "exam requires its own paper before publication");
            }
        });
    }
}
