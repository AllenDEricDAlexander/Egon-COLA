package top.egon.cola.component.gateway.test.rpc.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import top.egon.cola.component.gateway.test.rpc.contract.proto.CreateOrderRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.gateway.test.rpc.contract.proto.OrderServiceGrpc;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewayRpcContractTest {

    @Test
    void generatedServicesExposeAllUnaryMethods() {
        assertEquals(1, EchoServiceGrpc.getServiceDescriptor()
                .getMethods().size());
        assertEquals(4, OrderServiceGrpc.getServiceDescriptor()
                .getMethods().size());
    }

    @Test
    void rpcServicesHaveCatalogGroupingAndInternalExposureDefault()
            throws Exception {
        assertNotNull(EchoRpc.class.getAnnotation(
                GatewayInterfaceGroup.class
        ));
        assertNotNull(OrderRpc.class.getAnnotation(
                GatewayInterfaceGroup.class
        ));
        GatewayOperation operation = EchoRpc.class
                .getMethod(
                        "echo",
                        top.egon.cola.component.gateway.test.rpc.contract.proto
                                .EchoRequest.class
                )
                .getAnnotation(GatewayOperation.class);
        assertFalse(operation.externalAccessible());
    }

    @Test
    void createOrderDocumentsEveryRequestAndResponseField()
            throws Exception {
        GatewayOperation operation = OrderRpc.class
                .getMethod("createOrder", CreateOrderRequest.class)
                .getAnnotation(GatewayOperation.class);

        assertEquals(
                Map.of(
                        "customerId", "客户编号",
                        "sku", "商品 SKU 列表",
                        "deliveryAddress", "配送地址",
                        "deliveryAddress.province", "配送省份",
                        "deliveryAddress.city", "配送城市"
                ),
                descriptions(operation.requestSchemaFields())
        );
        assertEquals(
                Map.of(
                        "orderId", "订单编号",
                        "status", "订单状态",
                        "providerId", "服务提供者实例编号"
                ),
                descriptions(operation.responseSchemaFields())
        );
    }

    private Map<String, String> descriptions(
            GatewaySchemaField[] fields) {
        return Arrays.stream(fields).collect(Collectors.toMap(
                GatewaySchemaField::path,
                GatewaySchemaField::description
        ));
    }
}
