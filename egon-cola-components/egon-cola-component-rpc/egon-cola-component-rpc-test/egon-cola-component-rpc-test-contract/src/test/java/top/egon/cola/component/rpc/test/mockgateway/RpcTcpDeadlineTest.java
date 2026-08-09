package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.Context;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.registry.model.DdcServiceKind;
import top.egon.cola.component.ddc.registry.model.DdcServiceKey;
import top.egon.cola.component.ddc.registry.model.DdcServiceRegistration;
import top.egon.cola.component.ddc.lease.DdcLeaseSession;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.test.contract.EchoRpc;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.rpc.test.support.InMemoryDdcRegistryBackend;
import top.egon.cola.component.rpc.test.support.InMemoryDdcServiceRegistryClient;
import top.egon.cola.component.rpc.test.support.TestDdcScopes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcTcpDeadlineTest {

    @Test
    void shouldPropagateDeadlineThroughGatewayToProvider() throws Exception {
        CountDownLatch providerEntered = new CountDownLatch(1);
        CountDownLatch providerCancelled = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        Server provider = blockingProvider(
                providerEntered,
                providerCancelled,
                releaseProvider
        );
        InMemoryDdcRegistryBackend backend =
                new InMemoryDdcRegistryBackend();
        InMemoryDdcServiceRegistryClient providerRegistry =
                new InMemoryDdcServiceRegistryClient(backend);
        DdcLeaseSession providerLease = providerRegistry.register(
                providerRegistration(provider.getPort())
        );
        MockRpcGateway gateway = new MockRpcGateway(
                new InMemoryDdcServiceRegistryClient(backend),
                "test",
                "mock-gateway-deadline",
                MockGatewayProperties.defaults(),
                List.of(EchoServiceGrpc.getEchoMethod().getFullMethodName())
        );
        RpcConsumerGatewayManager consumerGateway = null;
        try {
            gateway.start();
            EgonRpcProperties properties = new EgonRpcProperties();
            properties.getConsumer().setGatewayDiscoveryTimeoutMs(2000);
            RpcProcessIdentity identity = new RpcProcessIdentity(
                    "consumer",
                    "test",
                    "default",
                    "127.0.0.1",
                    1,
                    "consumer-deadline"
            );
            consumerGateway = new RpcConsumerGatewayManager(
                    new InMemoryDdcServiceRegistryClient(backend),
                    new RpcConsumerChannelFactory(),
                    properties,
                    identity,
                    TestDdcScopes.serviceKeyFactory()
            );
            consumerGateway.start();
            EchoRpc proxy = new RpcConsumerProxyFactory(
                    new RpcContractValidator(),
                    consumerGateway,
                    identity,
                    new RpcStatusExceptionMapper(),
                    3000
            ).create(EchoRpc.class, 100);

            assertThatThrownBy(() -> proxy.echo(
                    EchoRequest.newBuilder()
                            .setMessage("deadline")
                            .build()
            )).isInstanceOfSatisfying(EgonRpcException.class, exception ->
                    assertThat(exception.getCode()).isEqualTo(
                            EgonRpcErrorCode.RPC_DEADLINE_EXCEEDED
                    )
            );
            assertThat(providerEntered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(providerCancelled.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            releaseProvider.countDown();
            if (consumerGateway != null) {
                consumerGateway.stop();
            }
            gateway.close();
            providerRegistry.deregister(
                    providerLease.instanceId(),
                    providerLease.leaseId()
            );
            provider.shutdownNow().awaitTermination();
        }
    }

    private Server blockingProvider(
            CountDownLatch entered,
            CountDownLatch cancelled,
            CountDownLatch release) throws Exception {
        EchoServiceGrpc.EchoServiceImplBase service =
                new EchoServiceGrpc.EchoServiceImplBase() {
                    @Override
                    public void echo(
                            EchoRequest request,
                            StreamObserver<EchoResponse> observer) {
                        Context.current().addListener(
                                ignored -> cancelled.countDown(),
                                Runnable::run
                        );
                        entered.countDown();
                        try {
                            release.await(3, TimeUnit.SECONDS);
                            observer.onNext(EchoResponse.newBuilder()
                                    .setProviderId("deadline-provider")
                                    .setMessage(request.getMessage())
                                    .build());
                            observer.onCompleted();
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            observer.onError(exception);
                        }
                    }
                };
        return NettyServerBuilder.forPort(0)
                .addService(service)
                .build()
                .start();
    }

    private DdcServiceRegistration providerRegistration(int port) {
        return new DdcServiceRegistration(
                "deadline-provider",
                new DdcServiceKey(
                        "test-biz",
                        "test",
                        "test-app",
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
                30,
                10
        );
    }
}
