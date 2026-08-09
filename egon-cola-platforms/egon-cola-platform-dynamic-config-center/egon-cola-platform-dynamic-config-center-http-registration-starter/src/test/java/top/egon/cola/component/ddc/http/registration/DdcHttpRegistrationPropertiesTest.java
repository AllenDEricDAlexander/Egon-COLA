package top.egon.cola.component.ddc.http.registration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcHttpRegistrationPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesConfiguration.class)
                    .withPropertyValues(
                            "egon.cola.component.ddc.registry.http.enabled=true",
                            "egon.cola.component.ddc.registry.http.env=test",
                            "egon.cola.component.ddc.registry.http.namespace=gateway-test",
                            "egon.cola.component.ddc.registry.http.instance-id=orders-1",
                            "egon.cola.component.ddc.registry.http.service-name=orders",
                            "egon.cola.component.ddc.registry.http.group=blue",
                            "egon.cola.component.ddc.registry.http.version=1.0.0",
                            "egon.cola.component.ddc.registry.http.protocol=http",
                            "egon.cola.component.ddc.registry.http.advertised-host=127.0.0.1",
                            "egon.cola.component.ddc.registry.http.port=0",
                            "egon.cola.component.ddc.registry.http.lease-seconds=30",
                            "egon.cola.component.ddc.registry.http.heartbeat-interval-seconds=10",
                            "egon.cola.component.ddc.registry.http.fail-fast=true",
                            "egon.cola.component.ddc.registry.http.metadata.gateway.weight=100"
                    );

    @Test
    void bindsHttpProviderPropertiesAndResolvesActualServerPort() {
        contextRunner.run(context -> {
            DdcHttpRegistrationProperties properties = context.getBean(
                    DdcHttpRegistrationProperties.class
            );

            DdcHttpRegistrationRuntimeProperties runtime =
                    properties.toRuntime(
                            "1.0.0",
                            Map.of(
                                    "gateway.definition-set-id",
                                    "definition-set-a"
                            ),
                            18101
                    );

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
                "egon.cola.component.ddc.registry.http.env=prod",
                "egon.cola.component.ddc.registry.http.port=8080",
                "wildcard or loopback provider host is not allowed"
        );
    }

    @Test
    void rejectsHeartbeatThatIsNotShorterThanLease() {
        assertInvalid(
                "egon.cola.component.ddc.registry.http.heartbeat-interval-seconds=30",
                "heartbeat interval must be positive and less than lease"
        );
    }

    @Test
    void rejectsUnsupportedProtocol() {
        assertInvalid(
                "egon.cola.component.ddc.registry.http.protocol=grpc",
                "protocol must be http or https"
        );
    }

    @Test
    void rejectsVersionThatConflictsWithContributor() {
        contextRunner.run(context -> {
            DdcHttpRegistrationProperties properties = context.getBean(
                    DdcHttpRegistrationProperties.class
            );

            assertThatThrownBy(() -> properties.toRuntime(
                    "2.0.0",
                    Map.of(),
                    18101
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "version conflicts with contributor"
                    );
        });
    }

    private void assertInvalid(String property, String message) {
        contextRunner.withPropertyValues(property).run(context -> {
            DdcHttpRegistrationProperties properties = context.getBean(
                    DdcHttpRegistrationProperties.class
            );

            assertThatThrownBy(() -> properties.toRuntime(
                    null,
                    Map.of(),
                    18101
            ))
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
                    DdcHttpRegistrationProperties properties = context.getBean(
                            DdcHttpRegistrationProperties.class
                    );

                    assertThatThrownBy(
                            () -> properties.toRuntime(
                                    null,
                                    Map.of(),
                                    18101
                            )
                    ).isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining(message);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DdcHttpRegistrationProperties.class)
    static class PropertiesConfiguration {
    }
}
