package top.egon.cola.platform.idp.admin.oauth.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;
import top.egon.cola.platform.idp.admin.oauth.service.impl.PrivateKeyJwtAuthenticator;
import top.egon.cola.platform.idp.admin.resource.repo.JpaClientCredentialStore;
import top.egon.cola.platform.idp.core.port.ClientAssertionReplayStore;
import top.egon.cola.platform.idp.core.port.ClientCredentialStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;

import java.time.Clock;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthConfigTest {

    @Test
    void tokenEndpointAuthenticatorIsThePrimaryAuthenticator() throws Exception {
        var method = OAuthConfig.class.getDeclaredMethod(
                "privateKeyJwtAuthenticator",
                OAuthClientStore.class,
                ClientCredentialStore.class,
                ClientAssertionReplayStore.class,
                String.class,
                Clock.class
        );

        assertThat(method.isAnnotationPresent(Primary.class)).isTrue();
        assertThat(method.getReturnType())
                .isEqualTo(PrivateKeyJwtAuthenticator.class);
    }

    @Test
    void transactionalClientCredentialStoreCanBeClassProxied() {
        assertThat(Modifier.isFinal(
                JpaClientCredentialStore.class.getModifiers()
        )).isFalse();
    }
}
