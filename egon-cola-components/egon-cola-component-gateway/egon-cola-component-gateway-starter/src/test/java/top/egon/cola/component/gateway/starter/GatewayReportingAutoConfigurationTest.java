package top.egon.cola.component.gateway.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.gateway.contract.reporting.GatewayDefinitionIdentity;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportHttpClient;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayReportingAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            GatewayReportingAutoConfiguration.class
                    ));

    @Test
    void remainsDisabledByDefault() {
        runner.run(context -> assertThat(context)
                .doesNotHaveBean(GatewayDefinitionIdentity.class));
    }

    @Test
    void publishesStableIdentityWhenEnabled() {
        runner.withPropertyValues(
                        "egon.cola.component.gateway.reporting.enabled=true",
                        "egon.cola.component.gateway.reporting.admin-base-url="
                                + "http://127.0.0.1:18080",
                        "egon.cola.component.gateway.reporting."
                                + "application-code=inventory",
                        "egon.cola.component.gateway.reporting."
                                + "application-name=Inventory",
                        "egon.cola.component.gateway.reporting.env=test",
                        "egon.cola.component.gateway.reporting.namespace=default",
                        "egon.cola.component.gateway.reporting."
                                + "artifact-version=1.0.0",
                        "egon.cola.component.gateway.reporting.build-id=build-1",
                        "egon.cola.component.gateway.reporting.access-key=ak",
                        "egon.cola.component.gateway.reporting.secret-key=sk"
                )
                .withBean(
                        GatewayReportHttpClient.class,
                        () -> new GatewayReportHttpClient(
                                enabledProperties()
                        )
                )
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(GatewayDefinitionIdentity.class);
                    assertThat(context.getBean(
                            GatewayDefinitionIdentity.class
                    ).buildId()).isEqualTo("build-1");
                });
    }

    private GatewayReportingProperties enabledProperties() {
        GatewayReportingProperties properties =
                new GatewayReportingProperties();
        properties.setEnabled(true);
        properties.setAdminBaseUrl("http://127.0.0.1:18080");
        properties.setApplicationCode("inventory");
        properties.setApplicationName("Inventory");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("1.0.0");
        properties.setBuildId("build-1");
        properties.setAccessKey("ak");
        properties.setSecretKey("sk");
        return properties;
    }
}
