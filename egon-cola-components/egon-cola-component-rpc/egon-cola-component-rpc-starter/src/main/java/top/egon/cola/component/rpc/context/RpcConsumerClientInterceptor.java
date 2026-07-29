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
import top.egon.cola.component.common.trace.TraceParent;
import top.egon.cola.component.common.trace.TraceState;
import top.egon.cola.component.rpc.contract.RpcContractDescriptor;

public class RpcConsumerClientInterceptor implements ClientInterceptor {

    private final RpcContractDescriptor contract;

    private final RpcProcessIdentity processIdentity;

    private final String invocationId;

    public RpcConsumerClientInterceptor(
            RpcContractDescriptor contract,
            RpcProcessIdentity processIdentity) {
        this.contract = contract;
        this.processIdentity = processIdentity;
        this.invocationId = UuidV7.simpleString();
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
                headers.merge(metadataAtStart(headers));
                super.start(responseListener, headers);
            }
        };
    }

    private Metadata metadataAtStart(Metadata existingHeaders) {
        Metadata metadata = new Metadata();
        metadata.put(RpcMetadataKeys.SERVICE, contract.serviceName());
        metadata.put(RpcMetadataKeys.GROUP, contract.group());
        metadata.put(RpcMetadataKeys.VERSION, contract.version());
        metadata.put(RpcMetadataKeys.INVOCATION_ID, invocationId);
        metadata.put(RpcMetadataKeys.SOURCE_APP, processIdentity.applicationName());
        metadata.put(RpcMetadataKeys.SOURCE_INSTANCE, processIdentity.instanceId());
        TraceState child = TraceContext.currentOrCreate()
                .withSource(
                        processIdentity.applicationName(),
                        processIdentity.instanceId()
                )
                .child();
        if (!TraceParent.parse(
                existingHeaders.get(RpcMetadataKeys.TRACEPARENT)
        ).isPresent()) {
            metadata.put(RpcMetadataKeys.TRACEPARENT, child.traceparent());
        }
        if (child.tracestate() != null && !child.tracestate().isBlank()) {
            metadata.put(RpcMetadataKeys.TRACESTATE, child.tracestate());
        }
        if (child.requestId() != null && !child.requestId().isBlank()) {
            metadata.put(RpcMetadataKeys.REQUEST_ID, child.requestId());
        }
        return metadata;
    }
}
