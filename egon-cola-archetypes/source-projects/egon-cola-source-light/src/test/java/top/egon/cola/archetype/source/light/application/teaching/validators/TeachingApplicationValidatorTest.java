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

import java.time.Instant;

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
    void command_micro_normalization_precedes_window_validation() {
        ScheduleCourseCommand command = new ScheduleCourseCommand(1003L, 1002L,
                Instant.parse("2026-10-13T06:07:08.123456100Z"), Instant.parse("2026-10-13T06:07:08.123456900Z"),
                "operator-1", "request-1");
        assertEquals(Instant.parse("2026-10-13T06:07:08.123456Z"), command.startsAt());
        assertEquals(command.startsAt(), command.endsAt());
        TeachingUseCaseException error = assertThrows(TeachingUseCaseException.class, () -> validator.validate(command));
        assertEquals("INVALID_SCHEDULE", error.getStatus());
        ScheduleCourseCommand missing = new ScheduleCourseCommand(1003L, 1002L, null, null, "operator-1", "request-1");
        assertEquals(null, missing.startsAt());
        assertEquals("INVALID_SCHEDULE", assertThrows(TeachingUseCaseException.class, () -> validator.validate(missing)).getStatus());
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
        Instant startsAt = Instant.parse("2026-09-01T10:00:00Z");
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
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                "operator-1", "request-1");

        assertDoesNotThrow(() -> validator.validate(command));
    }
}
