package top.egon.cola.component.rpc.consumer.generic;

import com.google.protobuf.StringValue;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelPool;
import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationExecutor;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationPlan;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancers;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBinding;
import top.egon.cola.component.rpc.provider.binding.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.provider.server.RpcServerServiceDefinitionFactory;
import top.egon.cola.component.rpc.support.RpcProviderTestFixtures;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RpcGenericInvokerTest {

    private static final String SERVICE =
            "egon.rpc.fixture.v1.UnaryFixtureService";

    private static final String METHOD = SERVICE + "/Echo";

    @Test
    void invokesCanonicalRawBytesThroughTheExistingUnaryServer() throws Exception {
        try (RunningServer running = server()) {
            RpcGenericInvocation invocation = invocation(
                    StringValue.of("raw")
            );

            byte[] response = running.invoker.invokeBlocking(invocation);

            assertThat(StringValue.parseFrom(response))
                    .isEqualTo(StringValue.of("provider:raw"));
        }
    }

    @Test
    void invokesAsyncAndReturnsTheSameSerializedResponse() throws Exception {
        try (RunningServer running = server()) {
            CompletionStage<byte[]> response = running.invoker.invokeAsync(
                    invocation(StringValue.of("async"))
            );

            assertThat(StringValue.parseFrom(
                    response.toCompletableFuture().get(5, TimeUnit.SECONDS)))
                    .isEqualTo(StringValue.of("provider:async"));
        }
    }

    @Test
    void invokesDirectGenericTargetWithoutGatewayFallback() throws Exception {
        try (RunningServer running = server()) {
            RpcGenericInvocation invocation = RpcGenericInvocation.direct(
                    "biz",
                    "app",
                    "test",
                    SERVICE,
                    "test",
                    "1.0.0",
                    METHOD,
                    StringValue.of("direct").toByteArray(),
                    2000,
                    0,
                    LoadBalance.ROUND_ROBIN,
                    FailStrategy.FAIL_CLOSED,
                    null
            );

            byte[] response = running.invoker.invokeBlocking(invocation);

            assertThat(StringValue.parseFrom(response))
                    .isEqualTo(StringValue.of("provider:direct"));
        }
    }

    @Test
    void rejectsNonCanonicalTargetsAndExposesNoCallerMetadataSurface() {
        assertThatThrownBy(() -> RpcGenericInvocation.gateway(
                SERVICE,
                "test",
                "1.0.0",
                "egon.rpc.fixture.v1.UnaryFixtureService.Echo",
                new byte[0],
                1000,
                0,
                LoadBalance.ROUND_ROBIN,
                FailStrategy.FAIL_CLOSED,
                null
        ))
                .isInstanceOf(EgonRpcException.class)
                .satisfies(error -> assertThat(((EgonRpcException) error).getCode())
                        .isEqualTo(EgonRpcErrorCode.RPC_INVALID_REQUEST));
        assertThatThrownBy(() -> RpcGenericInvocation.gateway(
                SERVICE,
                "test",
                "1.0.0",
                "other.Service/Echo",
                new byte[0],
                1000,
                0,
                LoadBalance.ROUND_ROBIN,
                FailStrategy.FAIL_CLOSED,
                null
        )).isInstanceOf(EgonRpcException.class);
        assertThat(RpcGenericInvocation.class.getDeclaredFields())
                .noneMatch(field -> Set.of(
                        "metadata", "authorization", "host", "port"
                ).contains(field.getName().toLowerCase()));
    }

    @Test
    void forwardsGenericAsyncCancellationToTheSharedExecutor() {
        RpcConsumerChannelPool pool = mock(RpcConsumerChannelPool.class);
        RpcInvocationExecutor executor = mock(RpcInvocationExecutor.class);
        CompletableFuture<Object> pending = new CompletableFuture<>();
        when(executor.executeAsync(
                any(RpcInvocationPlan.class),
                any()
        )).thenReturn(pending);
        AtomicInteger closes = new AtomicInteger();
        RpcGenericTargetCache cache = new RpcGenericTargetCache(
                ignored -> strategy(List.of(), closes),
                4,
                Duration.ofSeconds(1)
        );
        RpcGenericInvoker invoker = new RpcGenericInvoker(
                cache,
                pool,
                executor,
                identity()
        );

        CompletionStage<byte[]> response = invoker.invokeAsync(
                invocation(StringValue.of("cancel"))
        );

        assertThat(response.toCompletableFuture().cancel(true)).isTrue();
        assertThat(pending.isCancelled()).isTrue();
        cache.close();
        assertThat(closes).hasValue(1);
    }

    @Test
    void failOpenReturnsNullOnlyAfterGenericAvailabilityExhaustion() {
        RpcConsumerChannelPool pool = mock(RpcConsumerChannelPool.class);
        RpcGenericTargetCache cache = new RpcGenericTargetCache(
                ignored -> strategy(List.of(), new AtomicInteger()),
                4,
                Duration.ofSeconds(1)
        );
        RpcGenericInvoker invoker = new RpcGenericInvoker(
                cache,
                pool,
                new RpcInvocationExecutor(new RpcStatusExceptionMapper()),
                identity()
        );

        RpcGenericInvocation invocation = new RpcGenericInvocation(
                RpcReferenceMode.GATEWAY,
                null,
                null,
                null,
                SERVICE,
                "test",
                "1.0.0",
                METHOD,
                StringValue.of("missing").toByteArray(),
                1000,
                0,
                LoadBalance.ROUND_ROBIN,
                FailStrategy.FAIL_OPEN,
                null
        );

        assertThat(invoker.invokeBlocking(invocation)).isNull();
        cache.close();
    }

    private RpcGenericInvocation invocation(StringValue request) {
        return RpcGenericInvocation.gateway(
                SERVICE,
                "test",
                "1.0.0",
                METHOD,
                request.toByteArray(),
                2000,
                0,
                LoadBalance.ROUND_ROBIN,
                FailStrategy.FAIL_CLOSED,
                null
        );
    }

    private RunningServer server() throws Exception {
        RpcContractDescriptor contract = new RpcContractValidator().validate(
                RpcProviderTestFixtures.EchoContract.class
        );
        RpcProviderBinding binding = new RpcProviderBinding(
                new RpcProviderTestFixtures.EchoProvider(),
                contract
        );
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        availability.available(binding.serviceIdentity());
        Server server = NettyServerBuilder.forPort(0)
                .addService(new RpcServerServiceDefinitionFactory(availability)
                        .create(new RpcProviderMethodRegistry(List.of(binding)))
                        .getFirst())
                .build()
                .start();
        RpcEndpoint endpoint = endpoint(server.getPort());
        RpcConsumerChannelPool pool = new RpcConsumerChannelPool(
                new RpcConsumerChannelFactory(),
                1000
        );
        RpcGenericTargetCache cache = new RpcGenericTargetCache(
                ignored -> strategy(
                        ignored.mode(),
                        List.of(endpoint),
                        new AtomicInteger()
                ),
                4,
                Duration.ofSeconds(1)
        );
        return new RunningServer(
                server,
                pool,
                cache,
                new RpcGenericInvoker(
                        cache,
                        pool,
                        new RpcInvocationExecutor(
                                new RpcStatusExceptionMapper()
                        ),
                        identity(),
                        List.<RpcClientInterceptorFactory>of(),
                        new RpcLoadBalancers()
                )
        );
    }

    private RpcReferenceStrategy strategy(
            List<RpcEndpoint> endpoints,
            AtomicInteger closes) {
        return strategy(RpcReferenceMode.GATEWAY, endpoints, closes);
    }

    private RpcReferenceStrategy strategy(
            RpcReferenceMode mode,
            List<RpcEndpoint> endpoints,
            AtomicInteger closes) {
        return new RpcReferenceStrategy() {
            @Override
            public RpcReferenceMode mode() {
                return mode;
            }

            @Override
            public String queryIdentity() {
                return SERVICE;
            }

            @Override
            public long revision() {
                return 1;
            }

            @Override
            public List<? extends RpcEndpoint> candidates() {
                return endpoints;
            }

            @Override
            public void close() {
                closes.incrementAndGet();
            }
        };
    }

    private RpcEndpoint endpoint(int port) {
        return new RpcEndpoint() {
            @Override
            public String host() {
                return "127.0.0.1";
            }

            @Override
            public int port() {
                return port;
            }

            @Override
            public boolean secure() {
                return false;
            }
        };
    }

    private static RpcProcessIdentity identity() {
        return new RpcProcessIdentity(
                "generic-test",
                "test",
                "127.0.0.1",
                1,
                "generic-1"
        );
    }

    private static final class RunningServer implements AutoCloseable {

        private final Server server;
        private final RpcConsumerChannelPool pool;
        private final RpcGenericTargetCache cache;
        private final RpcGenericInvoker invoker;

        private RunningServer(
                Server server,
                RpcConsumerChannelPool pool,
                RpcGenericTargetCache cache,
                RpcGenericInvoker invoker) {
            this.server = server;
            this.pool = pool;
            this.cache = cache;
            this.invoker = invoker;
        }

        @Override
        public void close() throws Exception {
            cache.close();
            pool.close();
            pool.stop();
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
