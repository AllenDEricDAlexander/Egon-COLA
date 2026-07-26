package top.egon.cola.component.ddc.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import top.egon.cola.component.ddc.admin.security.DdcAdminCapability;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAdminSecurityPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class);

    @Test
    void secureDefaultsRequireExplicitLocalDevelopmentMode() {
        DdcAdminProperties properties = new DdcAdminProperties();

        assertThat(properties.getSecurity().isLocalDev()).isFalse();
        assertThat(DdcAdminCapability.READ.authority())
                .isEqualTo("CAP_DDC_READ");
        assertThat(DdcAdminCapability.ALL.authority())
                .isEqualTo("CAP_*");
    }

    @Test
    void productionModeRequiresJwtIssuerAndAudience() {
        contextRunner
                .withPropertyValues(
                        "egon.cola.component.ddc.admin.security.local-dev=false",
                        "egon.cola.component.ddc.admin.openapi.signature-enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "DDC Admin JWT issuer is required"
                            );
                });
    }

    @Test
    void enabledHmacCredentialRequiresSecret() {
        contextRunner
                .withPropertyValues(
                        "egon.cola.component.ddc.admin.security.jwt.issuer=https://issuer.example",
                        "egon.cola.component.ddc.admin.security.jwt.audience=ddc-admin",
                        "egon.cola.component.ddc.admin.openapi.signature-enabled=true",
                        "egon.cola.component.ddc.admin.openapi.credentials[0].credential-id=sdk-a",
                        "egon.cola.component.ddc.admin.openapi.credentials[0].access-key=access-a",
                        "egon.cola.component.ddc.admin.openapi.credentials[0].client-type=SDK"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "DDC OpenAPI credential secret is required: sdk-a"
                            );
                });
    }

    @Test
    void explicitLocalDevelopmentModeMayDisableAuthentication() {
        contextRunner
                .withPropertyValues(
                        "egon.cola.component.ddc.admin.security.local-dev=true",
                        "egon.cola.component.ddc.admin.openapi.signature-enabled=false"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DdcAdminProperties.class)
    @Import(DdcAdminSecurityPropertiesValidator.class)
    static class TestConfiguration {
    }
}
