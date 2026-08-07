package top.egon.cola.component.rpc.context;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.rpc.contract.RpcContractDescriptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RpcConsumerClientInterceptorTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void createsTraceMetadataWhenClientCallStarts() {
        TraceContext state = TraceContext.root("request-1");
        AtomicReference<Metadata> observed = new AtomicReference<>();
        RpcConsumerClientInterceptor interceptor =
                new RpcConsumerClientInterceptor(contract(), identity());
        ClientCall<String, String> call = interceptor.interceptCall(
                method(),
                CallOptions.DEFAULT,
                new CapturingChannel(observed)
        );

        try (TraceContext.Scope ignored = state.open()) {
            call.start(new ClientCall.Listener<>() {
            }, new Metadata());
        }

        Metadata headers = observed.get();
        assertThat(headers.get(RpcMetadataKeys.TRACEPARENT))
                .startsWith("00-" + state.traceId() + "-");
        assertThat(headers.get(RpcMetadataKeys.REQUEST_ID))
                .isEqualTo("request-1");
        assertThat(headers.keys()).doesNotContain("x-egon-trace-id");
        assertThat(headers.get(RpcMetadataKeys.INVOCATION_ID)).isNotBlank();
        assertThat(headers.get(RpcMetadataKeys.SOURCE_APP)).isEqualTo("rpc-app");
        assertThat(headers.get(RpcMetadataKeys.SOURCE_INSTANCE)).isEqualTo("rpc-1");
    }

    private RpcContractDescriptor contract() {
        return new RpcContractDescriptor(
                Object.class,
                "egon.rpc.test.Echo",
                "default",
                "1.0.0",
                List.of()
        );
    }

    private RpcProcessIdentity identity() {
        return new RpcProcessIdentity(
                "rpc-app",
                "test",
                "default",
                "127.0.0.1",
                1L,
                "rpc-1"
        );
    }

    private MethodDescriptor<String, String> method() {
        return MethodDescriptor.<String, String>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("egon.rpc.test.Echo/Echo")
                .setRequestMarshaller(new StringMarshaller())
                .setResponseMarshaller(new StringMarshaller())
                .build();
    }

    private static final class CapturingChannel extends Channel {

        private final AtomicReference<Metadata> observed;

        private CapturingChannel(AtomicReference<Metadata> observed) {
            this.observed = observed;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
                MethodDescriptor<ReqT, RespT> methodDescriptor,
                CallOptions callOptions) {
            return new ClientCall<>() {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    observed.set(headers);
                }

                @Override
                public void request(int numMessages) {
                }

                @Override
                public void cancel(String message, Throwable cause) {
                }

                @Override
                public void halfClose() {
                }

                @Override
                public void sendMessage(ReqT message) {
                }
            };
        }

        @Override
        public String authority() {
            return "test";
        }
    }

    private static final class StringMarshaller
            implements MethodDescriptor.Marshaller<String> {

        @Override
        public java.io.InputStream stream(String value) {
            return new java.io.ByteArrayInputStream(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public String parse(java.io.InputStream stream) {
            try {
                return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
