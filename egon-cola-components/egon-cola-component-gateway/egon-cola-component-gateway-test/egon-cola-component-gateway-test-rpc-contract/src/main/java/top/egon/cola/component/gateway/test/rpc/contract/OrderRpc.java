package top.egon.cola.component.gateway.test.rpc.contract;

import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.test.rpc.contract.proto.CreateOrderRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.FailRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.GetOrderRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.OrderResponse;
import top.egon.cola.component.gateway.test.rpc.contract.proto.OrderServiceGrpc;
import top.egon.cola.component.gateway.test.rpc.contract.proto.SlowCallRequest;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

@EgonRpcService(
        grpcClass = OrderServiceGrpc.class,
        group = "default",
        version = "1.0.0"
)
@GatewayInterfaceGroup(
        businessDomainCode = "commerce",
        businessDomainName = "交易域",
        entityDomainCode = "order",
        entityDomainName = "订单实体域",
        code = "order-rpc",
        name = "订单 RPC 接口组"
)
public interface OrderRpc {

    @EgonRpcMethod(name = "GetOrder")
    @GatewayOperation(
            name = "RPC 查询订单",
            externalAccessible = false,
            tags = {"rpc", "query", "idempotent"}
    )
    OrderResponse getOrder(GetOrderRequest request);

    @EgonRpcMethod(name = "CreateOrder")
    @GatewayOperation(
            name = "RPC 创建订单",
            externalAccessible = false,
            tags = {"rpc", "command", "non-idempotent"}
    )
    OrderResponse createOrder(CreateOrderRequest request);

    @EgonRpcMethod(name = "SlowCall")
    @GatewayOperation(
            name = "RPC 延迟调用",
            externalAccessible = false,
            tags = {"rpc", "failure-test"}
    )
    OrderResponse slowCall(SlowCallRequest request);

    @EgonRpcMethod(name = "Fail")
    @GatewayOperation(
            name = "RPC 失败调用",
            externalAccessible = false,
            tags = {"rpc", "failure-test"}
    )
    OrderResponse fail(FailRequest request);
}
