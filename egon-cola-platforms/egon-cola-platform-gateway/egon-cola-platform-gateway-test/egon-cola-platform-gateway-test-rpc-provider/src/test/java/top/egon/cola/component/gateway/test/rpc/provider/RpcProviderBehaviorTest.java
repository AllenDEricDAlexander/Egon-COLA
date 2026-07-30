package top.egon.cola.component.gateway.test.rpc.provider;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.test.rpc.contract.proto.FailRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.GetOrderRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpcProviderBehaviorTest {

    @Test
    void returnsProviderIdentityAndTypedGrpcFailure() {
        OrderRpcProvider provider = new OrderRpcProvider("rpc-provider-a");

        assertEquals(
                "rpc-provider-a",
                provider.getOrder(GetOrderRequest.newBuilder()
                                .setOrderId("order-1")
                                .build())
                        .getProviderId()
        );
        StatusRuntimeException failure = assertThrows(
                StatusRuntimeException.class,
                () -> provider.fail(FailRequest.newBuilder()
                        .setCode("ORDER_LOCKED")
                        .build())
        );
        assertEquals(
                Status.Code.FAILED_PRECONDITION,
                failure.getStatus().getCode()
        );
    }
}
