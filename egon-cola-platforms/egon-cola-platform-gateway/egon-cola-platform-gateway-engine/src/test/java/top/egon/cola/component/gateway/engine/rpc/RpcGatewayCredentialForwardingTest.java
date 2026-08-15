package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class RpcGatewayCredentialForwardingTest {

    private static final String TOKEN = "verified.header.payload.signature";

    @Test
    void forwardsVerifiedBearerWhenPresent() throws Exception {
        GatewayRpcSecurityProcessor.Outcome security =
                new GatewayRpcSecurityProcessor.Outcome(
                        TrustedIdentity.empty(),
                        Set.of("authorization"),
                        new GatewayCredential("bearer", TOKEN, Map.of())
                );

        Invocation result = invoke(security, "forged-inbound-token", false);

        assertEquals("Bearer " + TOKEN, result.authorization());
        assertEquals(Status.Code.OK, result.status());
    }

    @Test
    void doesNotForwardRawInboundBearer() throws Exception {
        Invocation result = invoke(
                GatewayRpcSecurityProcessor.Outcome.anonymous(),
                "forged-inbound-token",
                false
        );

        assertNull(result.authorization());
        assertEquals(Status.Code.OK, result.status());
    }

    @Test
    void doesNotLogBearerOnFailure() throws Exception {
        GatewayRpcSecurityProcessor.Outcome security =
                new GatewayRpcSecurityProcessor.Outcome(
                        TrustedIdentity.empty(),
                        Set.of("authorization"),
                        new GatewayCredential("bearer", TOKEN, Map.of())
                );

        Invocation result = invoke(security, "forged-inbound-token", true);

        assertEquals(Status.Code.UNAVAILABLE, result.status());
        assertFalse(result.events().toString().contains(TOKEN));
        assertFalse(result.errorMessage().contains(TOKEN));
        assertFalse(security.toString().contains(TOKEN));
    }

    private Invocation invoke(
            GatewayRpcSecurityProcessor.Outcome security,
            String inboundToken,
            boolean providerFailure
    ) throws Exception {
        MethodDescriptor<byte[], byte[]> method =
                RawByteMarshaller.INSTANCE.descriptor("test.Echo/Call");
        AtomicReference<String> authorization = new AtomicReference<>();
        Server providerServer = NettyServerBuilder.forPort(0)
                .addService(ServerInterceptors.intercept(
                        ServerServiceDefinition.builder("test.Echo")
                                .addMethod(
                                        method,
                                        ServerCalls.asyncUnaryCall(
                                                (request, observer) -> {
                                                    if (providerFailure) {
                                                        observer.onError(
                                                                Status.UNAVAILABLE
                                                                        .asRuntimeException()
                                                        );
                                                        return;
                                                    }
                                                    observer.onNext(request);
                                                    observer.onCompleted();
                                                }
                                        )
                                )
                                .build(),
                        new ServerInterceptor() {
                            @Override
                            public <RequestT, ResponseT>
                                    ServerCall.Listener<RequestT> interceptCall(
                                            ServerCall<RequestT, ResponseT> call,
                                            Metadata headers,
                                            ServerCallHandler<RequestT, ResponseT> next
                                    ) {
                                authorization.set(headers.get(
                                        RpcMetadataKeys.AUTHORIZATION
                                ));
                                return next.startCall(call, headers);
                            }
                        }
                ))
                .build()
                .start();
        RpcProviderChannelCache channels = new RpcProviderChannelCache(
                Duration.ofSeconds(1)
        );
        List<GatewayCallEventV1> events = new CopyOnWriteArrayList<>();
        RpcGatewayForwarder forwarder = new RpcGatewayForwarder(
                ignored -> new ProviderSelectionHandle(
                        provider(providerServer.getPort()),
                        () -> {
                        }
                ),
                channels,
                Duration.ofSeconds(5),
                1024,
                (route, metadata, traceId, deadline) -> Mono.just(security),
                events::add,
                "engine-1",
                GatewayTrafficGovernance.noop()
        );
        RpcGatewayHandlerRegistry registry =
                new RpcGatewayHandlerRegistry(forwarder);
        registry.activate(new RpcMethodIndexCompiler().compile(
                List.of(route())
        ));
        RpcGatewayServer gateway = new RpcGatewayServer(0, 1024, registry);
        ManagedChannel consumer = null;
        Status.Code status = Status.Code.OK;
        String errorMessage = "";
        try {
            gateway.start();
            consumer = ManagedChannelBuilder.forAddress(
                            "127.0.0.1",
                            gateway.port()
                    )
                    .usePlaintext()
                    .build();
            Metadata inbound = new Metadata();
            inbound.put(
                    RpcMetadataKeys.AUTHORIZATION,
                    "Bearer " + inboundToken
            );
            Channel channel = ClientInterceptors.intercept(
                    consumer,
                    MetadataUtils.newAttachHeadersInterceptor(inbound)
            );
            try {
                ClientCalls.blockingUnaryCall(
                        channel,
                        method,
                        io.grpc.CallOptions.DEFAULT,
                        new byte[]{1}
                );
            } catch (StatusRuntimeException failure) {
                status = failure.getStatus().getCode();
                errorMessage = failure.getMessage();
            }
            return new Invocation(
                    authorization.get(),
                    List.copyOf(events),
                    status,
                    errorMessage == null ? "" : errorMessage
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

    private RuntimeRpcRoute route() {
        return new RuntimeRpcRoute(
                "route",
                "operation",
                "test.Echo/Call",
                serviceKey(),
                "bytes",
                "bytes",
                "sha",
                Set.of(),
                GatewayResponseMode.TRANSPARENT,
                false,
                Duration.ofSeconds(3)
        );
    }

    private ProviderInstance provider(int port) {
        return new ProviderInstance(
                serviceKey(),
                "provider",
                "lease",
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

    private ProviderServiceKey serviceKey() {
        return new ProviderServiceKey(
                "test-biz",
                "test-app",
                "test",
                "default",
                ProviderProtocolType.RPC,
                "test.Echo",
                "default",
                "v1",
                "grpc"
        );
    }

    private record Invocation(
            String authorization,
            List<GatewayCallEventV1> events,
            Status.Code status,
            String errorMessage
    ) {
    }
}
