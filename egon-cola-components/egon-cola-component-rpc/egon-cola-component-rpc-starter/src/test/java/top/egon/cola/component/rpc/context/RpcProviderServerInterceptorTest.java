package top.egon.cola.component.rpc.context;

import io.grpc.Attributes;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;

import static org.assertj.core.api.Assertions.assertThat;

class RpcProviderServerInterceptorTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldExposeValidatedMetadataAndCreateMissingTrace() {
        Metadata headers = new Metadata();
        headers.put(RpcMetadataKeys.SERVICE, "egon.rpc.test.Echo");
        headers.put(RpcMetadataKeys.GROUP, "default");
        RpcInvocationMetadata[] observed = new RpcInvocationMetadata[1];
        ServerCallHandler<String, String> next = (call, metadata) -> {
            observed[0] = RpcInvocationMetadata.current();
            return new ServerCall.Listener<>() {
            };
        };

        new RpcProviderServerInterceptor().interceptCall(
                new NoOpServerCall(),
                headers,
                next
        );

        assertThat(observed[0].service())
                .isEqualTo("egon.rpc.test.Echo");
        assertThat(observed[0].group()).isEqualTo("default");
        assertThat(observed[0].traceId()).matches("[0-9a-f]{32}");
        assertThat(observed[0].spanId()).matches("[0-9a-f]{16}");
        assertThat(RpcInvocationMetadata.current()).isNull();
    }

    @Test
    void restoresMdcForEveryServerListenerCallback() {
        Metadata headers = new Metadata();
        headers.put(
                RpcMetadataKeys.TRACEPARENT,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );
        headers.put(RpcMetadataKeys.REQUEST_ID, "request-1");
        headers.put(RpcMetadataKeys.INVOCATION_ID, "invoke-1");
        StringBuilder callbacks = new StringBuilder();
        ServerCallHandler<String, String> next = (call, metadata) ->
                new ServerCall.Listener<>() {
                    @Override
                    public void onMessage(String message) {
                        assertTrace(callbacks, "message");
                    }

                    @Override
                    public void onHalfClose() {
                        assertTrace(callbacks, "halfClose");
                    }

                    @Override
                    public void onReady() {
                        assertTrace(callbacks, "ready");
                    }

                    @Override
                    public void onComplete() {
                        assertTrace(callbacks, "complete");
                    }
                };

        ServerCall.Listener<String> listener =
                new RpcProviderServerInterceptor().interceptCall(
                        new NoOpServerCall(),
                        headers,
                        next
                );

        MDC.put(TraceContext.TRACE_ID, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        listener.onMessage("request");
        listener.onHalfClose();
        listener.onReady();
        listener.onComplete();

        assertThat(callbacks.toString())
                .isEqualTo("message;halfClose;ready;complete;");
        assertThat(MDC.get(TraceContext.TRACE_ID))
                .isEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    private void assertTrace(StringBuilder callbacks, String callback) {
        callbacks.append(callback).append(';');
        assertThat(MDC.get(TraceContext.TRACE_ID))
                .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(MDC.get(TraceContext.PARENT_SPAN_ID))
                .isEqualTo("00f067aa0ba902b7");
        assertThat(MDC.get(TraceContext.REQUEST_ID)).isEqualTo("request-1");
        assertThat(RpcInvocationMetadata.current().invocationId())
                .isEqualTo("invoke-1");
    }

    private static final class NoOpServerCall
            extends ServerCall<String, String> {

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void sendHeaders(Metadata headers) {
        }

        @Override
        public void sendMessage(String message) {
        }

        @Override
        public void close(Status status, Metadata trailers) {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public MethodDescriptor<String, String> getMethodDescriptor() {
            return null;
        }

        @Override
        public Attributes getAttributes() {
            return Attributes.EMPTY;
        }
    }
}
