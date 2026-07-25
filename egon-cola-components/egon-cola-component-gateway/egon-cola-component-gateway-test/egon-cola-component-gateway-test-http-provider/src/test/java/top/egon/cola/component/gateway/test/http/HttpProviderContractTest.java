package top.egon.cola.component.gateway.test.http;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HttpProviderContractTest {

    @Test
    void everyControllerDefinesItsOwnInterfaceGroup() {
        assertNotNull(OrderController.class.getAnnotation(
                GatewayInterfaceGroup.class
        ));
        assertNotNull(InventoryController.class.getAnnotation(
                GatewayInterfaceGroup.class
        ));
        assertNotNull(BehaviorController.class.getAnnotation(
                GatewayInterfaceGroup.class
        ));
    }

    @Test
    void internalInventoryIsNotExternallyAccessible() throws Exception {
        GatewayOperation operation = InventoryController.class
                .getMethod("inventory", String.class)
                .getAnnotation(GatewayOperation.class);

        assertFalse(operation.externalAccessible());
    }

    @Test
    void bodyEndpointEchoesBinaryPayload() {
        byte[] body = {0, 1, 2, 127};

        assertArrayEquals(body, new BehaviorController().echo(body));
    }
}
