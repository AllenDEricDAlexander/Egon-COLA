package top.egon.cola.platform.idp.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.idp.starter.state.IdentityUserStateReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdpStarterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            IdpStarterAutoConfiguration.class))
                    .withBean(ObjectMapper.class, ObjectMapper::new)
                    .withBean(RedissonClient.class,
                            () -> mock(RedissonClient.class))
                    .withBean(DdcAdmissionTicketSupplier.class,
                            () -> (biz, app, env, instance) -> {
                                throw new IllegalStateException("not invoked");
                            })
                    .withPropertyValues(
                            "egon.cola.platform.idp.enabled=true",
                            "egon.cola.platform.idp.issuer=https://idp.local",
                            "egon.cola.platform.idp.jwk-set-uri=https://idp.local/oauth2/jwks",
                            "egon.cola.platform.idp.audiences[0]=egon-api",
                            "egon.cola.platform.idp.client-ids[0]=gateway-admin");

    @Test
    void providesIdentityOnlyFilterBeforeRbac3Filter() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IdentityUserStateReader.class);
            assertThat(context).hasSingleBean(IdpJwtVerifier.class);
            assertThat(context).hasSingleBean(IdpBearerAuthenticationFilter.class);
            FilterRegistrationBean<?> registration = context.getBean(
                    "idpBearerFilterRegistration",
                    FilterRegistrationBean.class);
            assertThat(registration.getOrder()).isEqualTo(-102);
        });
    }

    @Test
    void remainsDisabledByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class))
                .run(context -> assertThat(context)
                        .doesNotHaveBean(IdpJwtVerifier.class));
    }

    @Test
    void providesAdmissionTicketSupplierWhenAllMachineIdentityIsConfigured()
            throws Exception {
        Path privateKey = ownerOnlyPrivateKey();

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "egon.cola.platform.idp.enabled=true",
                        "egon.cola.platform.idp.issuer=https://idp.local",
                        "egon.cola.platform.idp.jwk-set-uri=https://idp.local/oauth2/jwks",
                        "egon.cola.platform.idp.audiences[0]=https://api.example/idp",
                        "egon.cola.platform.idp.client-ids[0]=gateway-admin",
                        "egon.cola.platform.idp.admission.resource-server-id=rs-idp-prod",
                        "egon.cola.platform.idp.admission.resource-uri=https://api.example/idp",
                        "egon.cola.platform.idp.admission.biz-code=platform",
                        "egon.cola.platform.idp.admission.app-code=idp",
                        "egon.cola.platform.idp.admission.environment=prod",
                        "egon.cola.platform.idp.admission.instance-id=idp-local-8080",
                        "egon.cola.platform.idp.admission.management-client-id=idp-service",
                        "egon.cola.platform.idp.admission.kid=idp-service-2026-08",
                        "egon.cola.platform.idp.admission.private-key-path="
                                + privateKey,
                        "egon.cola.platform.idp.admission.endpoint=https://idp.local/oauth2/resource-server-admission",
                        "egon.cola.platform.idp.admission.renewal-skew=30s")
                .run(context -> assertThat(context)
                        .hasSingleBean(DdcAdmissionTicketSupplier.class));
    }

    private static Path ownerOnlyPrivateKey() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        byte[] privateKey = generator.generateKeyPair()
                .getPrivate().getEncoded();
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(privateKey);
        Path path = Files.createTempFile(
                "idp-admission-private-",
                ".pem"
        );
        Files.writeString(
                path,
                "-----BEGIN PRIVATE KEY-----\n"
                        + encoded
                        + "\n-----END PRIVATE KEY-----\n",
                StandardCharsets.US_ASCII
        );
        Files.setPosixFilePermissions(path, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
        ));
        path.toFile().deleteOnExit();
        return path;
    }
}
