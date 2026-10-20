package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityPersistenceAdapter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TenantIssuerServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthSecurityStateRepositoryImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl.OAuthSigningKeyRepositoryImpl;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class OAuthMethodValidationContractTest {

    @Test
    void inheritedMethodConstraintsHaveValidDeclarations() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Class<?> type : List.of(IdentityPersistenceAdapter.class, TenantIssuerServiceImpl.class,
                    OAuthSigningKeyRepositoryImpl.class, OAuthSecurityStateRepositoryImpl.class)) {
                assertThatCode(() -> validator.getConstraintsForClass(type)).doesNotThrowAnyException();
            }
        }
    }

    @Test
    void identityPortsRetainInputAndVersionValidation() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator().forExecutables();
            var users = mock(IdentityUserStore.class);
            assertThat(validator.validateParameters(users,
                    IdentityUserStore.class.getMethod("findByNormalizedUsername", String.class),
                    new Object[]{" "})).isNotEmpty();
            var credentials = mock(PasswordCredentialStore.class);
            assertThat(validator.validateParameters(credentials,
                    PasswordCredentialStore.class.getMethod("save",
                            top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential.class, long.class),
                    new Object[]{null, -1L})).hasSize(2);
        }
    }
}
