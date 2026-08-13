package top.egon.cola.component.gateway.admin.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayAdminConfigurationTest {

    @Test
    void createsDdcManagementClientThroughTheDirectRpcFactory() {
        GatewayAdminConfiguration configuration =
                new GatewayAdminConfiguration();
        DdcRpcClientFactory factory = mock(DdcRpcClientFactory.class);
        DdcManagementClient client = mock(DdcManagementClient.class);
        DdcRpcClientHandle<DdcManagementClient> expected =
                new DdcRpcClientHandle<>(client, () -> {
                });
        when(factory.managementClient()).thenReturn(expected);

        DdcRpcClientHandle<DdcManagementClient> handle = configuration
                .gatewayDdcManagementClientHandle(factory);

        assertThat(handle).isSameAs(expected);
        assertThat(configuration.ddcManagementClient(handle)).isSameAs(client);
        verify(factory).managementClient();
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
