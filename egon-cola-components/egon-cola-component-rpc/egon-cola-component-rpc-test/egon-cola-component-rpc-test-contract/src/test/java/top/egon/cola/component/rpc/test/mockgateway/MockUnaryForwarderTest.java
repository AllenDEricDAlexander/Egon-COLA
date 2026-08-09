package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.rpc.context.RpcMetadataKeys;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockUnaryForwarderTest {

    @Test
    void shouldForwardBytesToPreselectedProviderOverTcp() throws Exception {
        String fullMethodName = "egon.rpc.test.v1.RawService/Echo";
        MethodDescriptor<byte[], byte[]> method = method(fullMethodName);
        Server provider = NettyServerBuilder.forPort(0)
                .addService(ServerServiceDefinition.builder(
                                "egon.rpc.test.v1.RawService"
                        )
                        .addMethod(method, ServerCalls.asyncUnaryCall(
                                (request, observer) -> {
                                    observer.onNext(("provider:"
                                            + new String(request))
                                            .getBytes());
                                    observer.onCompleted();
                                }
                        ))
                        .build())
                .build()
                .start();
        MockProviderChannelFactory channels =
                new MockProviderChannelFactory();
        try {
            MockProviderEndpoint endpoint = endpoint(provider.getPort());
            CompletableFuture<byte[]> response = new CompletableFuture<>();

            new MockUnaryForwarder(channels).forward(
                    endpoint,
                    fullMethodName,
                    "hello".getBytes(),
                    new StreamObserver<>() {
                        @Override
                        public void onNext(byte[] value) {
                            response.complete(value);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            response.completeExceptionally(throwable);
                        }

                        @Override
                        public void onCompleted() {
                        }
                    }
            );

            assertThat(new String(response.get(2, TimeUnit.SECONDS)))
                    .isEqualTo("provider:hello");
            assertThat(channels.size()).isOne();
        } finally {
            channels.close();
            provider.shutdownNow().awaitTermination();
        }
    }

    @Test
    void shouldMarkDownstreamUnavailableAsProviderFailure() throws Exception {
        String fullMethodName = "egon.rpc.test.v1.RawService/Echo";
        MethodDescriptor<byte[], byte[]> method = method(fullMethodName);
        Server provider = NettyServerBuilder.forPort(0)
                .addService(ServerServiceDefinition.builder(
                                "egon.rpc.test.v1.RawService"
                        )
                        .addMethod(method, ServerCalls.asyncUnaryCall(
                                (request, observer) -> observer.onError(
                                        Status.UNAVAILABLE
                                                .asRuntimeException()
                                )
                        ))
                        .build())
                .build()
                .start();
        MockProviderChannelFactory channels =
                new MockProviderChannelFactory();
        try {
            CompletableFuture<byte[]> response = new CompletableFuture<>();

            new MockUnaryForwarder(channels).forward(
                    endpoint(provider.getPort()),
                    fullMethodName,
                    "hello".getBytes(),
                    new StreamObserver<>() {
                        @Override
                        public void onNext(byte[] value) {
                            response.complete(value);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            response.completeExceptionally(throwable);
                        }

                        @Override
                        public void onCompleted() {
                        }
                    }
            );

            assertThatThrownBy(() -> response.get(2, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(StatusRuntimeException.class)
                    .satisfies(throwable -> {
                        StatusRuntimeException status =
                                (StatusRuntimeException) throwable.getCause();
                        assertThat(status.getTrailers().get(
                                RpcMetadataKeys.FAILURE_STAGE
                        )).isEqualTo("provider");
                    });
        } finally {
            channels.close();
            provider.shutdownNow().awaitTermination();
        }
    }

    private MethodDescriptor<byte[], byte[]> method(String fullMethodName) {
        return MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethodName)
                .setRequestMarshaller(new MockByteArrayMarshaller())
                .setResponseMarshaller(new MockByteArrayMarshaller())
                .build();
    }

    private MockProviderEndpoint endpoint(int port) {
        DdcServiceKey key = new DdcServiceKey(
                "test-biz",
                "test",
                "test-app",
                DdcServiceKind.RPC_PROVIDER,
                "egon.rpc.test.v1.RawService",
                "default",
                "1.0.0",
                "grpc"
        );
        return new MockProviderEndpoint(
                key,
                "provider-1",
                "lease-1",
                "127.0.0.1",
                port,
                false,
                Instant.now().plusSeconds(30)
        );
    }
}
