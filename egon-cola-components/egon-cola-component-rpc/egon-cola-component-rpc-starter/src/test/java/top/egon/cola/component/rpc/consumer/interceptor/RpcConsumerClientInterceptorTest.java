package top.egon.cola.component.rpc.consumer.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;
import top.egon.cola.component.rpc.consumer.generic.RpcGenericInvocation;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;

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

    @Test
    void genericTargetUsesOneLogicalInvocationIdAcrossAttempts() {
        RpcConsumerClientInterceptor interceptor =
                RpcConsumerClientInterceptor.forTarget(
                        "egon.rpc.test.Echo",
                        "default",
                        "1.0.0",
                        identity(),
                        "invocation-1"
                );
        AtomicReference<Metadata> firstHeaders = new AtomicReference<>();
        AtomicReference<Metadata> secondHeaders = new AtomicReference<>();
        ClientCall<String, String> first = interceptor.interceptCall(
                method(),
                CallOptions.DEFAULT,
                new CapturingChannel(firstHeaders)
        );
        ClientCall<String, String> second = interceptor.interceptCall(
                method(),
                CallOptions.DEFAULT,
                new CapturingChannel(secondHeaders)
        );

        first.start(new ClientCall.Listener<>() {
        }, new Metadata());
        second.start(new ClientCall.Listener<>() {
        }, new Metadata());

        assertThat(firstHeaders.get().get(RpcMetadataKeys.INVOCATION_ID))
                .isEqualTo("invocation-1");
        assertThat(secondHeaders.get().get(RpcMetadataKeys.INVOCATION_ID))
                .isEqualTo("invocation-1");
        assertThat(firstHeaders.get().get(RpcMetadataKeys.SERVICE))
                .isEqualTo("egon.rpc.test.Echo");
    }

    @Test
    void genericInvocationContextIsRawAndDefensive() {
        byte[] payload = new byte[]{1, 2, 3};
        RpcGenericInvocation command = RpcGenericInvocation.gateway(
                "egon.rpc.test.Echo",
                "default",
                "1.0.0",
                "egon.rpc.test.Echo/Echo",
                payload,
                1000,
                0,
                LoadBalance.ROUND_ROBIN,
                FailStrategy.FAIL_CLOSED,
                null
        );
        RpcClientInvocation invocation = RpcClientInvocation.generic(
                command,
                identity(),
                "invocation-2"
        );
        payload[0] = 9;

        assertThat(invocation.generic()).isTrue();
        assertThat(invocation.contract()).isNull();
        assertThat(invocation.method()).isNull();
        assertThat(invocation.request()).isNull();
        assertThat(invocation.rawRequestPayload())
                .containsExactly(1, 2, 3);
        assertThat(invocation.invocationId()).isEqualTo("invocation-2");
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
