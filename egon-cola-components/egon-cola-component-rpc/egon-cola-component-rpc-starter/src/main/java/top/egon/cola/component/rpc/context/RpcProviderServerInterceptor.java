package top.egon.cola.component.rpc.context;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.TracePropagation;
import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceState;

import java.util.Objects;
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
        TracePropagation.Extracted extracted = TracePropagation.extract(
                name -> metadata(headers, name),
                TracePropagation.Options.defaults()
        );
        TraceState traceState = extracted.state().withSource(
                safe(headers.get(RpcMetadataKeys.SOURCE_APP)),
                safe(headers.get(RpcMetadataKeys.SOURCE_INSTANCE))
        );
        RpcInvocationMetadata invocation = new RpcInvocationMetadata(
                safe(headers.get(RpcMetadataKeys.SERVICE)),
                safe(headers.get(RpcMetadataKeys.GROUP)),
                safe(headers.get(RpcMetadataKeys.VERSION)),
                safe(headers.get(RpcMetadataKeys.INVOCATION_ID)),
                safe(headers.get(RpcMetadataKeys.SOURCE_APP)),
                safe(headers.get(RpcMetadataKeys.SOURCE_INSTANCE)),
                traceState.traceId(),
                traceState.spanId(),
                traceState.parentSpanId(),
                traceState.requestId(),
                traceState.traceparent(),
                traceState.tracestate()
        );
        Context context = Context.current().withValue(
                RpcInvocationMetadata.CONTEXT_KEY,
                invocation
        );
        ServerCall.Listener<ReqT> listener =
                Contexts.interceptCall(context, call, headers, next);
        return new TraceServerCallListener<>(listener, traceState);
    }

    private String safe(String value) {
        return value != null && SAFE_VALUE.matcher(value).matches()
                ? value
                : null;
    }

    private String metadata(Metadata headers, String name) {
        if (Objects.equals(name, "traceparent")) {
            return headers.get(RpcMetadataKeys.TRACEPARENT);
        }
        if (Objects.equals(name, "tracestate")) {
            return headers.get(RpcMetadataKeys.TRACESTATE);
        }
        if (Objects.equals(name, "x-egon-request-id")) {
            return headers.get(RpcMetadataKeys.REQUEST_ID);
        }
        return null;
    }

    private static final class TraceServerCallListener<ReqT>
            extends ForwardingServerCallListener
            .SimpleForwardingServerCallListener<ReqT> {

        private final TraceState traceState;

        private TraceServerCallListener(ServerCall.Listener<ReqT> delegate,
                                        TraceState traceState) {
            super(delegate);
            this.traceState = traceState;
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
            try (TraceScope ignored = TraceContext.open(traceState)) {
                callback.run();
            }
        }
    }
}
