package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.engine.discovery.ProviderCallOutcome;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficPolicyCompiler;
import top.egon.cola.component.gateway.engine.traffic.RuntimeTrafficPolicy;
import top.egon.cola.component.rpc.context.invocation.RpcFailureStage;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RpcGatewayServerTest {

    @Test
    void marksGatewayGeneratedFailuresWithGatewayStage() throws Exception {
        RpcProviderChannelCache channels = new RpcProviderChannelCache(
                Duration.ofSeconds(1)
        );
        RpcGatewayForwarder forwarder = new RpcGatewayForwarder(
                ignored -> {
                    throw new AssertionError("provider must not be selected");
                },
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
            consumer = ManagedChannelBuilder.forAddress(
                            "127.0.0.1",
                            gateway.port()
                    )
                    .usePlaintext()
                    .build();
            Metadata headers = new Metadata();
            headers.put(RpcMetadataKeys.SERVICE, "wrong-service");
            Channel callChannel = ClientInterceptors.intercept(
                    consumer,
                    MetadataUtils.newAttachHeadersInterceptor(headers)
            );

            StatusRuntimeException failure = assertThrows(
                    StatusRuntimeException.class,
                    () -> ClientCalls.blockingUnaryCall(
                            callChannel,
                            RawByteMarshaller.INSTANCE.descriptor(
                                    "test.Echo/Call"
                            ),
                            io.grpc.CallOptions.DEFAULT,
                            new byte[]{1}
                    )
            );

            assertEquals(
                    RpcFailureStage.GATEWAY,
                    RpcFailureStage.from(failure.getTrailers())
                            .orElseThrow()
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
        }
    }

    @Test
    void forcesProviderStageForProxiedProviderFailure() throws Exception {
        MethodDescriptor<byte[], byte[]> method =
                RawByteMarshaller.INSTANCE.descriptor("test.Echo/Call");
        Metadata misleadingTrailers = new Metadata();
        misleadingTrailers.put(RpcMetadataKeys.FAILURE_STAGE, "gateway");
        Server providerServer = NettyServerBuilder.forPort(0)
                .addService(ServerServiceDefinition.builder("test.Echo")
                        .addMethod(
                                method,
                                ServerCalls.asyncUnaryCall(
                                        (request, observer) -> observer.onError(
                                                Status.UNAVAILABLE
                                                        .asRuntimeException(
                                                                misleadingTrailers
                                                        )
                                        )
                                )
                        )
                        .build())
                .build()
                .start();
        RpcProviderChannelCache channels = new RpcProviderChannelCache(
                Duration.ofSeconds(1)
        );
        RpcGatewayForwarder forwarder = new RpcGatewayForwarder(
                ignored -> new ProviderSelectionHandle(
                        provider("lease-failure", providerServer.getPort()),
                        () -> {
                        }
                ),
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
            consumer = ManagedChannelBuilder.forAddress(
                            "127.0.0.1",
                            gateway.port()
                    )
                    .usePlaintext()
                    .build();
            ManagedChannel activeConsumer = consumer;

            StatusRuntimeException failure = assertThrows(
                    StatusRuntimeException.class,
                    () -> ClientCalls.blockingUnaryCall(
                            activeConsumer,
                            method,
                            io.grpc.CallOptions.DEFAULT,
                            new byte[]{1}
                    )
            );

            assertEquals(
                    RpcFailureStage.PROVIDER,
                    RpcFailureStage.from(failure.getTrailers())
                            .orElseThrow()
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
        List<top.egon.cola.component.gateway.contract.observability
                .GatewayCallEventV1> events = new CopyOnWriteArrayList<>();
        List<ProviderCallOutcome> outcomes = new CopyOnWriteArrayList<>();
        RpcGatewayForwarder forwarder = new RpcGatewayForwarder(
                ignored -> new ProviderSelectionHandle(provider, () -> {
                }),
                channels,
                Duration.ofSeconds(5),
                1024,
                (route, metadata, traceId, deadline) ->
                        reactor.core.publisher.Mono.just(
                                GatewayRpcSecurityProcessor.Outcome.anonymous()
                ),
                events::add,
                "engine-1",
                GatewayTrafficGovernance.noop(),
                (runtimeIdentity, outcome) -> outcomes.add(outcome)
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
            assertEquals(1, events.size());
            assertEquals(
                    "SUCCESS",
                    events.getFirst().result().category()
            );
            assertEquals(1, events.getFirst().attempts().size());
            assertEquals(List.of(ProviderCallOutcome.SUCCESS), outcomes);
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

    @Test
    void retriesExplicitlyIdempotentUnaryCallWithinSharedDeadline()
            throws Exception {
        MethodDescriptor<byte[], byte[]> method =
                RawByteMarshaller.INSTANCE.descriptor("test.Echo/Call");
        AtomicInteger providerCalls = new AtomicInteger();
        Server providerServer = NettyServerBuilder.forPort(0)
                .addService(ServerServiceDefinition.builder("test.Echo")
                        .addMethod(
                                method,
                                ServerCalls.asyncUnaryCall(
                                        (request, observer) -> {
                                            if (providerCalls.incrementAndGet()
                                                    == 1) {
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
                        .build())
                .build()
                .start();
        ProviderInstance provider = provider(
                "lease-retry",
                providerServer.getPort()
        );
        RpcProviderChannelCache channels = new RpcProviderChannelCache(
                Duration.ofSeconds(1)
        );
        List<top.egon.cola.component.gateway.contract.observability
                .GatewayCallEventV1> events = new CopyOnWriteArrayList<>();
        RpcGatewayForwarder forwarder = new RpcGatewayForwarder(
                ignored -> new ProviderSelectionHandle(provider, () -> {
                }),
                channels,
                Duration.ofSeconds(5),
                1024,
                (route, metadata, traceId, deadline) ->
                        reactor.core.publisher.Mono.just(
                                GatewayRpcSecurityProcessor.Outcome.anonymous()
                        ),
                events::add,
                "engine-1",
                retryGovernance()
        );
        RpcGatewayHandlerRegistry registry =
                new RpcGatewayHandlerRegistry(forwarder);
        registry.activate(new RpcMethodIndexCompiler().compile(
                List.of(route(Set.of("retry"), true))
        ));
        RpcGatewayServer gateway = new RpcGatewayServer(0, 1024, registry);
        ManagedChannel consumer = null;
        try {
            gateway.start();
            consumer = ManagedChannelBuilder.forAddress(
                            "127.0.0.1",
                            gateway.port()
                    )
                    .usePlaintext()
                    .build();
            byte[] request = "retry".getBytes(StandardCharsets.UTF_8);

            byte[] response = ClientCalls.blockingUnaryCall(
                    consumer,
                    method,
                    io.grpc.CallOptions.DEFAULT,
                    request
            );

            assertArrayEquals(request, response);
            assertEquals(2, providerCalls.get());
            assertEquals(2, events.getFirst().attempts().size());
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
    void mapsSecurityDenialBeforeProviderSelection() throws Exception {
        AtomicBoolean providerSelected = new AtomicBoolean();
        RpcProviderChannelCache channels = new RpcProviderChannelCache(
                Duration.ofSeconds(1)
        );
        RpcGatewayForwarder forwarder = new RpcGatewayForwarder(
                ignored -> {
                    providerSelected.set(true);
                    throw new AssertionError(
                            "provider must not be selected"
                    );
                },
                channels,
                Duration.ofSeconds(5),
                1024,
                (route, metadata, traceId, deadline) ->
                        reactor.core.publisher.Mono.error(
                                top.egon.cola.component.gateway.engine.security
                                        .GatewaySecurityException
                                        .authorizationDenied()
                        )
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
            consumer = ManagedChannelBuilder.forAddress(
                            "127.0.0.1",
                            gateway.port()
                    )
                    .usePlaintext()
                    .build();
            ManagedChannel activeConsumer = consumer;

            StatusRuntimeException failure = assertThrows(
                    StatusRuntimeException.class,
                    () -> ClientCalls.blockingUnaryCall(
                            activeConsumer,
                            RawByteMarshaller.INSTANCE.descriptor(
                                    "test.Echo/Call"
                            ),
                            io.grpc.CallOptions.DEFAULT,
                            new byte[]{1}
                    )
            );

            assertEquals(
                    Status.Code.PERMISSION_DENIED,
                    failure.getStatus().getCode()
            );
            assertEquals(false, providerSelected.get());
        } finally {
            if (consumer != null) {
                consumer.shutdownNow().awaitTermination(
                        1,
                        TimeUnit.SECONDS
                );
            }
            gateway.close();
            channels.close();
        }
    }

    private RuntimeRpcRoute route() {
        return route(Set.of(), false);
    }

    private RuntimeRpcRoute route(
            Set<String> policyRefs,
            boolean idempotent) {
        return new RuntimeRpcRoute(
                "route",
                "operation",
                "test.Echo/Call",
                key(),
                "bytes",
                "bytes",
                "sha",
                policyRefs,
                GatewayResponseMode.TRANSPARENT,
                idempotent,
                Duration.ofSeconds(3)
        );
    }

    private GatewayTrafficGovernance retryGovernance() {
        RuntimeTrafficPolicy retry = new GatewayTrafficPolicyCompiler()
                .compile(List.of(new GatewayRuntimePolicy(
                        "retry",
                        "RETRY",
                        "OPERATION",
                        Map.of(
                                "maxAttempts", 2,
                                "initialBackoff", "PT0S",
                                "maximumBackoff", "PT0S",
                                "minimumAttemptBudget", "PT0.001S",
                                "retryableRpcStatuses",
                                List.of("UNAVAILABLE")
                        )
                ))).get("retry");
        GatewayRuleContent content = new GatewayRuleContent(
                "group",
                "group",
                "test",
                "default",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        CompiledGatewayRules rules = new CompiledGatewayRules(
                new GatewayRuleSnapshot(
                        "v1",
                        "release",
                        Instant.EPOCH,
                        "content",
                        "artifact",
                        content
                ),
                new HttpRouteCompiler().compile(List.of()),
                RpcMethodIndex.empty(),
                Set.of(),
                Map.of(),
                Map.of("retry", retry),
                Map.of(),
                Map.of()
        );
        return new GatewayTrafficGovernance(() -> rules, null);
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
}
