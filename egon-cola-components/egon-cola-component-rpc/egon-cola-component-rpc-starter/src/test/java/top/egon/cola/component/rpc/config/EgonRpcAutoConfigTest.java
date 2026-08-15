package top.egon.cola.component.rpc.config;

import com.google.protobuf.StringValue;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.test.util.ReflectionTestUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcDirectReference;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.channel.RpcEndpoint;
import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewayDirectory;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewayEndpoint;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewaySnapshot;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.consumer.provider.RpcConsumerProviderManager;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderDirectory;
import top.egon.cola.component.rpc.consumer.provider.RpcProviderSnapshot;
import top.egon.cola.component.rpc.consumer.proxy.RpcDirectReferenceProxyFactory;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EgonRpcAutoConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            EgonRpcAutoConfig.class
                    ))
                    .withPropertyValues(
                            "egon.cola.component.rpc.enabled=true",
                            "egon.cola.component.rpc.consumer.enabled=true",
                            "egon.cola.component.rpc.identity.env=test",
                            "egon.cola.component.rpc.consumer.gateway-discovery-timeout-ms=100",
                            "spring.application.name=consumer-test"
                    )
                    .withBean(
                            RpcConsumerChannelFactory.class,
                            TestChannelFactory::new
                    );

    @Test
    void directOnlyDoesNotRequireGatewayDirectory() {
        SnapshotProviderDirectory directory =
                new SnapshotProviderDirectory();

        contextRunner
                .withBean(RpcProviderDirectory.class, () -> directory)
                .withUserConfiguration(DirectReferenceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(
                            RpcConsumerProviderManager.class
                    );
                    assertThat(context).doesNotHaveBean(
                            RpcConsumerGatewayManager.class
                    );
                    assertThat(context.getBean(
                            DirectReferences.class
                    ).reference).isNotNull();
                    assertThat(directory.subscribeCount.get()).isOne();
                    assertThat(directory.query.bizCode())
                            .isEqualTo("commerce");
                    assertThat(directory.query.appCode())
                            .isEqualTo("orders");
                    assertThat(directory.query.env()).isEqualTo("test");
                    assertThat(directory.query.serviceName()).isEqualTo(
                            "egon.rpc.fixture.v1.UnaryFixtureService"
                    );
                    assertThat(directory.query.group())
                            .isEqualTo("default");
                    assertThat(directory.query.version())
                            .isEqualTo("1.0.0");
                    assertThat(directory.query.protocol())
                            .isEqualTo("grpc");
                });
    }

    @Test
    void gatewayOnlyKeepsExistingPath() {
        SnapshotGatewayDirectory directory =
                new SnapshotGatewayDirectory();

        contextRunner
                .withBean(RpcGatewayDirectory.class, () -> directory)
                .withUserConfiguration(GatewayReferenceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(
                            RpcConsumerGatewayManager.class
                    );
                    assertThat(context).doesNotHaveBean(
                            RpcConsumerProviderManager.class
                    );
                    assertThat(Proxy.isProxyClass(context.getBean(
                            GatewayReferences.class
                    ).reference.getClass())).isTrue();
                    assertThat(directory.subscribeCount.get()).isOne();
                });
    }

    @Test
    void bothModesCoexist() {
        SnapshotGatewayDirectory gatewayDirectory =
                new SnapshotGatewayDirectory();
        SnapshotProviderDirectory providerDirectory =
                new SnapshotProviderDirectory();

        contextRunner
                .withBean(
                        RpcGatewayDirectory.class,
                        () -> gatewayDirectory
                )
                .withBean(
                        RpcProviderDirectory.class,
                        () -> providerDirectory
                )
                .withBean("firstRpcInterceptor", OrderedInterceptor.class,
                        () -> new OrderedInterceptor(1))
                .withBean("secondRpcInterceptor", OrderedInterceptor.class,
                        () -> new OrderedInterceptor(2))
                .withUserConfiguration(BothReferenceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(
                            RpcConsumerGatewayManager.class
                    );
                    assertThat(context).hasSingleBean(
                            RpcConsumerProviderManager.class
                    );
                    BothReferences references = context.getBean(
                            BothReferences.class
                    );
                    assertThat(Proxy.isProxyClass(
                            references.gatewayReference.getClass()
                    )).isTrue();
                    assertThat(Proxy.isProxyClass(
                            references.directReference.getClass()
                    )).isTrue();
                    assertInterceptorOrders(
                            references.gatewayReference,
                            1,
                            2
                    );
                    assertInterceptorOrders(
                            references.directReference,
                            1,
                            2
                    );
                    assertThat(gatewayDirectory.subscribeCount.get()).isOne();
                    assertThat(providerDirectory.subscribeCount.get()).isOne();
                });
    }

    @Test
    void missingSelectedDirectoryFailsAtInjection() {
        contextRunner
                .withUserConfiguration(DirectReferenceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("directReferences")
                            .hasStackTraceContaining("reference")
                            .hasStackTraceContaining(
                                    "@EgonRpcDirectReference"
                            );
                });
    }

    @SuppressWarnings("unchecked")
    private void assertInterceptorOrders(
            SampleContract reference,
            Integer... orders) {
        Object invocationHandler = Proxy.getInvocationHandler(reference);
        List<RpcClientInterceptorFactory> factories =
                (List<RpcClientInterceptorFactory>) ReflectionTestUtils
                        .getField(invocationHandler, "interceptorFactories");
        assertThat(factories)
                .extracting(factory -> ((Ordered) factory).getOrder())
                .containsExactly(orders);
    }

    @Configuration(proxyBeanMethods = false)
    static class DirectReferenceConfiguration {

        @Bean
        DirectReferences directReferences() {
            return new DirectReferences();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class GatewayReferenceConfiguration {

        @Bean
        GatewayReferences gatewayReferences() {
            return new GatewayReferences();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BothReferenceConfiguration {

        @Bean
        BothReferences bothReferences() {
            return new BothReferences();
        }
    }

    static final class DirectReferences {

        @EgonRpcDirectReference(
                bizCode = "commerce",
                appCode = "orders"
        )
        private SampleContract reference;
    }

    static final class GatewayReferences {

        @EgonRpcReference
        private SampleContract reference;
    }

    static final class BothReferences {

        @EgonRpcReference
        private SampleContract gatewayReference;

        @EgonRpcDirectReference(
                bizCode = "commerce",
                appCode = "orders"
        )
        private SampleContract directReference;
    }

    @EgonRpcService(
            grpcClass = UnaryFixtureGrpc.class,
            group = "default",
            version = "1.0.0"
    )
    interface SampleContract {

        @EgonRpcMethod(name = "Echo")
        StringValue echo(StringValue request);
    }

    private static final class SnapshotGatewayDirectory
            implements RpcGatewayDirectory {

        private final AtomicInteger subscribeCount = new AtomicInteger();

        @Override
        public top.egon.cola.component.rpc.consumer.gateway.RpcGatewaySubscription
                subscribe(
                top.egon.cola.component.rpc.consumer.gateway.RpcGatewayQuery query,
                java.util.function.Consumer<RpcGatewaySnapshot> listener) {
            subscribeCount.incrementAndGet();
            Instant now = Instant.now();
            listener.accept(new RpcGatewaySnapshot(
                    1,
                    now,
                    List.of(new RpcGatewayEndpoint(
                            "gateway-1",
                            "lease-1",
                            "127.0.0.1",
                            19090,
                            false,
                            now.plusSeconds(30)
                    ))
            ));
            return () -> {
            };
        }
    }

    private static final class SnapshotProviderDirectory
            implements RpcProviderDirectory {

        private final AtomicInteger subscribeCount = new AtomicInteger();

        private top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery
                query;

        @Override
        public top.egon.cola.component.rpc.consumer.provider.RpcProviderSubscription
                subscribe(
                top.egon.cola.component.rpc.consumer.provider.RpcProviderQuery query,
                java.util.function.Consumer<RpcProviderSnapshot> listener) {
            subscribeCount.incrementAndGet();
            this.query = query;
            listener.accept(new RpcProviderSnapshot(
                    1,
                    Instant.now(),
                    List.of()
            ));
            return () -> {
            };
        }
    }

    private static final class TestChannelFactory
            extends RpcConsumerChannelFactory {

        @Override
        public ManagedChannel create(RpcEndpoint endpoint) {
            ManagedChannel channel = mock(ManagedChannel.class);
            when(channel.getState(true)).thenReturn(ConnectivityState.READY);
            return channel;
        }

        @Override
        public boolean awaitReady(ManagedChannel channel, long timeoutMs) {
            return true;
        }
    }

    private record OrderedInterceptor(int order)
            implements RpcClientInterceptorFactory, Ordered {

        @Override
        public io.grpc.ClientInterceptor create(
                top.egon.cola.component.rpc.consumer.interceptor.RpcClientInvocation invocation) {
            return null;
        }

        @Override
        public int getOrder() {
            return order;
        }
    }
}
