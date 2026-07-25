package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpcGatewayServerTest {

    @Test
    void forwardsRawUnaryBytesThroughRealGrpcServers() throws Exception {
        MethodDescriptor<byte[], byte[]> method =
                RawByteMarshaller.INSTANCE.descriptor("test.Echo/Call");
        Server providerServer = NettyServerBuilder.forPort(0)
                .addService(ServerServiceDefinition.builder("test.Echo")
                        .addMethod(
                                method,
                                ServerCalls.asyncUnaryCall(
                                        (request, observer) -> {
                                            observer.onNext(request);
                                            observer.onCompleted();
                                        }
                                )
                        )
                        .build())
                .build()
                .start();
        ProviderInstance provider = provider(
                "lease-a",
                providerServer.getPort()
        );
        RpcProviderChannelCache channels = new RpcProviderChannelCache(
                Duration.ofSeconds(1)
        );
        RpcGatewayForwarder forwarder = new RpcGatewayForwarder(
                ignored -> provider,
                channels,
                Duration.ofSeconds(5),
                1024
        );
        RpcGatewayHandlerRegistry registry =
                new RpcGatewayHandlerRegistry(forwarder);
        registry.activate(new RpcMethodIndexCompiler().compile(
                List.of(route())
        ));
        RpcGatewayServer gateway = new RpcGatewayServer(0, 1024, registry);
        ManagedChannel consumer = null;
        try {
            gateway.start();
            consumer = ManagedChannelBuilder
                    .forAddress("127.0.0.1", gateway.port())
                    .usePlaintext()
                    .build();
            byte[] request = "hello".getBytes(StandardCharsets.UTF_8);

            byte[] response = ClientCalls.blockingUnaryCall(
                    consumer,
                    method,
                    io.grpc.CallOptions.DEFAULT,
                    request
            );

            assertArrayEquals(request, response);
            MethodDescriptor<byte[], byte[]> unknown =
                    RawByteMarshaller.INSTANCE.descriptor(
                            "test.Echo/Unknown"
                    );
            ManagedChannel activeConsumer = consumer;
            StatusRuntimeException failure = assertThrows(
                    StatusRuntimeException.class,
                    () -> ClientCalls.blockingUnaryCall(
                            activeConsumer,
                            unknown,
                            io.grpc.CallOptions.DEFAULT,
                            request
                    )
            );
            assertEquals(
                    Status.Code.UNIMPLEMENTED,
                    failure.getStatus().getCode()
            );
        } finally {
            if (consumer != null) {
                consumer.shutdownNow().awaitTermination(
                        1,
                        TimeUnit.SECONDS
                );
            }
            gateway.close();
            channels.close();
            providerServer.shutdownNow().awaitTermination(
                    1,
                    TimeUnit.SECONDS
            );
        }
    }

    @Test
    void channelCacheDoesNotReuseAddressAcrossLeaseIds() {
        RpcProviderChannelCache channels = new RpcProviderChannelCache(
                Duration.ofMillis(10)
        );
        RpcProviderChannelCache.ChannelHandle first = channels.acquire(
                provider("lease-a", 12345)
        );
        RpcProviderChannelCache.ChannelHandle second = channels.acquire(
                provider("lease-b", 12345)
        );

        assertNotSame(first.channel(), second.channel());
        assertEquals(2, channels.size());
        channels.retainOnly(Set.of());
        first.close();
        second.close();
        assertEquals(0, channels.size());
    }

    private RuntimeRpcRoute route() {
        return new RuntimeRpcRoute(
                "route",
                "operation",
                "test.Echo/Call",
                key(),
                "bytes",
                "bytes",
                "sha",
                Set.of(),
                GatewayResponseMode.TRANSPARENT,
                Duration.ofSeconds(3)
        );
    }

    private ProviderInstance provider(String lease, int port) {
        return new ProviderInstance(
                key(),
                "provider",
                lease,
                "127.0.0.1",
                port,
                false,
                Map.of(),
                Instant.now().plusSeconds(30),
                ProviderRegistryState.REGISTERED,
                ProviderHealthState.HEALTHY,
                ProviderHealthState.HEALTHY
        );
    }

    private ProviderServiceKey key() {
        return new ProviderServiceKey(
                "test",
                "default",
                ProviderProtocolType.RPC,
                "test.Echo",
                "default",
                "v1",
                "grpc"
        );
    }
}
