package top.egon.cola.archetype.source.lightopen.infrastructure.user.validators;

import jakarta.validation.Validation;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.archetype.source.lightopen.common.exception.UserDomainException;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.ExternalUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserInfrastructureValidatorTest {
    private final UserInfrastructureValidator validator = new UserInfrastructureValidator(new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()));

    @Test
    void rejects_missing_external_identifier_and_malformed_cache() {
        assertEquals("INVALID_EXTERNAL_USER", assertThrows(UserDomainException.class,
                () -> validator.validateExternalUser(new ExternalUser("ext-1", "Mario"), "other")).getStatus());
        assertEquals("INVALID_USER_CACHE",
                validator.invalidCachePayload("not-json", new IllegalArgumentException()).getStatus());
    }

    @Test
    void translates_jpa_uniqueness_failure() {
        assertEquals("USER_ALREADY_EXISTS", validator.persistenceFailure(
                new RuntimeException("unique constraint")).getStatus());
    }
}
