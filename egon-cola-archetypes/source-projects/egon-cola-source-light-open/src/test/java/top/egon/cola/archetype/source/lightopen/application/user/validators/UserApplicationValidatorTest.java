package top.egon.cola.archetype.source.lightopen.application.user.validators;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The claim moved to the idempotency service, so this fixture only covers stateless use-case rules. */
class UserApplicationValidatorTest {
    private final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final UserApplicationValidator validator =
            new UserApplicationValidator(new ValidationUtils(validators.getValidator()));

    @AfterEach
    void closeValidationFactory() {
        validators.close();
    }

    @Test
    void rejects_missing_operator_context() {
        CreateUserCommand command = new CreateUserCommand(
                "ext-1", "Mario", "mario@example.com", " ", "request-1");

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> validator.validate(command));

        assertEquals("MISSING_OPERATOR", error.getStatus());
    }

    @Test
    void rejects_missing_request_context() {
        AssignRoleCommand command = new AssignRoleCommand(1001L, "teacher", "operator-1", " ");

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> validator.validate(command));

        assertEquals("MISSING_IDEMPOTENCY_KEY", error.getStatus());
    }

    @Test
    void accepts_a_well_formed_grant_request() {
        GrantPermissionCommand command = new GrantPermissionCommand(
                "teacher", "course:read", "operator-1", "request-1");

        assertDoesNotThrow(() -> validator.validate(command));
    }
}
