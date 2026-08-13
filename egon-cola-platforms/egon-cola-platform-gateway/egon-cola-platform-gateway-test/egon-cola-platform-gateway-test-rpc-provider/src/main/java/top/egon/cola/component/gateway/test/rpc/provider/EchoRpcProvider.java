package top.egon.cola.component.gateway.test.rpc.provider;

import org.springframework.beans.factory.annotation.Value;
import top.egon.cola.component.gateway.test.rpc.contract.EchoRpc;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoRequest;
import top.egon.cola.component.gateway.test.rpc.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.context.invocation.RpcInvocationMetadata;

@EgonRpcProvider
public class EchoRpcProvider implements EchoRpc {

    private final String providerId;

    public EchoRpcProvider(
            @Value("${gateway.test.provider-id:rpc-provider-default}")
            String providerId) {
        this.providerId = providerId;
    }

    @Override
    public EchoResponse echo(EchoRequest request) {
        RpcInvocationMetadata invocation = RpcInvocationMetadata.current();
        return EchoResponse.newBuilder()
                .setProviderId(providerId)
                .setMessage(request.getMessage())
                .setInvocationId(invocation == null
                        ? ""
                        : value(invocation.invocationId()))
                .setTraceId(invocation == null
                        ? ""
                        : value(invocation.traceId()))
                .build();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
