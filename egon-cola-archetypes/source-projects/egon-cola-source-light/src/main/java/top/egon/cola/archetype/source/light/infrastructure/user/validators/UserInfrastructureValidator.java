package top.egon.cola.archetype.source.light.infrastructure.user.validators;

import top.egon.cola.archetype.source.light.domain.user.exceptions.UserDomainException;
import top.egon.cola.archetype.source.light.domain.user.vos.ExternalUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("userInfrastructureValidator")
@Slf4j
public class UserInfrastructureValidator {
    public void validateExternalUser(ExternalUser user, String expectedExternalId) {
        if (user == null || !expectedExternalId.equals(user.externalId())) {
            throw new UserDomainException("INVALID_EXTERNAL_USER", "external user response is invalid");
        }
    }

    public UserDomainException invalidCachePayload(Object payload, Throwable cause) {
        return new UserDomainException("INVALID_USER_CACHE", "user cache payload is invalid", cause);
    }

    public UserDomainException persistenceFailure(Throwable cause) {
        String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase();
        String code = message.contains("unique") ? "USER_ALREADY_EXISTS" : "USER_PERSISTENCE_FAILED";
        return new UserDomainException(code, "user persistence failed", cause);
    }

    public void requirePublished(boolean published) {
        if (!published) {
            throw new UserDomainException("USER_EVENT_PUBLISH_FAILED", "user event publication failed");
        }
    }
}
