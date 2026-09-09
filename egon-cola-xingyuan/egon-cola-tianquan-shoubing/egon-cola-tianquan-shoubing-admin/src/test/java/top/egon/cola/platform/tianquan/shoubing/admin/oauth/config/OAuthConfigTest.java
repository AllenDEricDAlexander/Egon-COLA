package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientSecretRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.ClientSecretBasicAuthenticator;
import top.egon.cola.platform.tianquan.shoubing.core.port.OAuthClientStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthConfigTest {

    @Test
    void tokenEndpointAuthenticatorIsThePrimaryAuthenticator() throws Exception {
        var method = OAuthConfig.class.getDeclaredMethod(
                "clientSecretBasicAuthenticator",
                IdentityClientRepository.class,
                IdentityClientSecretRepository.class,
                PasswordHashPort.class
        );

        assertThat(method.isAnnotationPresent(Primary.class)).isTrue();
        assertThat(method.getReturnType())
                .isEqualTo(ClientSecretBasicAuthenticator.class);
    }
}
