package top.egon.cola.component.rpc.context;

import io.grpc.Context;
import top.egon.cola.component.common.trace.TraceContext;

public record RpcInvocationMetadata(
        String service,
        String group,
        String version,
        String invocationId,
        TraceContext traceContext
) {

    static final Context.Key<RpcInvocationMetadata> CONTEXT_KEY =
            Context.key("egon-rpc-invocation-metadata");

    public static RpcInvocationMetadata current() {
        return CONTEXT_KEY.get();
    }

    public String traceId() {
        return traceContext.traceId();
    }

    public String spanId() {
        return traceContext.spanId();
    }

    public String parentSpanId() {
        return traceContext.parentSpanId();
    }

    public String requestId() {
        return traceContext.requestId();
    }

    public String sourceApp() {
        return traceContext.sourceApp();
    }

    public String sourceInstance() {
        return traceContext.sourceInstance();
    }

    public String traceparent() {
        return traceContext.traceparent();
    }

    public String tracestate() {
        return traceContext.tracestate();
    }
}
