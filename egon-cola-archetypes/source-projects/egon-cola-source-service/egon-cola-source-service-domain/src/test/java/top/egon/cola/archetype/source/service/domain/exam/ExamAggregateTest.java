package top.egon.cola.archetype.source.service.domain.exam;

import top.egon.cola.archetype.source.service.common.exception.EvaluationDomainException;
import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.service.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.service.domain.exam.enums.ExamStatus;
import top.egon.cola.archetype.source.service.domain.exam.validators.ExamDomainValidator;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
import jakarta.validation.Validation;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamAggregateTest {

    @Test
    void shouldRejectInvalidExamWindowAndPaperPoints() {
        var validator = new ExamDomainValidator(
                new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()));
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);

        assertThrows(EvaluationDomainException.class, () -> validator.validateExam(
                course, "Midterm", Instant.EPOCH.plusSeconds(1), Instant.EPOCH));
        assertThrows(EvaluationDomainException.class, () -> validator.validatePaper("Paper", 0));
    }
}
