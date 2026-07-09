package ${package}.application.teaching.validators;

import ${package}.application.teaching.command.CreateCourseCommand;
import ${package}.application.teaching.command.ScheduleCourseCommand;
import ${package}.application.teaching.manage.TeachingUseCaseException;
import ${package}.domain.teaching.service.CourseCacheService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeachingApplicationValidatorTest {
    private final CourseCacheService courseCacheService = mock(CourseCacheService.class);
    private final TeachingApplicationValidator validator = new TeachingApplicationValidator(courseCacheService);

    @Test
    void rejects_missing_operator_context() {
        CreateCourseCommand command = new CreateCourseCommand("math", "Mathematics", " ", "request-1");

        TeachingUseCaseException error = assertThrows(
                TeachingUseCaseException.class, () -> validator.validate(command));

        assertEquals("MISSING_OPERATOR", error.getCode());
    }

    @Test
    void rejects_duplicate_request() {
        ScheduleCourseCommand command = validCommand();
        when(courseCacheService.claimIdempotency("request-1", Duration.ofMinutes(5))).thenReturn(false);

        TeachingUseCaseException error = assertThrows(
                TeachingUseCaseException.class, () -> validator.validate(command));

        assertEquals("DUPLICATE_REQUEST", error.getCode());
    }

    @Test
    void rejects_invalid_schedule_interval() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 9, 1, 10, 0);
        ScheduleCourseCommand command = new ScheduleCourseCommand(
                "class-1", "course-math", startsAt, startsAt, "operator-1", "request-1");

        TeachingUseCaseException error = assertThrows(
                TeachingUseCaseException.class, () -> validator.validate(command));

        assertEquals("INVALID_SCHEDULE", error.getCode());
    }

    @Test
    void claims_valid_schedule_request() {
        ScheduleCourseCommand command = validCommand();
        when(courseCacheService.claimIdempotency("request-1", Duration.ofMinutes(5))).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(command));

        verify(courseCacheService).claimIdempotency("request-1", Duration.ofMinutes(5));
    }

    private ScheduleCourseCommand validCommand() {
        return new ScheduleCourseCommand(
                "class-1", "course-math",
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 1, 10, 0),
                "operator-1", "request-1");
    }
}
