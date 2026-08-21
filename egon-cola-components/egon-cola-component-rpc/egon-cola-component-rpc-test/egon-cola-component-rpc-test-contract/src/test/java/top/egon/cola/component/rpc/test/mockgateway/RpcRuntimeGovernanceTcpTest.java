package top.egon.cola.component.rpc.test.mockgateway;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.test.util.ReflectionTestUtils;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelPool;
import top.egon.cola.component.rpc.consumer.generic.RpcGenericInvocation;
import top.egon.cola.component.rpc.consumer.generic.RpcGenericInvoker;
import top.egon.cola.component.rpc.consumer.generic.RpcGenericTargetCache;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationExecutor;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalancers;
import top.egon.cola.component.rpc.consumer.provider.RpcConsumerProviderManager;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderDirectory;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderEndpoint;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderSnapshot;
import top.egon.cola.component.rpc.consumer.proxy.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceDefinition;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.consumer.reference.RpcReferencePolicy;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategy;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceStrategyFactory;
import top.egon.cola.component.rpc.config.RpcTransportSecurity;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBeanScanner;
import top.egon.cola.component.rpc.provider.binding.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderLifecycle;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistrationMode;
import top.egon.cola.component.rpc.provider.server.RpcProviderServerFactory;
import top.egon.cola.component.rpc.provider.server.RpcServerServiceDefinitionFactory;
import top.egon.cola.component.rpc.consumer.lifecycle.RpcConsumerLifecycleCoordinator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.test.contract.AsyncEchoRpc;
import top.egon.cola.component.rpc.test.contract.EchoRpc;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RpcRuntimeGovernanceTcpTest {

    @Test
    void typedAsyncGenericAndConcurrentCallsShareOneLoopbackChannel()
            throws Exception {
        AnnotationConfigApplicationContext providerContext =
                providerContext();
        RpcProviderLifecycle provider = providerLifecycle(providerContext);
        RpcConsumerChannelPool pool = null;
        RpcConsumerProviderManager providerManager = null;
        RpcReferenceStrategyFactory strategies = null;
        RpcGenericTargetCache genericTargets = null;
        RpcConsumerLifecycleCoordinator consumerLifecycle = null;
        try {
            provider.start();
            RpcProviderEndpoint endpoint = new RpcProviderEndpoint(
                    "provider-tcp",
                    "lease-tcp",
                    "127.0.0.1",
                    provider.boundPort(),
                    false,
                    Instant.now().plusSeconds(60),
                    100
            );
            RpcProviderDirectory directory = (query, listener) -> {
                listener.accept(new RpcProviderSnapshot(
                        1,
                        Instant.now(),
                        List.of(endpoint)
                ));
                return () -> { };
            };
            EgonRpcProperties consumerProperties = new EgonRpcProperties();
            consumerProperties.getConsumer().setDefaultTimeoutMs(3000);
            RpcConsumerChannelFactory channels = new RpcConsumerChannelFactory(
                    RpcTransportSecurity.developmentPlaintextConfig());
            pool = new RpcConsumerChannelPool(channels, Duration.ofSeconds(2));
            providerManager = new RpcConsumerProviderManager(
                    directory,
                    channels,
                    consumerProperties
            );
            RpcLoadBalancers loadBalancers = new RpcLoadBalancers();
            RpcProviderQuery query = new RpcProviderQuery(
                    "test-biz",
                    "test-app",
                    "test",
                    "egon.rpc.test.v1.EchoService",
                    "default",
                    "1.0.0",
                    "grpc"
            );
            consumerLifecycle = new RpcConsumerLifecycleCoordinator(
                    pool,
                    null,
                    providerManager,
                    List.of()
            );
            consumerLifecycle.start();
            RpcReferenceStrategyFactory strategyFactory =
                    new RpcReferenceStrategyFactory(null, providerManager);
            strategies = strategyFactory;
            RpcInvocationExecutor executor = new RpcInvocationExecutor(
                    consumerLifecycle,
                    new RpcStatusExceptionMapper());
            RpcConsumerProxyFactory proxyFactory = new RpcConsumerProxyFactory(
                    new RpcContractValidator(),
                    pool,
                    executor,
                    identity(),
                    loadBalancers,
                    new RpcStatusExceptionMapper(),
                    3000,
                    List.of()
            );
            EchoRpc blocking = proxy(
                    EchoRpc.class,
                    query,
                    strategyFactory,
                    proxyFactory,
                    loadBalancers
            );
            AsyncEchoRpc async = proxy(
                    AsyncEchoRpc.class,
                    query,
                    strategyFactory,
                    proxyFactory,
                    loadBalancers
            );
            assertThat(ClassUtils.isCglibProxyClass(blocking.getClass())).isTrue();
            assertThat(ClassUtils.isCglibProxyClass(async.getClass())).isTrue();

            EchoResponse response = blocking.echo(request("blocking"));
            assertThat(response.getMessage()).isEqualTo("blocking");

            genericTargets = new RpcGenericTargetCache(
                    strategyFactory,
                    4,
                    Duration.ofSeconds(5),
                    loadBalancers
            );
            RpcGenericInvoker generic = new RpcGenericInvoker(
                    genericTargets,
                    pool,
                    executor,
                    identity(),
                    List.of(),
                    loadBalancers
            );
            byte[] raw = generic.invokeBlocking(RpcGenericInvocation.direct(
                    "test-biz",
                    "test-app",
                    "test",
                    "egon.rpc.test.v1.EchoService",
                    "default",
                    "1.0.0",
                    "egon.rpc.test.v1.EchoService/Echo",
                    request("generic").toByteArray(),
                    3000,
                    0,
                    LoadBalance.ROUND_ROBIN,
                    FailStrategy.FAIL_CLOSED,
                    null
            ));
            assertThat(EchoResponse.parseFrom(raw).getMessage())
                    .isEqualTo("generic");

            ExecutorService concurrent = Executors.newFixedThreadPool(16);
            try {
                List<CompletableFuture<EchoResponse>> calls =
                        java.util.stream.IntStream.range(0, 100)
                                .mapToObj(index -> CompletableFuture.supplyAsync(
                                        () -> blocking.echo(request("call-" + index)),
                                        concurrent))
                                .toList();
                CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new))
                        .join();
                assertThat(calls).allSatisfy(call ->
                        assertThat(call.join().getMessage()).startsWith("call-"));
            } finally {
                concurrent.shutdownNow();
            }

            SlowEchoProvider providerBean = providerContext.getBean(
                    SlowEchoProvider.class);
            providerBean.hold();
            CompletionStage<EchoResponse> asyncResponse =
                    async.echoAsync(request("async"));
            assertThat(asyncResponse.toCompletableFuture()).isNotDone();
            assertThat(providerBean.awaitEntered()).isTrue();
            @SuppressWarnings("unchecked")
            Map<?, ?> entries = (Map<?, ?>) ReflectionTestUtils.getField(
                    pool,
                    "entries");
            assertThat(entries).hasSize(1);
            providerBean.release();
            assertThat(asyncResponse.toCompletableFuture().get().getMessage())
                    .isEqualTo("async");
        } finally {
            if (genericTargets != null) {
                genericTargets.close();
            }
            if (strategies != null) {
                strategies.close();
            }
            if (consumerLifecycle != null) {
                consumerLifecycle.stop();
            } else if (pool != null) {
                pool.close();
            }
            providerContext.getBean(SlowEchoProvider.class).release();
            provider.stop();
            providerContext.close();
        }

        assertThat(providerContext.isActive()).isFalse();
    }

    private EchoRequest request(String message) {
        return EchoRequest.newBuilder().setMessage(message).build();
    }

    private <T> T proxy(
            Class<T> contractType,
            RpcProviderQuery query,
            RpcReferenceStrategyFactory strategyFactory,
            RpcConsumerProxyFactory proxyFactory,
            RpcLoadBalancers loadBalancers) {
        RpcContractDescriptor descriptor = new RpcContractValidator()
                .validate(contractType);
        RpcReferencePolicy policy = new RpcReferencePolicy(
                3000,
                1,
                LoadBalance.ROUND_ROBIN,
                FailStrategy.FAIL_CLOSED,
                "",
                null
        );
        Map<java.lang.reflect.Method, RpcReferencePolicy> policies =
                descriptor.methods().stream().collect(Collectors.toMap(
                        method -> method.javaMethod(),
                        ignored -> policy
                ));
        RpcReferenceDefinition definition = new RpcReferenceDefinition(
                RpcReferenceMode.DIRECT,
                new top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity(
                        descriptor.serviceName(),
                        descriptor.group(),
                        descriptor.version()),
                query,
                policies
        );
        RpcReferenceStrategy strategy = strategyFactory.create(definition);
        return proxyFactory.create(descriptor, definition, strategy);
    }

    private AnnotationConfigApplicationContext providerContext() {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        context.registerBean(
                SlowEchoProvider.class,
                SlowEchoProvider::new
        );
        context.refresh();
        return context;
    }

    private RpcProviderLifecycle providerLifecycle(
            AnnotationConfigApplicationContext context) {
        EgonRpcProperties properties = new EgonRpcProperties();
        properties.getProvider().setEnabled(true);
        properties.getProvider().setBindAddress("127.0.0.1");
        properties.getProvider().setPort(0);
        properties.getProvider().setRegistrationMode(
                RpcProviderRegistrationMode.DISABLED
        );
        properties.getProvider().setGracefulShutdownTimeoutMs(1000);
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        RpcProviderMethodRegistry methods = new RpcProviderBeanScanner(
                context,
                new RpcContractValidator()).scan();
        RpcProcessIdentity identity = identity();
        return new RpcProviderLifecycle(
                methods,
                new RpcServerServiceDefinitionFactory(availability),
                new RpcProviderServerFactory(),
                null,
                availability,
                List.of(),
                properties,
                identity
        );
    }

    private RpcProcessIdentity identity() {
        return new RpcProcessIdentity(
                "tcp-consumer",
                "test",
                "default",
                "127.0.0.1",
                1,
                "tcp-consumer-process"
        );
    }

    @EgonRpcProvider
    static final class SlowEchoProvider implements EchoRpc {

        private final CountDownLatch entered = new CountDownLatch(1);

        private final CountDownLatch release = new CountDownLatch(1);

        private volatile boolean hold;

        @Override
        public EchoResponse echo(EchoRequest request) {
            if (hold) {
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("TCP test release timed out");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("TCP test interrupted", exception);
                }
            }
            return EchoResponse.newBuilder()
                    .setProviderId("tcp-provider")
                    .setMessage(request.getMessage())
                    .build();
        }

        void hold() {
            hold = true;
        }

        boolean awaitEntered() {
            try {
                return entered.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        void release() {
            release.countDown();
        }
    }
}
