package top.egon.cola.component.gateway.test.rpc.provider;

import io.grpc.Status;
import org.springframework.beans.factory.annotation.Value;
import top.egon.cola.component.gateway.test.rpc.contract.OrderRpc;
import top.egon.cola.component.gateway.test.rpc.contract.proto.CreateOrderRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.FailRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.GetOrderRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.OrderResponse;
import top.egon.cola.component.gateway.test.rpc.contract.proto.SlowCallRequest;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

@EgonRpcProvider
public class OrderRpcProvider implements OrderRpc {

    private final String providerId;

    public OrderRpcProvider(
            @Value("${gateway.test.provider-id:rpc-provider-default}")
            String providerId) {
        this.providerId = providerId;
    }

    @Override
    public OrderResponse getOrder(GetOrderRequest request) {
        return response(request.getOrderId(), "CREATED");
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        return response("order-" + request.getCustomerId(), "CREATED");
    }

    @Override
    public OrderResponse slowCall(SlowCallRequest request) {
        try {
            Thread.sleep(Math.max(
                    0,
                    Math.min(request.getDelayMillis(), 10_000)
            ));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw Status.CANCELLED
                    .withDescription("provider call interrupted")
                    .asRuntimeException();
        }
        return response(request.getOrderId(), "SLOW_COMPLETED");
    }

    @Override
    public OrderResponse fail(FailRequest request) {
        throw Status.FAILED_PRECONDITION
                .withDescription("gateway-test:" + request.getCode())
                .asRuntimeException();
    }

    private OrderResponse response(String orderId, String status) {
        return OrderResponse.newBuilder()
                .setOrderId(orderId)
                .setStatus(status)
                .setProviderId(providerId)
                .build();
    }
}
