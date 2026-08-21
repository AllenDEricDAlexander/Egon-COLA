package top.egon.cola.component.rpc.consumer.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;

public class RpcConsumerClientInterceptor implements ClientInterceptor {

    private final String serviceName;

    private final String group;

    private final String version;

    private final RpcProcessIdentity processIdentity;

    private final String invocationId;

    public RpcConsumerClientInterceptor(
            RpcContractDescriptor contract,
            RpcProcessIdentity processIdentity) {
        this(
                contract == null ? null : contract.serviceName(),
                contract == null ? null : contract.group(),
                contract == null ? null : contract.version(),
                processIdentity,
                UuidV7.simpleString()
        );
    }

    public RpcConsumerClientInterceptor(
            String serviceName,
            String group,
            String version,
            RpcProcessIdentity processIdentity,
            String invocationId) {
        this.serviceName = required(serviceName, "serviceName");
        this.group = required(group, "group");
        this.version = required(version, "version");
        this.processIdentity = java.util.Objects.requireNonNull(
                processIdentity,
                "processIdentity"
        );
        this.invocationId = required(invocationId, "invocationId");
    }

    public static RpcConsumerClientInterceptor forTarget(
            String serviceName,
            String group,
            String version,
            RpcProcessIdentity processIdentity,
            String invocationId) {
        return new RpcConsumerClientInterceptor(
                serviceName,
                group,
                version,
                processIdentity,
                invocationId
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
                headers.merge(metadataAtStart(headers));
                super.start(responseListener, headers);
            }
        };
    }

    private Metadata metadataAtStart(Metadata existingHeaders) {
        Metadata metadata = new Metadata();
        metadata.put(RpcMetadataKeys.SERVICE, serviceName);
        metadata.put(RpcMetadataKeys.GROUP, group);
        metadata.put(RpcMetadataKeys.VERSION, version);
        metadata.put(RpcMetadataKeys.INVOCATION_ID, invocationId);
        metadata.put(RpcMetadataKeys.SOURCE_APP, processIdentity.applicationName());
        metadata.put(RpcMetadataKeys.SOURCE_INSTANCE, processIdentity.instanceId());
        if (!TraceContext.isValidTraceparent(
                existingHeaders.get(RpcMetadataKeys.TRACEPARENT))) {
            TraceContext child = TraceContext.currentOrCreate()
                    .withSource(
                            processIdentity.applicationName(),
                            processIdentity.instanceId()
                    )
                    .child();
            metadata.put(RpcMetadataKeys.TRACEPARENT, child.traceparent());
            if (child.tracestate() != null) {
                metadata.put(RpcMetadataKeys.TRACESTATE, child.tracestate());
            }
            if (child.requestId() != null) {
                metadata.put(RpcMetadataKeys.REQUEST_ID, child.requestId());
            }
        }
        return metadata;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
