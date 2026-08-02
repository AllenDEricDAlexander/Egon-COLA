package top.egon.cola.platform.rbac3.admin.integration.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3ApplicationConfigurationTest {

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
}
