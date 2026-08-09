package top.egon.cola.component.rpc.consumer;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.autoconfigure.DdcProperties;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcFailureStage;
import top.egon.cola.component.rpc.context.RpcMetadataKeys;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class RpcConsumerInvocationHandlerTest {

    @Test
    void shouldRetryIdempotentGatewayFailureWithOriginalCallContext()
            throws Exception {
        ScenarioResult result = executeScenario(
                true,
                Status.UNAVAILABLE,
                RpcFailureStage.GATEWAY,
                120
        );

        assertThat(result.failure()).isNull();
        assertThat(result.response().getValue()).isEqualTo("gateway:hello");
        assertThat(result.calls()).containsExactly(1, 1);
        assertThat(result.requests()).containsExactly("hello", "hello");
        assertThat(result.invocationIds())
                .hasSize(2)
                .allMatch(result.invocationIds().getFirst()::equals);
        assertThat(result.remainingDeadlineMs()).hasSize(2);
        assertThat(result.remainingDeadlineMs().get(1))
                .isLessThan(result.remainingDeadlineMs().getFirst());
        assertThat(result.elapsedMs()).isLessThan(500);
    }

    @Test
    void shouldNotRetryNonIdempotentGatewayFailure() throws Exception {
        ScenarioResult result = executeScenario(
                false,
                Status.UNAVAILABLE,
                RpcFailureStage.GATEWAY,
                0
        );

        assertThat(result.failure().getCode())
                .isEqualTo(EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE);
        assertThat(result.calls()).containsExactly(1, 0);
        assertThat(result.activeGateways()).isEqualTo(2);
    }

    @Test
    void shouldNotRetryProviderFailure() throws Exception {
        ScenarioResult result = executeScenario(
                true,
                Status.UNAVAILABLE,
                RpcFailureStage.PROVIDER,
                0
        );

        assertThat(result.failure().getCode())
                .isEqualTo(EgonRpcErrorCode.RPC_PROVIDER_UNAVAILABLE);
        assertThat(result.calls()).containsExactly(1, 0);
        assertThat(result.activeGateways()).isEqualTo(2);
    }

    @Test
    void shouldNotRetryUnavailableWithoutFailureStage() throws Exception {
        ScenarioResult result = executeScenario(
                true,
                Status.UNAVAILABLE,
                null,
                0
        );

        assertThat(result.failure().getCode())
                .isEqualTo(EgonRpcErrorCode.RPC_GATEWAY_UNAVAILABLE);
        assertThat(result.calls()).containsExactly(1, 0);
        assertThat(result.activeGateways()).isEqualTo(2);
    }

    @Test
    void shouldNotRetryDeadlineExceeded() throws Exception {
        ScenarioResult result = executeScenario(
                true,
                Status.DEADLINE_EXCEEDED,
                RpcFailureStage.GATEWAY,
                0
        );

        assertThat(result.failure().getCode())
                .isEqualTo(EgonRpcErrorCode.RPC_DEADLINE_EXCEEDED);
        assertThat(result.calls()).containsExactly(1, 0);
        assertThat(result.activeGateways()).isEqualTo(2);
    }

    @Test
    void shouldCallGatewayWithGeneratedDescriptorAndFrameworkMetadata()
            throws Exception {
        AtomicReference<String> observedService = new AtomicReference<>();
        List<String> invocationIds = new CopyOnWriteArrayList<>();
        Server server = startGateway(observedService, invocationIds);
        RpcConsumerGatewayManager manager = null;
        try {
            EgonRpcProperties properties = new EgonRpcProperties();
            properties.getConsumer().setGatewayDiscoveryTimeoutMs(1000);
            GatewayRegistry registry = new GatewayRegistry(server.getPort());
            RpcProcessIdentity identity = new RpcProcessIdentity(
                    "consumer-test",
                    "test",
                    "default",
                    "127.0.0.1",
                    1,
                    "consumer-1"
            );
            manager = new RpcConsumerGatewayManager(
                    registry,
                    new RpcConsumerChannelFactory(),
                    properties,
                    identity,
                    serviceKeyFactory()
            );
            manager.start();
            RpcConsumerProxyFactory proxyFactory = new RpcConsumerProxyFactory(
                    new RpcContractValidator(),
                    manager,
                    identity,
                    new RpcStatusExceptionMapper(),
                    1000
            );
            EchoConsumer proxy = proxyFactory.create(EchoConsumer.class, 500);

            StringValue response = proxy.echo(StringValue.of("hello"));
            proxy.echo(StringValue.of("again"));

            assertThat(response.getValue()).isEqualTo("gateway:hello");
            assertThat(observedService.get())
                    .isEqualTo("egon.rpc.fixture.v1.UnaryFixtureService");
            assertThat(proxy.toString()).contains(EchoConsumer.class.getName());
            assertThat(invocationIds)
                    .hasSize(2)
                    .doesNotHaveDuplicates();
        } finally {
            if (manager != null) {
                manager.stop();
            }
            server.shutdownNow().awaitTermination();
        }
    }

    private ScenarioResult executeScenario(
            boolean idempotent,
            Status firstStatus,
            RpcFailureStage firstStage,
            long firstDelayMs) throws Exception {
        List<String> invocationIds = new CopyOnWriteArrayList<>();
        List<String> requests = new CopyOnWriteArrayList<>();
        List<Long> remainingDeadlineMs = new CopyOnWriteArrayList<>();
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        Server first = startScenarioGateway(
                firstStatus,
                firstStage,
                firstDelayMs,
                firstCalls,
                invocationIds,
                requests,
                remainingDeadlineMs
        );
        Server second = startScenarioGateway(
                null,
                null,
                0,
                secondCalls,
                invocationIds,
                requests,
                remainingDeadlineMs
        );
        RpcConsumerGatewayManager manager = null;
        try {
            EgonRpcProperties properties = new EgonRpcProperties();
            properties.getConsumer().setGatewayDiscoveryTimeoutMs(1000);
            properties.getConsumer().setGatewayMaxAttempts(2);
            RpcProcessIdentity identity = new RpcProcessIdentity(
                    "consumer-test",
                    "test",
                    "default",
                    "127.0.0.1",
                    1,
                    "consumer-1"
            );
            manager = new RpcConsumerGatewayManager(
                    new GatewayRegistry(first.getPort(), second.getPort()),
                    new RpcConsumerChannelFactory(),
                    properties,
                    identity,
                    serviceKeyFactory()
            );
            manager.start();
            RpcConsumerProxyFactory proxyFactory = new RpcConsumerProxyFactory(
                    new RpcContractValidator(),
                    manager,
                    identity,
                    new RpcStatusExceptionMapper(),
                    500
            );
            StringValue response = null;
            EgonRpcException failure = null;
            long startedAt = System.nanoTime();
            try {
                response = idempotent
                        ? proxyFactory.create(
                                IdempotentEchoConsumer.class,
                                500
                        ).echo(StringValue.of("hello"))
                        : proxyFactory.create(
                                NonIdempotentEchoConsumer.class,
                                500
                        ).echo(StringValue.of("hello"));
            } catch (EgonRpcException exception) {
                failure = exception;
            }
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startedAt
            );
            return new ScenarioResult(
                    response,
                    failure,
                    List.of(firstCalls.get(), secondCalls.get()),
                    List.copyOf(invocationIds),
                    List.copyOf(requests),
                    List.copyOf(remainingDeadlineMs),
                    elapsedMs,
                    manager.endpoints().size()
            );
        } finally {
            if (manager != null) {
                manager.stop();
            }
            first.shutdownNow().awaitTermination();
            second.shutdownNow().awaitTermination();
        }
    }

    private DdcServiceKeyFactory serviceKeyFactory() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("test-biz");
        properties.setAppCode("consumer-test");
        return new DdcServiceKeyFactory(properties);
    }

    private Server startScenarioGateway(
            Status failureStatus,
            RpcFailureStage failureStage,
            long delayMs,
            AtomicInteger calls,
            List<String> invocationIds,
            List<String> requests,
            List<Long> remainingDeadlineMs) throws Exception {
        @SuppressWarnings("unchecked")
        MethodDescriptor<Message, Message> method =
                (MethodDescriptor<Message, Message>) (MethodDescriptor<?, ?>)
                        UnaryFixtureGrpc.getServiceDescriptor()
                                .getMethods()
                                .iterator()
                                .next();
        ServerServiceDefinition service = ServerServiceDefinition.builder(
                        UnaryFixtureGrpc.getServiceDescriptor().getName()
                )
                .addMethod(method, ServerCalls.asyncUnaryCall(
                        (request, observer) -> {
                            calls.incrementAndGet();
                            requests.add(((StringValue) request).getValue());
                            remainingDeadlineMs.add(Context.current()
                                    .getDeadline()
                                    .timeRemaining(TimeUnit.MILLISECONDS));
                            if (delayMs > 0) {
                                try {
                                    Thread.sleep(delayMs);
                                } catch (InterruptedException exception) {
                                    Thread.currentThread().interrupt();
                                    observer.onError(Status.CANCELLED
                                            .asRuntimeException());
                                    return;
                                }
                            }
                            if (failureStatus != null) {
                                Metadata trailers = new Metadata();
                                if (failureStage != null) {
                                    failureStage.put(trailers);
                                }
                                observer.onError(failureStatus
                                        .asRuntimeException(trailers));
                                return;
                            }
                            observer.onNext(StringValue.of(
                                    "gateway:"
                                            + ((StringValue) request).getValue()
                            ));
                            observer.onCompleted();
                        }
                ))
                .build();
        ServerInterceptor metadata = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                invocationIds.add(headers.get(
                        RpcMetadataKeys.INVOCATION_ID
                ));
                return next.startCall(call, headers);
            }
        };
        return NettyServerBuilder.forPort(0)
                .addService(service)
                .intercept(metadata)
                .build()
                .start();
    }

    private Server startGateway(
            AtomicReference<String> observedService,
            List<String> invocationIds)
            throws Exception {
        @SuppressWarnings("unchecked")
        MethodDescriptor<Message, Message> method =
                (MethodDescriptor<Message, Message>) (MethodDescriptor<?, ?>)
                        UnaryFixtureGrpc.getServiceDescriptor()
                                .getMethods()
                                .iterator()
                                .next();
        ServerServiceDefinition service = ServerServiceDefinition.builder(
                        UnaryFixtureGrpc.getServiceDescriptor().getName()
                )
                .addMethod(method, ServerCalls.asyncUnaryCall(
                        (request, observer) -> {
                            StringValue value = (StringValue) request;
                            observer.onNext(StringValue.of(
                                    "gateway:" + value.getValue()
                            ));
                            observer.onCompleted();
                        }
                ))
                .build();
        ServerInterceptor metadata = new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                observedService.set(headers.get(RpcMetadataKeys.SERVICE));
                invocationIds.add(headers.get(
                        RpcMetadataKeys.INVOCATION_ID
                ));
                return next.startCall(call, headers);
            }
        };
        return NettyServerBuilder.forPort(0)
                .addService(service)
                .intercept(metadata)
                .build()
                .start();
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    interface EchoConsumer {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    interface IdempotentEchoConsumer {

        @EgonRpcMethod(name = "Echo", idempotent = true)
        StringValue echo(StringValue request);
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "test",
            version = "1.0.0"
    )
    interface NonIdempotentEchoConsumer {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    private static final class GatewayRegistry
            implements DdcServiceRegistryClient {

        private final int[] ports;

        private GatewayRegistry(int... ports) {
            this.ports = ports;
        }

        @Override
        public DdcRegistrySubscription subscribe(
                DdcServiceKey serviceKey,
                Consumer<DdcServiceSnapshot> listener) {
            Instant now = Instant.now();
            List<DdcServiceInstance> instances = java.util.stream.IntStream
                    .range(0, ports.length)
                    .mapToObj(index -> new DdcServiceInstance(
                            "gateway-" + (index + 1),
                            "lease-" + (index + 1),
                            serviceKey,
                            "127.0.0.1",
                            ports[index],
                            false,
                            java.util.Map.of(),
                            30,
                            10,
                            now,
                            now,
                            now.plusSeconds(30),
                            "UP",
                            1
                    ))
                    .toList();
            listener.accept(new DdcServiceSnapshot(
                    serviceKey,
                    1,
                    instances,
                    now
            ));
            return () -> {
            };
        }

        @Override
        public DdcLeaseSession register(DdcServiceRegistration registration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseOperationResult heartbeat(
                String instanceId,
                String leaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcLeaseOperationResult deregister(
                String instanceId,
                String leaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcServiceCatalogSnapshot getServiceKeys(
                DdcServiceQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DdcRegistrySubscription subscribeServices(
                DdcServiceQuery query,
                Consumer<DdcServiceCatalogSnapshot> listener) {
            throw new UnsupportedOperationException();
        }
    }

    private record ScenarioResult(
            StringValue response,
            EgonRpcException failure,
            List<Integer> calls,
            List<String> invocationIds,
            List<String> requests,
            List<Long> remainingDeadlineMs,
            long elapsedMs,
            int activeGateways) {
    }
}
