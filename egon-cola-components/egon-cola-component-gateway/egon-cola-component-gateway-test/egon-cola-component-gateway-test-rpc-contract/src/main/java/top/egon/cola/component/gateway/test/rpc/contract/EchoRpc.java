package top.egon.cola.component.gateway.test.rpc.contract;

import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoResponse;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

@EgonRpcService(
        grpcClass = EchoServiceGrpc.class,
        group = "default",
        version = "1.0.0"
)
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台域",
        entityDomainCode = "rpc-test",
        entityDomainName = "RPC 测试实体域",
        code = "echo-rpc",
        name = "Echo RPC 接口组"
)
public interface EchoRpc {

    @EgonRpcMethod(name = "Echo")
    @GatewayOperation(
            name = "RPC Echo",
            summary = "回显消息并返回调用元数据",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"rpc", "idempotent"}
    )
    EchoResponse echo(EchoRequest request);
}
