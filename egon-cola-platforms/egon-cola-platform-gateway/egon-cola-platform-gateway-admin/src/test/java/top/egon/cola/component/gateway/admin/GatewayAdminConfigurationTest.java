package top.egon.cola.component.gateway.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import top.egon.cola.component.ddc.management.DdcManagementClient;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAdminConfigurationTest {

    @Test
    void createsSignedDdcManagementClientWhenEnabled() throws Exception {
        GatewayAdminConfiguration configuration =
                new GatewayAdminConfiguration();
        DdcManagementClient client = configuration.ddcManagementClient(
                "http://127.0.0.1:18080",
                "gateway-admin",
                "secret",
                Duration.ofSeconds(2),
                Duration.ofSeconds(7)
        );

        assertThat(client).isNotNull();
        Method method = GatewayAdminConfiguration.class.getDeclaredMethod(
                "ddcManagementClient",
                String.class,
                String.class,
                String.class,
                Duration.class,
                Duration.class
        );
        assertThat(method.getParameters())
                .allSatisfy(parameter ->
                        assertThat(parameter.getAnnotation(Value.class))
                                .isNotNull());
    }

    @Test
    void consumesTheGatewayEngineDefaultCallEventTopic() {
        Method consumerFactory = Arrays.stream(
                        GatewayAdminConfiguration.class.getDeclaredMethods()
                )
                .filter(method -> method.getName().equals(
                        "gatewayKafkaCallEventConsumer"
                ))
                .findFirst()
                .orElseThrow();

        assertThat(Arrays.stream(consumerFactory.getParameters())
                .map(parameter -> parameter.getAnnotation(Value.class))
                .filter(java.util.Objects::nonNull)
                .map(Value::value))
                .contains(
                        "${gateway.admin.observability.kafka.topic:"
                                + "egon.gateway.call.v1}"
                );
    }
}
