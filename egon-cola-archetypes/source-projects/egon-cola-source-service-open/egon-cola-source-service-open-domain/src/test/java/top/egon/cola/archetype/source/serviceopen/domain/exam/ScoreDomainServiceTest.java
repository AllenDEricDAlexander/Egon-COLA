package top.egon.cola.archetype.source.serviceopen.domain.exam;

import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationDomainException;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.Course;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ExamPaperStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ExamStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.validators.ScoreDomainValidator;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ExamId;
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
