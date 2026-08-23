package ${package}.infrastructure.user.validators;

import ${package}.domain.user.exceptions.UserDomainException;
import ${package}.domain.user.vos.ExternalUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserInfrastructureValidatorTest {
    private final UserInfrastructureValidator validator = new UserInfrastructureValidator();

    @Test
    void rejects_missing_external_identifier_and_malformed_cache() {
        assertEquals("INVALID_EXTERNAL_USER", assertThrows(UserDomainException.class,
                () -> validator.validateExternalUser(new ExternalUser("ext-1", "Mario"), "other")).getCode());
        assertEquals("INVALID_USER_CACHE",
                validator.invalidCachePayload("not-json", new IllegalArgumentException()).getCode());
    }

    @Test
    void translates_jpa_uniqueness_failure() {
        assertEquals("USER_ALREADY_EXISTS", validator.persistenceFailure(
                new RuntimeException("unique constraint")).getCode());
    }
}
