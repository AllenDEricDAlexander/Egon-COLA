package top.egon.cola.component.gateway.test.rpc.consumer;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewayRpcTestClientTest {

    @Test
    void consumerReferencesBusinessContractsThroughRpcComponent()
            throws Exception {
        EgonRpcReference echo = GatewayRpcTestClient.class
                .getDeclaredField("echoRpc")
                .getAnnotation(EgonRpcReference.class);
        EgonRpcReference order = GatewayRpcTestClient.class
                .getDeclaredField("orderRpc")
                .getAnnotation(EgonRpcReference.class);

        assertNotNull(echo);
        assertNotNull(order);
        assertEquals(3000, echo.timeoutMs());
        assertEquals(3000, order.timeoutMs());
    }
}
