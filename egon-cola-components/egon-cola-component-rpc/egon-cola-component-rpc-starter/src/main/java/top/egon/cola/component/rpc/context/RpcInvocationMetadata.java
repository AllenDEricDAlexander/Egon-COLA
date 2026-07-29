package top.egon.cola.component.rpc.context;

import io.grpc.Context;

public record RpcInvocationMetadata(
        String service,
        String group,
        String version,
        String invocationId,
        String sourceApp,
        String sourceInstance,
        String traceId,
        String spanId,
        String parentSpanId,
        String requestId,
        String traceparent,
        String tracestate
) {

    static final Context.Key<RpcInvocationMetadata> CONTEXT_KEY =
            Context.key("egon-rpc-invocation-metadata");

    public static RpcInvocationMetadata current() {
        return CONTEXT_KEY.get();
    }
}
