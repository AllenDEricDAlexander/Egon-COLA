package top.egon.cola.component.rpc.test.fixture.consumer;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.test.contract.AsyncEchoRpc;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;

import java.util.concurrent.CompletionStage;

/** Isolated async Consumer fixture; enable profile {@code rpc-async-fixture}. */
@Component
@Profile("rpc-async-fixture")
public class AsyncEchoRpcTestClient {

    @EgonRpcReference(
            mode = RpcReferenceMode.GATEWAY,
            timeoutMs = 3000,
            retries = 1)
    private AsyncEchoRpc asyncEchoRpc;

    public CompletionStage<EchoResponse> echoAsync(String message) {
        return asyncEchoRpc.echoAsync(EchoRequest.newBuilder()
                .setMessage(message)
                .build());
    }

    public AsyncEchoRpc proxy() {
        return asyncEchoRpc;
    }
}
