package top.egon.cola.component.rpc.test.mockgateway;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.EgonRpcReferenceBeanPostProcessor;
import top.egon.cola.component.rpc.consumer.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.RpcProviderServerInterceptor;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.provider.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.provider.RpcProviderBeanScanner;
import top.egon.cola.component.rpc.provider.RpcProviderLeaseManager;
import top.egon.cola.component.rpc.provider.RpcProviderLifecycle;
import top.egon.cola.component.rpc.provider.RpcProviderServerFactory;
import top.egon.cola.component.rpc.provider.RpcServerServiceDefinitionFactory;
import top.egon.cola.component.rpc.test.consumer.EchoRpcClient;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.rpc.test.provider.EchoRpcProvider;
import top.egon.cola.component.rpc.test.support.InMemoryDdcRegistryBackend;
import top.egon.cola.component.rpc.test.support.InMemoryDdcServiceRegistryClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RpcTcpCallTest {

    @Test
    void shouldCallConsumerThroughMockGatewayToProviderOverTcp()
            throws Exception {
        InMemoryDdcRegistryBackend backend =
                new InMemoryDdcRegistryBackend();
        InMemoryDdcServiceRegistryClient providerRegistry =
                new InMemoryDdcServiceRegistryClient(backend);
        InMemoryDdcServiceRegistryClient gatewayRegistry =
                new InMemoryDdcServiceRegistryClient(backend);
        InMemoryDdcServiceRegistryClient consumerRegistry =
                new InMemoryDdcServiceRegistryClient(backend);
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
                    "default",
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
                            consumerGateway,
                            consumerIdentity,
                            new RpcStatusExceptionMapper(),
                            3000
                    );
            consumerContext = consumerContext(proxyFactory);
            EchoRpcClient client =
                    consumerContext.getBean(EchoRpcClient.class);

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
            assertThat(consumerRegistry.subscribedKeys())
                    .allSatisfy(key -> assertThat(key.serviceKind())
                            .isEqualTo(DdcServiceKind.INTERNAL_GATEWAY));
            assertThat(backend.allInstances())
                    .extracting(instance -> instance.serviceKey().serviceKind())
                    .containsExactlyInAnyOrder(
                            DdcServiceKind.RPC_PROVIDER,
                            DdcServiceKind.INTERNAL_GATEWAY
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
                EchoRpcProvider.class,
                () -> new EchoRpcProvider("provider-a")
        );
        context.refresh();
        return context;
    }

    private RpcProviderLifecycle providerLifecycle(
            AnnotationConfigApplicationContext context,
            InMemoryDdcServiceRegistryClient registry,
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
                ),
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
                new RpcProviderServerInterceptor(),
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
        context.registerBean(EchoRpcClient.class);
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
