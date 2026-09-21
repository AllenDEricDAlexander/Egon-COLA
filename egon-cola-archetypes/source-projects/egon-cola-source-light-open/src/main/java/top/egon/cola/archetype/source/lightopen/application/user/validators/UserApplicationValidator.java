package top.egon.cola.archetype.source.lightopen.application.user.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** User use-case rules; the claim itself belongs to the idempotency service. */
@Component("userApplicationValidator")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class UserApplicationValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void validate(CreateUserCommand command) {
        validateBean(command);
        validateContext(command.operatorId(), command.idempotencyKey());
    }

    public void validate(AssignRoleCommand command) {
        validateBean(command);
        validateContext(command.operatorId(), command.idempotencyKey());
    }

    public void validate(GrantPermissionCommand command) {
        validateBean(command);
        validateContext(command.operatorId(), command.idempotencyKey());
    }

    private void validateContext(String operatorId, String idempotencyKey) {
        if (operatorId == null || operatorId.isBlank()) {
            throw new UserUseCaseException("MISSING_OPERATOR", "operator context is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new UserUseCaseException("MISSING_IDEMPOTENCY_KEY", "idempotency key is required");
        }
    }
}
