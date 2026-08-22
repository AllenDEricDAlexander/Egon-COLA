package top.egon.cola.component.ddc.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import top.egon.cola.component.ddc.admin.security.management.DdcAdminCapability;
import top.egon.cola.component.ddc.admin.security.rpc.DdcHmacCredentialRegistry;
import top.egon.cola.component.ddc.admin.security.rpc.DdcRpcSecurityConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAdminSecurityPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class);

    private final ApplicationContextRunner rpcContextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(RpcTestConfiguration.class);

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
                        "egon.cola.component.ddc.admin.rpc.signature-enabled=false"
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
                        "egon.cola.component.ddc.admin.rpc.signature-enabled=true",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].credential-id=sdk-a",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].access-key=access-a",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].client-type=SDK"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "DDC RPC credential secret is required: sdk-a"
                            );
                });
    }

    @Test
    void explicitLocalDevelopmentModeMayDisableAuthentication() {
        contextRunner
                .withPropertyValues(
                        "egon.cola.component.ddc.admin.security.local-dev=true",
                        "egon.cola.component.ddc.admin.rpc.signature-enabled=false"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void registrationScopeMustRemainConfigured() {
        contextRunner
                .withPropertyValues(
                        "egon.cola.component.ddc.admin.security.local-dev=true",
                        "egon.cola.component.ddc.admin.rpc.signature-enabled=false",
                        "egon.cola.component.ddc.admin.registration.required-scope="
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "DDC registration scope is required"
                            );
                });
    }

    @Test
    void enabledRpcSignaturesRequireSharedNonceStore() {
        rpcContextRunner
                .withPropertyValues(
                        "egon.cola.component.ddc.admin.security.local-dev=true",
                        "egon.cola.component.ddc.admin.rpc.signature-enabled=true",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].credential-id=sdk-a",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].access-key=access-a",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].secret=secret-a",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].client-type=SDK",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].app-code-patterns[0]=*",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].env-patterns[0]=*",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].biz-code-patterns[0]=*",
                        "egon.cola.component.ddc.admin.rpc.credentials[0].allowed-operations[0]=*"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "Redis DdcNonceStore is required when DDC RPC signatures are enabled"
                            );
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DdcAdminProperties.class)
    @Import(DdcAdminSecurityPropertiesValidator.class)
    static class TestConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DdcAdminProperties.class)
    @Import({
            DdcAdminSecurityPropertiesValidator.class,
            DdcHmacCredentialRegistry.class,
            DdcRpcSecurityConfiguration.class
    })
    static class RpcTestConfiguration {
    }
}
