package top.egon.cola.component.rpc.test.contract;

import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;

@EgonRpcService(
        grpcClass = EchoServiceGrpc.class,
        group = "default",
        version = "1.0.0"
)
public interface EchoRpc {

    @EgonRpcMethod(name = "Echo")
    EchoResponse echo(EchoRequest request);
}
