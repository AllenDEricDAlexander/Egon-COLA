package top.egon.cola.component.rpc.test.provider;

import org.springframework.beans.factory.annotation.Value;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.test.contract.EchoRpc;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;

@EgonRpcProvider
public class EchoRpcProvider implements EchoRpc {

    private final String providerId;

    public EchoRpcProvider(
            @Value("${rpc.test.provider-id:provider-default}")
            String providerId) {
        this.providerId = providerId;
    }

    @Override
    public EchoResponse echo(EchoRequest request) {
        return EchoResponse.newBuilder()
                .setProviderId(providerId)
                .setMessage(request.getMessage())
                .build();
    }
}
