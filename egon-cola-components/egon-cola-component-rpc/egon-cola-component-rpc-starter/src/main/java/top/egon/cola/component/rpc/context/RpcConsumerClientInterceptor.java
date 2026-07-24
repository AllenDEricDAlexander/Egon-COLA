package top.egon.cola.component.rpc.context;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.rpc.contract.RpcContractDescriptor;

public class RpcConsumerClientInterceptor implements ClientInterceptor {

    private final Metadata metadata;

    public RpcConsumerClientInterceptor(
            RpcContractDescriptor contract,
            RpcProcessIdentity processIdentity) {
        metadata = new Metadata();
        metadata.put(RpcMetadataKeys.SERVICE, contract.serviceName());
        metadata.put(RpcMetadataKeys.GROUP, contract.group());
        metadata.put(RpcMetadataKeys.VERSION, contract.version());
        metadata.put(RpcMetadataKeys.INVOCATION_ID, UuidV7.simpleString());
        metadata.put(
                RpcMetadataKeys.SOURCE_APP,
                processIdentity.applicationName()
        );
        metadata.put(
                RpcMetadataKeys.SOURCE_INSTANCE,
                processIdentity.instanceId()
        );
        String traceId = TraceContext.getTraceId();
        metadata.put(
                RpcMetadataKeys.TRACE_ID,
                traceId == null || traceId.isBlank()
                        ? UuidV7.simpleString()
                        : traceId
        );
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        ClientCall<ReqT, RespT> delegate =
                next.newCall(method, callOptions);
        return new ForwardingClientCall.SimpleForwardingClientCall<>(delegate) {

            @Override
            public void start(
                    Listener<RespT> responseListener,
                    Metadata headers) {
                headers.merge(metadata);
                super.start(responseListener, headers);
            }
        };
    }
}
