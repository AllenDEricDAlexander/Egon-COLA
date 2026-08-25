#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam;

import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.validators.ScoreDomainValidator;
import ${package}.domain.exam.vos.ExamId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreDomainServiceTest {

    @Test
    void shouldRejectDuplicateOrOutOfRangeScore() {
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
        Exam exam = new Exam(new ExamId(4001L), new CourseId(course.getId()), "Midterm",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.PUBLISHED);
        ExamPaper paper = new ExamPaper(5001L, exam.getId(), "Paper", 100, ExamPaperStatus.PUBLISHED);
        ScoreDomainValidator validator = new ScoreDomainValidator();

        assertThrows(EvaluationDomainException.class,
                () -> validator.validate(exam, paper, 6001L, 101, false));
        assertThrows(EvaluationDomainException.class,
                () -> validator.validate(exam, paper, 6001L, 90, true));
    }
}
