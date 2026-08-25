package ${package}.application.user.validators;

import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.manage.UserUseCaseException;
import ${package}.domain.user.client.UserCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Lazy
@RequiredArgsConstructor
public class UserApplicationValidator {
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(5);

    @Qualifier("userCachePort")
    private final UserCachePort userCachePort;

    public void validate(CreateUserCommand command) {
        validateContext(command.operatorId(), command.idempotencyKey());
    }

    public void validate(AssignRoleCommand command) {
        validateContext(command.operatorId(), command.idempotencyKey());
    }

    public void validate(GrantPermissionCommand command) {
        validateContext(command.operatorId(), command.idempotencyKey());
    }

    private void validateContext(String operatorId, String idempotencyKey) {
        if (operatorId == null || operatorId.isBlank()) {
            throw new UserUseCaseException("MISSING_OPERATOR", "operator context is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new UserUseCaseException("MISSING_IDEMPOTENCY_KEY", "idempotency key is required");
        }
        if (!userCachePort.claimIdempotency(idempotencyKey, IDEMPOTENCY_TTL)) {
            throw new UserUseCaseException("DUPLICATE_REQUEST", "request was already processed");
        }
    }
}
