package top.egon.cola.platform.rbac3.admin.integration.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3ApplicationConfigurationTest {

    @Test
    void developmentRoleAutoActivationRequiresTheLocalProfile() {
        var configuration = new Rbac3ApplicationConfiguration();
        MockEnvironment local = developmentAutoActivationEnvironment("local");
        MockEnvironment nonLocal = developmentAutoActivationEnvironment("staging");

        assertThat(configuration.developmentRoleAutoActivationEnabled(local)).isTrue();
        assertThat(configuration.developmentRoleAutoActivationEnabled(nonLocal)).isFalse();
    }

    @Test
    void providesPasswordEncoderForBootstrapAndPasswordAuthentication() {
        PasswordEncoder encoder = new Rbac3ApplicationConfiguration()
                .rbac3PasswordEncoder();

        String encoded = encoder.encode("correct horse battery staple");

        assertThat(encoded).startsWith("$2");
        assertThat(encoder.matches(
                "correct horse battery staple",
                encoded
        )).isTrue();
        assertThat(encoder.matches("wrong password", encoded)).isFalse();
    }

    private MockEnvironment developmentAutoActivationEnvironment(String profile) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("egon.rbac3.development-bootstrap.enabled", "true")
                .withProperty(
                        "egon.rbac3.development-bootstrap.auto-activate-local-admin-roles",
                        "true");
        environment.setActiveProfiles(profile);
        return environment;
    }
}
