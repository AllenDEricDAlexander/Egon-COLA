package top.egon.cola.component.rpc.consumer;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.config.RpcTransportSecurity;
import top.egon.cola.component.rpc.context.RpcClientInvocation;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcDirectClientFactoryTest {

    private static final Metadata.Key<String> SIGNATURE =
            Metadata.Key.of("x-test-signature", Metadata.ASCII_STRING_MARSHALLER);

    @Test
    void invokesConfiguredTargetWithRequestAwareInterceptorAndDeadline()
            throws Exception {
        AtomicReference<String> observedSignature = new AtomicReference<>();
        AtomicLong observedDeadlineMs = new AtomicLong();
        Server server = server(observedSignature, observedDeadlineMs)
                .build()
                .start();
        AtomicReference<RpcClientInvocation> invocation =
                new AtomicReference<>();
        try {
            RpcDirectClientSettings settings = settings(server.getPort());
            RpcDirectClientFactory factory = new RpcDirectClientFactory();

            try (RpcDirectClientHandle<EchoContract> handle = factory.create(
                    EchoContract.class,
                    settings,
                    List.of(current -> {
                        invocation.set(current);
                        Metadata headers = new Metadata();
                        headers.put(
                                SIGNATURE,
                                current.method().fullMethodName()
                                        + ":"
                                        + ((StringValue) current.request())
                                        .getValue()
                        );
                        return MetadataUtils.newAttachHeadersInterceptor(
                                headers
                        );
                    })
            )) {
                StringValue response = handle.client().echo(
                        StringValue.of("hello")
                );

                assertThat(response.getValue()).isEqualTo("direct:hello");
                assertThat(invocation.get().contract().contractType())
                        .isEqualTo(EchoContract.class);
                assertThat(invocation.get().processIdentity())
                        .isEqualTo(settings.processIdentity());
                assertThat(observedSignature.get())
                        .isEqualTo(
                                "egon.rpc.fixture.v1.UnaryFixtureService/Echo:hello"
                        );
                assertThat(observedDeadlineMs.get())
                        .isPositive()
                        .isLessThanOrEqualTo(settings.deadlineMs());
                assertThat(handle.channel().isShutdown()).isFalse();
            }
        } finally {
            server.shutdownNow().awaitTermination();
        }
    }

    @Test
    void closesOwnedChannelWhenContractValidationFails() {
        AtomicReference<DirectRpcInvocationChannelProvider> created =
                new AtomicReference<>();
        RpcDirectClientFactory factory = new RpcDirectClientFactory(
                new RpcContractValidator(),
                new RpcStatusExceptionMapper(),
                settings -> {
                    DirectRpcInvocationChannelProvider provider =
                            new DirectRpcInvocationChannelProvider(settings);
                    created.set(provider);
                    return provider;
                }
        );

        assertThatThrownBy(() -> factory.create(
                InvalidContract.class,
                settings(65535),
                List.of()
        )).isInstanceOf(RuntimeException.class);

        assertThat(created.get()).isNotNull();
        assertThat(created.get().channel().isShutdown()).isTrue();
    }

    private NettyServerBuilder server(
            AtomicReference<String> observedSignature,
            AtomicLong observedDeadlineMs) {
        @SuppressWarnings("unchecked")
        io.grpc.MethodDescriptor<Message, Message> method =
                (io.grpc.MethodDescriptor<Message, Message>)
                        (io.grpc.MethodDescriptor<?, ?>)
                                UnaryFixtureGrpc.getServiceDescriptor()
                                        .getMethods()
                                        .iterator()
                                        .next();
        ServerServiceDefinition service = ServerServiceDefinition.builder(
                        UnaryFixtureGrpc.getServiceDescriptor().getName()
                )
                .addMethod(
                        method,
                        ServerCalls.asyncUnaryCall((request, observer) -> {
                            observedDeadlineMs.set(Context.current()
                                    .getDeadline()
                                    .timeRemaining(TimeUnit.MILLISECONDS));
                            observer.onNext(StringValue.of(
                                    "direct:"
                                            + ((StringValue) request).getValue()
                            ));
                            observer.onCompleted();
                        })
                )
                .build();
        ServerInterceptor headers = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata metadata,
                    ServerCallHandler<ReqT, RespT> next) {
                observedSignature.set(metadata.get(SIGNATURE));
                return next.startCall(call, metadata);
            }
        };
        return NettyServerBuilder.forPort(0)
                .addService(service)
                .intercept(headers);
    }

    private RpcDirectClientSettings settings(int port) {
        return new RpcDirectClientSettings(
                "localhost:" + port,
                processIdentity(),
                RpcTransportSecurity.developmentPlaintextConfig(),
                500,
                "round_robin",
                1024 * 1024,
                1000
        );
    }

    private RpcProcessIdentity processIdentity() {
        return new RpcProcessIdentity(
                "direct-test",
                "test",
                "default",
                "127.0.0.1",
                1,
                "direct-1"
        );
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    private interface EchoContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    private interface InvalidContract {

        StringValue echo(StringValue request);
    }
}
