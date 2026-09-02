package top.egon.cola.archetype.source.serviceopen.domain.exam;

import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationDomainException;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.Course;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ExamStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.validators.ExamDomainValidator;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ExamId;
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
