package top.egon.cola.component.rpc.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EgonRpcPropertiesTest {

    @Test
    void consumerDefaultsToGatewayEngineServiceIdentity() {
        assertThat(new EgonRpcProperties()
                .getConsumer()
                .getGatewayServiceName())
                .isEqualTo("egon-gateway-rpc");
    }
}
