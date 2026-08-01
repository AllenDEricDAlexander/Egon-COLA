package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.test.contract.EchoRpc;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.rpc.test.support.InMemoryDdcRegistryBackend;
import top.egon.cola.component.rpc.test.support.InMemoryDdcServiceRegistryClient;
import top.egon.cola.component.rpc.test.support.TestDdcScopes;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RpcMultiProviderDirectoryTest {

    @Test
    void shouldConvergeEvictAndAcceptReplacementLease() throws Exception {
        Server providerA = provider("provider-a");
        Server providerB = provider("provider-b");
        InMemoryDdcRegistryBackend backend =
                new InMemoryDdcRegistryBackend();
        InMemoryDdcServiceRegistryClient providerRegistryA =
                new InMemoryDdcServiceRegistryClient(backend);
        InMemoryDdcServiceRegistryClient providerRegistryB =
                new InMemoryDdcServiceRegistryClient(backend);
        DdcServiceRegistration registrationA =
                registration("provider-a", providerA.getPort());
        DdcServiceRegistration registrationB =
                registration("provider-b", providerB.getPort());
        DdcLeaseSession leaseA = providerRegistryA.register(registrationA);
        DdcLeaseSession leaseB = providerRegistryB.register(registrationB);
        MockRpcGateway gateway = new MockRpcGateway(
                new InMemoryDdcServiceRegistryClient(backend),
                "test",
                "default",
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
                    new InMemoryDdcServiceRegistryClient(backend),
                    new RpcConsumerChannelFactory(),
                    properties,
                    identity,
                    TestDdcScopes.serviceKeyFactory()
            );
            consumerGateway.start();
            EchoRpc consumer = new RpcConsumerProxyFactory(
                    new RpcContractValidator(),
                    consumerGateway,
                    identity,
                    new RpcStatusExceptionMapper(),
                    3000
            ).create(EchoRpc.class, 3000);

            assertThat(call(consumer)).isEqualTo("provider-a");
            assertThat(call(consumer)).isEqualTo("provider-b");
            assertThat(gateway.channelFactory().size()).isEqualTo(2);

            providerRegistryA.deregister(
                    leaseA.instanceId(),
                    leaseA.leaseId()
            );

            assertThat(gateway.directory().clusters().getFirst().endpoints())
                    .extracting(MockProviderEndpoint::instanceId)
                    .containsExactly("provider-b");
            assertThat(gateway.channelFactory().size()).isOne();
            assertThat(call(consumer)).isEqualTo("provider-b");
            assertThat(call(consumer)).isEqualTo("provider-b");

            DdcLeaseSession replacement =
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
            providerRegistryA.deregister(
                    "provider-a",
                    backend.allInstances().stream()
                            .filter(instance -> "provider-a".equals(
                                    instance.instanceId()
                            ))
                            .map(instance -> instance.leaseId())
                            .findFirst()
                            .orElse("absent")
            );
            providerRegistryB.deregister(
                    leaseB.instanceId(),
                    leaseB.leaseId()
            );
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

    private DdcServiceRegistration registration(
            String instanceId,
            int port) {
        return new DdcServiceRegistration(
                instanceId,
                new DdcServiceKey(
                        "test-biz",
                        "test-app",
                        "test",
                        "default",
                        DdcServiceKind.RPC_PROVIDER,
                        "egon.rpc.test.v1.EchoService",
                        "default",
                        "1.0.0",
                        "grpc"
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
}
