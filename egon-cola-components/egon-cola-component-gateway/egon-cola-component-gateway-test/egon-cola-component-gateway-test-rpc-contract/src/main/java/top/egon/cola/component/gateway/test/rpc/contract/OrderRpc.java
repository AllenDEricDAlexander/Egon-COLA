package top.egon.cola.component.gateway.test.rpc.contract;

import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
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

    @EgonRpcMethod(name = "GetOrder", idempotent = true)
    @GatewayOperation(
            name = "RPC 查询订单",
            externalAccessible = false,
            tags = {"rpc", "query", "idempotent"}
    )
    OrderResponse getOrder(GetOrderRequest request);

    @EgonRpcMethod(name = "CreateOrder", idempotent = false)
    @GatewayOperation(
            name = "RPC 创建订单",
            externalAccessible = false,
            tags = {"rpc", "command", "non-idempotent"},
            requestSchemaFields = {
                    @GatewaySchemaField(
                            path = "customerId",
                            description = "客户编号"
                    ),
                    @GatewaySchemaField(
                            path = "sku",
                            description = "商品 SKU 列表"
                    ),
                    @GatewaySchemaField(
                            path = "deliveryAddress",
                            description = "配送地址"
                    ),
                    @GatewaySchemaField(
                            path = "deliveryAddress.province",
                            description = "配送省份"
                    ),
                    @GatewaySchemaField(
                            path = "deliveryAddress.city",
                            description = "配送城市"
                    )
            },
            responseSchemaFields = {
                    @GatewaySchemaField(
                            path = "orderId",
                            description = "订单编号"
                    ),
                    @GatewaySchemaField(
                            path = "status",
                            description = "订单状态"
                    ),
                    @GatewaySchemaField(
                            path = "providerId",
                            description = "服务提供者实例编号"
                    )
            }
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
