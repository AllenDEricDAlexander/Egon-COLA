package top.egon.cola.component.rpc.test.contract;

import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;

import java.util.concurrent.CompletionStage;

/** Java async view of the existing unary Echo wire method. */
@EgonRpcService(
        grpcClass = EchoServiceGrpc.class,
        group = "default",
        version = "1.0.0"
)
public interface AsyncEchoRpc {

    @EgonRpcMethod(name = "Echo")
    CompletionStage<EchoResponse> echoAsync(EchoRequest request);
}
