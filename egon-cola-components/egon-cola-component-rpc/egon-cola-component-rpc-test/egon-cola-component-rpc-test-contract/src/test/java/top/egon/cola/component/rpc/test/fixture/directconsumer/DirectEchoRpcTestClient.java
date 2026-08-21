package top.egon.cola.component.rpc.test.fixture.directconsumer;

import org.springframework.stereotype.Component;
import top.egon.cola.component.rpc.annotation.EgonRpcDirectReference;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.test.contract.EchoRpc;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;

/** Direct-only process fixture; it deliberately declares no Gateway reference. */
@Component
public class DirectEchoRpcTestClient {

    @EgonRpcDirectReference(
            bizCode = "test-biz",
            appCode = "test-app",
            retries = 1,
            loadBalance = LoadBalance.ROUND_ROBIN
    )
    private EchoRpc echoRpc;

    public EchoResponse echo(String message) {
        return echoRpc.echo(EchoRequest.newBuilder()
                .setMessage(message)
                .build());
    }

    public EchoRpc proxy() {
        return echoRpc;
    }
}
