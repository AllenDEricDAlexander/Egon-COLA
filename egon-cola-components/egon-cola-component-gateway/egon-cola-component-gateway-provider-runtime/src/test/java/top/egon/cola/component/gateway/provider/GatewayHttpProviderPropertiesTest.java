package top.egon.cola.component.gateway.provider;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.gateway.contract.definition.GatewayDefinitionIdentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayHttpProviderPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesConfiguration.class)
                    .withPropertyValues(
                            "egon.cola.component.gateway.provider.http.enabled=true",
                            "egon.cola.component.gateway.provider.http.env=test",
                            "egon.cola.component.gateway.provider.http.namespace=gateway-test",
                            "egon.cola.component.gateway.provider.http.instance-id=orders-1",
                            "egon.cola.component.gateway.provider.http.service-name=orders",
                            "egon.cola.component.gateway.provider.http.group=blue",
                            "egon.cola.component.gateway.provider.http.version=1.0.0",
                            "egon.cola.component.gateway.provider.http.protocol=http",
                            "egon.cola.component.gateway.provider.http.advertised-host=127.0.0.1",
                            "egon.cola.component.gateway.provider.http.port=0",
                            "egon.cola.component.gateway.provider.http.lease-seconds=30",
                            "egon.cola.component.gateway.provider.http.heartbeat-interval-seconds=10",
                            "egon.cola.component.gateway.provider.http.fail-fast=true",
                            "egon.cola.component.gateway.provider.http.metadata.gateway.weight=100"
                    );

    @Test
    void bindsHttpProviderPropertiesAndResolvesActualServerPort() {
        contextRunner.run(context -> {
            GatewayHttpProviderProperties properties = context.getBean(
                    GatewayHttpProviderProperties.class
            );
            GatewayDefinitionIdentity identity =
                    new GatewayDefinitionIdentity(
                            "definition-set-a",
                            "1.0.0",
                            "build-a"
                    );

            HttpProviderRuntimeProperties runtime =
                    properties.toRuntime(identity, 18101);

            assertThat(properties.isEnabled()).isTrue();
            assertThat(runtime.env()).isEqualTo("test");
            assertThat(runtime.namespace()).isEqualTo("gateway-test");
            assertThat(runtime.instanceId()).isEqualTo("orders-1");
            assertThat(runtime.serviceName()).isEqualTo("orders");
            assertThat(runtime.group()).isEqualTo("blue");
            assertThat(runtime.version()).isEqualTo("1.0.0");
            assertThat(runtime.host()).isEqualTo("127.0.0.1");
            assertThat(runtime.port()).isEqualTo(18101);
            assertThat(runtime.metadata()).containsEntry(
                    "gateway.definition-set-id",
                    "definition-set-a"
            );
            assertThat(runtime.metadata()).containsEntry(
                    "gateway.weight",
                    "100"
            );
        });
    }

    @Test
    void rejectsProductionLoopbackAddress() {
        assertInvalid(
                "egon.cola.component.gateway.provider.http.env=prod",
                "egon.cola.component.gateway.provider.http.port=8080",
                "wildcard or loopback provider host is not allowed"
        );
    }

    @Test
    void rejectsHeartbeatThatIsNotShorterThanLease() {
        assertInvalid(
                "egon.cola.component.gateway.provider.http.heartbeat-interval-seconds=30",
                "heartbeat interval must be positive and less than lease"
        );
    }

    @Test
    void rejectsUnsupportedProtocol() {
        assertInvalid(
                "egon.cola.component.gateway.provider.http.protocol=grpc",
                "protocol must be http or https"
        );
    }

    @Test
    void rejectsVersionThatConflictsWithReportedDefinition() {
        contextRunner.run(context -> {
            GatewayHttpProviderProperties properties = context.getBean(
                    GatewayHttpProviderProperties.class
            );

            assertThatThrownBy(() -> properties.toRuntime(
                    new GatewayDefinitionIdentity(
                            "definition-set-a",
                            "2.0.0",
                            "build-a"
                    ),
                    18101
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "provider version conflicts with definition identity"
                    );
        });
    }

    private void assertInvalid(String property, String message) {
        contextRunner.withPropertyValues(property).run(context -> {
            GatewayHttpProviderProperties properties = context.getBean(
                    GatewayHttpProviderProperties.class
            );

            assertThatThrownBy(() -> properties.toRuntime(null, 18101))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(message);
        });
    }

    private void assertInvalid(
            String firstProperty,
            String secondProperty,
            String message) {
        contextRunner.withPropertyValues(firstProperty, secondProperty)
                .run(context -> {
                    GatewayHttpProviderProperties properties = context.getBean(
                            GatewayHttpProviderProperties.class
                    );

                    assertThatThrownBy(
                            () -> properties.toRuntime(null, 18101)
                    ).isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining(message);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GatewayHttpProviderProperties.class)
    static class PropertiesConfiguration {
    }
}
