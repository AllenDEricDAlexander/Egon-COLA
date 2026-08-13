package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.gateway.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.gateway.GatewayRpcInvocationChannelProvider;
import top.egon.cola.component.rpc.consumer.proxy.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLease;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLeaseIdentity;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistration;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;
import top.egon.cola.component.rpc.test.contract.EchoRpc;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.rpc.test.support.InMemoryRpcRegistryBackend;
import top.egon.cola.component.rpc.test.support.InMemoryRpcRegistryClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RpcMultiProviderDirectoryTest {

    @Test
    void shouldConvergeEvictAndAcceptReplacementLease() throws Exception {
        Server providerA = provider("provider-a");
        Server providerB = provider("provider-b");
        InMemoryRpcRegistryBackend backend =
                new InMemoryRpcRegistryBackend();
        InMemoryRpcRegistryClient providerRegistryA =
                new InMemoryRpcRegistryClient(backend);
        InMemoryRpcRegistryClient providerRegistryB =
                new InMemoryRpcRegistryClient(backend);
        RpcProviderRegistration registrationA =
                registration("provider-a", providerA.getPort());
        RpcProviderRegistration registrationB =
                registration("provider-b", providerB.getPort());
        RpcProviderLease leaseA = providerRegistryA.register(registrationA);
        RpcProviderLease leaseB = providerRegistryB.register(registrationB);
        MockRpcGateway gateway = new MockRpcGateway(
                new InMemoryRpcRegistryClient(backend),
                "test",
                "mock-gateway-multi",
                MockGatewayProperties.defaults(),
                List.of(EchoServiceGrpc.getEchoMethod().getFullMethodName())
        );
        RpcConsumerGatewayManager consumerGateway = null;
        try {
            gateway.start();
            RpcProcessIdentity identity = new RpcProcessIdentity(
                    "consumer",
                    "test",
                    "default",
                    "127.0.0.1",
                    1,
                    "consumer-multi"
            );
            EgonRpcProperties properties = new EgonRpcProperties();
            properties.getConsumer().setGatewayDiscoveryTimeoutMs(2000);
            consumerGateway = new RpcConsumerGatewayManager(
                    new InMemoryRpcRegistryClient(backend),
                    new RpcConsumerChannelFactory(),
                    properties,
                    identity
            );
            consumerGateway.start();
            EchoRpc consumer = new RpcConsumerProxyFactory(
                    new RpcContractValidator(),
                    new GatewayRpcInvocationChannelProvider(consumerGateway),
                    identity,
                    new RpcStatusExceptionMapper(),
                    3000
            ).create(EchoRpc.class, 3000);

            assertThat(call(consumer)).isEqualTo("provider-a");
            assertThat(call(consumer)).isEqualTo("provider-b");
            assertThat(gateway.channelFactory().size()).isEqualTo(2);

            providerRegistryA.deregister(leaseIdentity(leaseA));

            assertThat(gateway.directory().clusters().getFirst().endpoints())
                    .extracting(MockProviderEndpoint::instanceId)
                    .containsExactly("provider-b");
            assertThat(gateway.channelFactory().size()).isOne();
            assertThat(call(consumer)).isEqualTo("provider-b");
            assertThat(call(consumer)).isEqualTo("provider-b");

            RpcProviderLease replacement =
                    providerRegistryA.register(registrationA);

            assertThat(replacement.leaseId()).isNotEqualTo(leaseA.leaseId());
            assertThat(call(consumer)).isEqualTo("provider-a");
            assertThat(gateway.invocations().getLast().providerLeaseId())
                    .isEqualTo(replacement.leaseId());

            backend.advance(Duration.ofSeconds(6));

            assertThat(gateway.directory().clusters()).isEmpty();
            assertThat(gateway.channelFactory().size()).isZero();
        } finally {
            if (consumerGateway != null) {
                consumerGateway.stop();
            }
            gateway.close();
            providerRegistryA.deregister(new RpcProviderLeaseIdentity(
                    serviceIdentity(),
                    "provider-a",
                    backend.allInstances().stream()
                            .filter(instance -> "provider-a".equals(
                                    instance.instanceId()
                            ))
                            .map(instance -> instance.leaseId())
                            .findFirst()
                            .orElse("absent")
            ));
            providerRegistryB.deregister(leaseIdentity(leaseB));
            providerA.shutdownNow().awaitTermination();
            providerB.shutdownNow().awaitTermination();
        }
        assertThat(backend.allInstances()).isEmpty();
    }

    private String call(EchoRpc consumer) {
        return consumer.echo(EchoRequest.newBuilder()
                        .setMessage("hello")
                        .build())
                .getProviderId();
    }

    private Server provider(String providerId) throws Exception {
        EchoServiceGrpc.EchoServiceImplBase service =
                new EchoServiceGrpc.EchoServiceImplBase() {
                    @Override
                    public void echo(
                            EchoRequest request,
                            StreamObserver<EchoResponse> observer) {
                        observer.onNext(EchoResponse.newBuilder()
                                .setProviderId(providerId)
                                .setMessage(request.getMessage())
                                .build());
                        observer.onCompleted();
                    }
                };
        return NettyServerBuilder.forPort(0)
                .addService(service)
                .build()
                .start();
    }

    private RpcProviderRegistration registration(
            String instanceId,
            int port) {
        return new RpcProviderRegistration(
                serviceIdentity(),
                new RpcProcessIdentity(
                        "provider-test",
                        "test",
                        "127.0.0.1",
                        1,
                        instanceId
                ),
                "127.0.0.1",
                port,
                false,
                Map.of(
                        "egon.rpc.transport", "grpc",
                        "egon.rpc.serialization", "protobuf",
                        "egon.rpc.runtime-version", "test"
                ),
                5,
                1
        );
    }

    private RpcProviderLeaseIdentity leaseIdentity(RpcProviderLease lease) {
        return new RpcProviderLeaseIdentity(
                serviceIdentity(),
                lease.instanceId(),
                lease.leaseId()
        );
    }

    private RpcServiceIdentity serviceIdentity() {
        return new RpcServiceIdentity(
                "egon.rpc.test.v1.EchoService",
                "default",
                "1.0.0"
        );
    }
}
