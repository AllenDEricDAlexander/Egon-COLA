package ${package}.application.user.validators;

import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.manage.UserUseCaseException;
import ${package}.domain.user.service.UserCacheService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserApplicationValidatorTest {
    private final UserCacheService userCacheService = mock(UserCacheService.class);
    private final UserApplicationValidator validator = new UserApplicationValidator(userCacheService);

    @Test
    void rejects_missing_operator_context() {
        CreateUserCommand command = new CreateUserCommand(
                "ext-1", "Mario", "mario@example.com", " ", "request-1");

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> validator.validate(command));

        assertEquals("MISSING_OPERATOR", error.getCode());
    }

    @Test
    void rejects_duplicate_request() {
        CreateUserCommand command = command();
        when(userCacheService.claimIdempotency("request-1", Duration.ofMinutes(5))).thenReturn(false);

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> validator.validate(command));

        assertEquals("DUPLICATE_REQUEST", error.getCode());
    }

    @Test
    void claims_valid_request_key() {
        CreateUserCommand command = command();
        when(userCacheService.claimIdempotency("request-1", Duration.ofMinutes(5))).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(command));

        verify(userCacheService).claimIdempotency("request-1", Duration.ofMinutes(5));
    }

    private CreateUserCommand command() {
        return new CreateUserCommand(
                "ext-1", "Mario", "mario@example.com", "operator-1", "request-1");
    }
}
