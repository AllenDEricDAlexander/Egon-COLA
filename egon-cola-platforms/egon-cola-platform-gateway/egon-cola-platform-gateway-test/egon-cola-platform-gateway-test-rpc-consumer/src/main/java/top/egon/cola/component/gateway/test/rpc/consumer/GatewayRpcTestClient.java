package top.egon.cola.component.gateway.test.rpc.consumer;

import org.springframework.stereotype.Component;
import top.egon.cola.component.gateway.test.rpc.contract.EchoRpc;
import top.egon.cola.component.gateway.test.rpc.contract.OrderRpc;
import top.egon.cola.component.gateway.test.rpc.contract.proto.CreateOrderRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoResponse;
import top.egon.cola.component.gateway.test.rpc.contract.proto.FailRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.GetOrderRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.OrderResponse;
import top.egon.cola.component.gateway.test.rpc.contract.proto.SlowCallRequest;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;

@Component
public class GatewayRpcTestClient {

    @EgonRpcReference(timeoutMs = 3000)
    private EchoRpc echoRpc;

    @EgonRpcReference(timeoutMs = 3000)
    private OrderRpc orderRpc;

    public EchoResponse echo(String message) {
        return echoRpc.echo(EchoRequest.newBuilder()
                .setMessage(message)
                .build());
    }

    public OrderResponse order(String orderId) {
        return orderRpc.getOrder(GetOrderRequest.newBuilder()
                .setOrderId(orderId)
                .build());
    }

    public OrderResponse create(
            String customerId,
            java.util.List<String> skus) {
        return orderRpc.createOrder(CreateOrderRequest.newBuilder()
                .setCustomerId(customerId)
                .addAllSku(skus)
                .build());
    }

    public OrderResponse slow(String orderId, long delayMillis) {
        return orderRpc.slowCall(SlowCallRequest.newBuilder()
                .setOrderId(orderId)
                .setDelayMillis(delayMillis)
                .build());
    }

    public OrderResponse fail(String code) {
        return orderRpc.fail(FailRequest.newBuilder()
                .setCode(code)
                .build());
    }
}
