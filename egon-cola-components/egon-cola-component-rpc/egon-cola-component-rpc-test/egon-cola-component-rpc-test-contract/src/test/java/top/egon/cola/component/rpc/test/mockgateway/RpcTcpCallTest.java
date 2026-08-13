package top.egon.cola.component.rpc.test.mockgateway;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.proxy.EgonRpcReferenceBeanPostProcessor;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.gateway.GatewayRpcInvocationChannelProvider;
import top.egon.cola.component.rpc.consumer.proxy.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.provider.server.RpcProviderServerInterceptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBeanScanner;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLeaseManager;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderLifecycle;
import top.egon.cola.component.rpc.provider.server.RpcProviderServerFactory;
import top.egon.cola.component.rpc.provider.server.RpcServerServiceDefinitionFactory;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.rpc.test.fixture.consumer.EchoRpcTestClient;
import top.egon.cola.component.rpc.test.fixture.provider.EchoRpcTestProvider;
import top.egon.cola.component.rpc.test.support.InMemoryRpcRegistryBackend;
import top.egon.cola.component.rpc.test.support.InMemoryRpcRegistryClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RpcTcpCallTest {

    @Test
    void shouldCallConsumerThroughMockGatewayToProviderOverTcp()
            throws Exception {
        InMemoryRpcRegistryBackend backend =
                new InMemoryRpcRegistryBackend();
        InMemoryRpcRegistryClient providerRegistry =
                new InMemoryRpcRegistryClient(backend);
        InMemoryRpcRegistryClient gatewayRegistry =
                new InMemoryRpcRegistryClient(backend);
        InMemoryRpcRegistryClient consumerRegistry =
                new InMemoryRpcRegistryClient(backend);
        AnnotationConfigApplicationContext providerContext =
                providerContext();
        RpcProviderLifecycle provider = providerLifecycle(
                providerContext,
                providerRegistry,
                "provider-a"
        );
        MockRpcGateway gateway = null;
        RpcConsumerGatewayManager consumerGateway = null;
        GenericApplicationContext consumerContext = null;
        try {
            provider.start();
            gateway = new MockRpcGateway(
                    gatewayRegistry,
                    "test",
                    "mock-gateway-1",
                    MockGatewayProperties.defaults(),
                    List.of(EchoServiceGrpc.getEchoMethod()
                            .getFullMethodName())
            );
            gateway.start();

            EgonRpcProperties consumerProperties = new EgonRpcProperties();
            consumerProperties.getConsumer()
                    .setGatewayDiscoveryTimeoutMs(2000);
            RpcProcessIdentity consumerIdentity = identity(
                    "consumer",
                    "consumer-1"
            );
            consumerGateway = new RpcConsumerGatewayManager(
                    consumerRegistry,
                    new RpcConsumerChannelFactory(),
                    consumerProperties,
                    consumerIdentity
            );
            consumerGateway.start();
            RpcConsumerProxyFactory proxyFactory =
                    new RpcConsumerProxyFactory(
                            new RpcContractValidator(),
                            new GatewayRpcInvocationChannelProvider(
                                    consumerGateway
                            ),
                            consumerIdentity,
                            new RpcStatusExceptionMapper(),
                            3000
                    );
            consumerContext = consumerContext(proxyFactory);
            EchoRpcTestClient client =
                    consumerContext.getBean(EchoRpcTestClient.class);

            EchoResponse response = client.echo("hello");

            assertThat(response.getMessage()).isEqualTo("hello");
            assertThat(response.getProviderId()).isEqualTo("provider-a");
            assertThat(gateway.invocations()).hasSize(1);
            assertThat(gateway.invocations().getFirst().fullMethodName())
                    .isEqualTo(
                            "egon.rpc.test.v1.EchoService/Echo"
                    );
            assertThat(gateway.invocations().getFirst().providerInstanceId())
                    .contains("provider-process");
            assertThat(gateway.channelFactory().size()).isOne();
            assertThat(consumerRegistry.subscribedQueries())
                    .allSatisfy(query -> assertThat(query.serviceName())
                            .isEqualTo("egon-gateway-rpc"));
            assertThat(backend.allInstances())
                    .extracting(instance -> instance.serviceIdentity())
                    .containsExactlyInAnyOrder(
                            new RpcServiceIdentity(
                                    "egon.rpc.test.v1.EchoService",
                                    "default",
                                    "1.0.0"
                            ),
                            new RpcServiceIdentity(
                                    "egon-gateway-rpc",
                                    "default",
                                    "1.0.0"
                            )
                    );
        } finally {
            if (consumerContext != null) {
                consumerContext.close();
            }
            if (consumerGateway != null) {
                consumerGateway.stop();
            }
            if (gateway != null) {
                gateway.close();
            }
            provider.stop();
            providerContext.close();
        }

        assertThat(backend.allInstances()).isEmpty();
    }

    private AnnotationConfigApplicationContext providerContext() {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        context.registerBean(
                EchoRpcTestProvider.class,
                () -> new EchoRpcTestProvider("provider-a")
        );
        context.refresh();
        return context;
    }

    private RpcProviderLifecycle providerLifecycle(
            AnnotationConfigApplicationContext context,
            InMemoryRpcRegistryClient registry,
            String providerId) {
        EgonRpcProperties properties = new EgonRpcProperties();
        properties.getProvider().setEnabled(true);
        properties.getProvider().setBindAddress("127.0.0.1");
        properties.getProvider().setPort(0);
        properties.getProvider().setAdvertisedHost("127.0.0.1");
        properties.getProvider().setGracefulShutdownTimeoutMs(1000);
        RpcProcessIdentity identity = identity(
                providerId,
                "provider-process"
        );
        RpcProviderAvailabilityRegistry availability =
                new RpcProviderAvailabilityRegistry();
        return new RpcProviderLifecycle(
                new RpcProviderBeanScanner(
                        context,
                        new RpcContractValidator()
                ).scan(),
                new RpcServerServiceDefinitionFactory(availability),
                new RpcProviderServerFactory(),
                new RpcProviderLeaseManager(
                        registry,
                        availability,
                        properties,
                        identity,
                        "test"
                ),
                availability,
                List.of(new RpcProviderServerInterceptor()),
                properties,
                identity
        );
    }

    private GenericApplicationContext consumerContext(
            RpcConsumerProxyFactory proxyFactory) {
        GenericApplicationContext context =
                new GenericApplicationContext();
        context.registerBean(
                EgonRpcReferenceBeanPostProcessor.class,
                () -> new EgonRpcReferenceBeanPostProcessor(proxyFactory)
        );
        context.registerBean(EchoRpcTestClient.class);
        context.refresh();
        return context;
    }

    private RpcProcessIdentity identity(
            String applicationName,
            String instanceId) {
        return new RpcProcessIdentity(
                applicationName,
                "test",
                "default",
                "127.0.0.1",
                1,
                instanceId
        );
    }
}
