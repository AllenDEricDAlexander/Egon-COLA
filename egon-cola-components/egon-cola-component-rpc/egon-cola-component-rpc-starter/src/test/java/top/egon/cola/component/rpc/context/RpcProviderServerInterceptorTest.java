package top.egon.cola.component.rpc.context;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RpcProviderServerInterceptorTest {

    @Test
    void shouldExposeOnlyValidatedMetadataAndReplaceInvalidTrace() {
        Metadata headers = new Metadata();
        headers.put(RpcMetadataKeys.SERVICE, "egon.rpc.test.Echo");
        headers.put(RpcMetadataKeys.GROUP, "default");
        headers.put(RpcMetadataKeys.TRACE_ID, "not-a-trace");
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
        assertThat(RpcInvocationMetadata.current()).isNull();
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
