package top.egon.cola.archetype.source.light.application.teaching.validators;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The claim moved to the idempotency service, so this fixture only covers stateless use-case rules. */
class TeachingApplicationValidatorTest {
    private final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final TeachingApplicationValidator validator =
            new TeachingApplicationValidator(new ValidationUtils(validators.getValidator()));

    @AfterEach
    void closeValidationFactory() {
        validators.close();
    }

    @Test
    void rejects_missing_operator_context() {
        CreateCourseCommand command = new CreateCourseCommand("math", "Mathematics", " ", "request-1");

        TeachingUseCaseException error = assertThrows(
                TeachingUseCaseException.class, () -> validator.validate(command));

        assertEquals("MISSING_OPERATOR", error.getStatus());
    }

    @Test
    void rejects_missing_idempotency_key() {
        CreateSchoolClassCommand command = new CreateSchoolClassCommand("Class One", "2026-FALL", "operator-1", " ");

        TeachingUseCaseException error = assertThrows(
                TeachingUseCaseException.class, () -> validator.validate(command));

        assertEquals("MISSING_IDEMPOTENCY_KEY", error.getStatus());
    }

    @Test
    void rejects_invalid_schedule_interval() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 9, 1, 10, 0);
        ScheduleCourseCommand command = new ScheduleCourseCommand(
                1003L, 1002L, startsAt, startsAt, "operator-1", "request-1");

        TeachingUseCaseException error = assertThrows(
                TeachingUseCaseException.class, () -> validator.validate(command));

        assertEquals("INVALID_SCHEDULE", error.getStatus());
    }

    @Test
    void accepts_a_well_formed_schedule_request() {
        ScheduleCourseCommand command = new ScheduleCourseCommand(
                1003L, 1002L,
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 1, 10, 0),
                "operator-1", "request-1");

        assertDoesNotThrow(() -> validator.validate(command));
    }
}
