package top.egon.cola.component.gateway.contract.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayOperationKeyTest {

    @Test
    void httpKeyNormalizesMethodAndPathWithoutChangingPathCase() {
        GatewayOperationKey key = GatewayOperationKey.http(
                "order-service",
                " get ",
                " Orders//{orderId}/ "
        );

        assertEquals(
                "order-service:http:GET:/Orders/{orderId}/",
                key.value()
        );
    }

    @Test
    void httpProtocolIdentityChangesProduceDifferentKeys() {
        GatewayOperationKey get = GatewayOperationKey.http(
                "order-service",
                "GET",
                "/orders/{orderId}"
        );
        GatewayOperationKey post = GatewayOperationKey.http(
                "order-service",
                "POST",
                "/orders/{orderId}"
        );
        GatewayOperationKey otherPath = GatewayOperationKey.http(
                "order-service",
                "GET",
                "/orders/{id}/details"
        );

        assertNotEquals(get, post);
        assertNotEquals(get, otherPath);
    }

    @Test
    void rpcKeyUsesTheCompleteServiceAndMethodIdentity() {
        GatewayOperationKey key = GatewayOperationKey.rpc(
                "order-service",
                "egon.order.v1.OrderService",
                "internal",
                "1.2.0",
                "egon.order.v1.OrderService/GetOrder"
        );

        assertEquals(
                "order-service:rpc:egon.order.v1.OrderService:internal:1.2.0:"
                        + "egon.order.v1.OrderService/GetOrder",
                key.value()
        );
    }

    @Test
    void blankProtocolIdentityPartsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> GatewayOperationKey.http(
                " ",
                "GET",
                "/orders"
        ));
        assertThrows(IllegalArgumentException.class, () -> GatewayOperationKey.http(
                "orders",
                " ",
                "/orders"
        ));
        assertThrows(IllegalArgumentException.class, () -> GatewayOperationKey.http(
                "orders",
                "GET",
                " "
        ));
        assertThrows(IllegalArgumentException.class, () -> GatewayOperationKey.rpc(
                "orders",
                "OrderService",
                "default",
                "1.0.0",
                " "
        ));
    }
}
