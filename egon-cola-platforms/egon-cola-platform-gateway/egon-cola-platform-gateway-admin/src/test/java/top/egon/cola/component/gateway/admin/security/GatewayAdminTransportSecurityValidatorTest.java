package top.egon.cola.component.gateway.admin.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayAdminTransportSecurityValidatorTest {

    private static final String PREFIX =
            "gateway.admin.transport-security";

    @Test
    void acceptsCompleteReloadablePemMtlsBundle() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(PREFIX + ".mode", "MTLS")
                .withProperty("server.ssl.enabled", "true")
                .withProperty("server.ssl.client-auth", "need")
                .withProperty("server.ssl.bundle", "management")
                .withProperty(
                        "spring.ssl.bundle.pem.management."
                                + "keystore.certificate",
                        "file:/run/secrets/server.crt"
                )
                .withProperty(
                        "spring.ssl.bundle.pem.management."
                                + "keystore.private-key",
                        "file:/run/secrets/server.key"
                )
                .withProperty(
                        "spring.ssl.bundle.pem.management."
                                + "truststore.certificate",
                        "file:/run/secrets/ca.crt"
                );

        assertThatCode(() ->
                GatewayAdminTransportSecurityValidator.validate(
                        environment,
                        PREFIX
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void rejectsMtlsWithoutRequiredClientAuthentication() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(PREFIX + ".mode", "MTLS")
                .withProperty("server.ssl.enabled", "true")
                .withProperty("server.ssl.client-auth", "want");

        assertThatThrownBy(() ->
                GatewayAdminTransportSecurityValidator.validate(
                        environment,
                        PREFIX
                )
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("client-auth=need");
    }
}
