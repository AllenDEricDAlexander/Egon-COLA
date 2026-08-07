package top.egon.cola.component.rpc.context;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import top.egon.cola.component.common.trace.TraceContext;

import java.util.regex.Pattern;

public class RpcProviderServerInterceptor implements ServerInterceptor {

    private static final int MAX_VALUE_LENGTH = 256;

    private static final Pattern SAFE_VALUE =
            Pattern.compile("[\\x20-\\x7e]{1," + MAX_VALUE_LENGTH + "}");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String sourceApp = safe(headers.get(RpcMetadataKeys.SOURCE_APP));
        String sourceInstance = safe(headers.get(RpcMetadataKeys.SOURCE_INSTANCE));
        TraceContext traceContext = TraceContext.fromHeaders(
                name -> traceHeader(headers, name),
                false
        ).withSource(sourceApp, sourceInstance);
        RpcInvocationMetadata invocation = new RpcInvocationMetadata(
                safe(headers.get(RpcMetadataKeys.SERVICE)),
                safe(headers.get(RpcMetadataKeys.GROUP)),
                safe(headers.get(RpcMetadataKeys.VERSION)),
                safe(headers.get(RpcMetadataKeys.INVOCATION_ID)),
                traceContext
        );
        Context context = Context.current().withValue(
                RpcInvocationMetadata.CONTEXT_KEY,
                invocation
        );
        ServerCall.Listener<ReqT> listener;
        try (TraceContext.Scope ignored = traceContext.open()) {
            listener = Contexts.interceptCall(context, call, headers, next);
        }
        return new TraceServerCallListener<>(listener, traceContext);
    }

    private String safe(String value) {
        return value != null && SAFE_VALUE.matcher(value).matches()
                ? value
                : null;
    }

    private String traceHeader(Metadata headers, String name) {
        if (TraceContext.TRACEPARENT_HEADER.equals(name)) {
            return headers.get(RpcMetadataKeys.TRACEPARENT);
        }
        if (TraceContext.TRACESTATE_HEADER.equals(name)) {
            return headers.get(RpcMetadataKeys.TRACESTATE);
        }
        if (TraceContext.REQUEST_ID_HEADER.equals(name)) {
            return headers.get(RpcMetadataKeys.REQUEST_ID);
        }
        return null;
    }

    private static final class TraceServerCallListener<ReqT>
            extends ForwardingServerCallListener
            .SimpleForwardingServerCallListener<ReqT> {

        private final TraceContext traceContext;

        private TraceServerCallListener(ServerCall.Listener<ReqT> delegate,
                                        TraceContext traceContext) {
            super(delegate);
            this.traceContext = traceContext;
        }

        @Override
        public void onMessage(ReqT message) {
            withScope(() -> super.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            withScope(super::onHalfClose);
        }

        @Override
        public void onCancel() {
            withScope(super::onCancel);
        }

        @Override
        public void onComplete() {
            withScope(super::onComplete);
        }

        @Override
        public void onReady() {
            withScope(super::onReady);
        }

        private void withScope(Runnable callback) {
            try (TraceContext.Scope ignored = traceContext.open()) {
                callback.run();
            }
        }
    }
}
