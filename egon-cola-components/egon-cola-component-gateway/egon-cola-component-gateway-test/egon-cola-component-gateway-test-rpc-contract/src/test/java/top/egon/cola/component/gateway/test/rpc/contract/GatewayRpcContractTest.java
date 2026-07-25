package top.egon.cola.component.gateway.test.rpc.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.gateway.test.rpc.contract.proto.OrderServiceGrpc;

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
}
