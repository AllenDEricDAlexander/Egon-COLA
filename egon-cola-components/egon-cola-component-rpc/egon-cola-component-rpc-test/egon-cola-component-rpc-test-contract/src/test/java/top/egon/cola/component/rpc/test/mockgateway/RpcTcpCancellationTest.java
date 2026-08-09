package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.Context;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.consumer.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.consumer.RpcConsumerGatewayManager;
import top.egon.cola.component.rpc.consumer.GatewayRpcInvocationChannelProvider;
import top.egon.cola.component.rpc.consumer.RpcConsumerProxyFactory;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.provider.RpcProviderLease;
import top.egon.cola.component.rpc.provider.RpcProviderLeaseIdentity;
import top.egon.cola.component.rpc.provider.RpcProviderRegistration;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;
import top.egon.cola.component.rpc.test.contract.EchoRpc;
import top.egon.cola.component.rpc.test.contract.proto.EchoRequest;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.rpc.test.support.InMemoryRpcRegistryBackend;
import top.egon.cola.component.rpc.test.support.InMemoryRpcRegistryClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RpcTcpCancellationTest {

    @Test
    void shouldCancelForwardedProviderCall() throws Exception {
        CountDownLatch providerEntered = new CountDownLatch(1);
        CountDownLatch providerCancelled = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        Server provider = startBlockingProvider(
                providerEntered,
                providerCancelled,
                releaseProvider
        );
        InMemoryRpcRegistryBackend backend =
                new InMemoryRpcRegistryBackend();
        InMemoryRpcRegistryClient providerRegistry =
                new InMemoryRpcRegistryClient(backend);
        RpcProviderLease providerLease = providerRegistry.register(
                providerRegistration(provider.getPort())
        );
        MockRpcGateway gateway = new MockRpcGateway(
                new InMemoryRpcRegistryClient(backend),
                "test",
                "mock-gateway-cancel",
                MockGatewayProperties.defaults(),
                List.of(EchoServiceGrpc.getEchoMethod().getFullMethodName())
        );
        RpcConsumerGatewayManager consumerGateway = null;
        ExecutorService caller = Executors.newSingleThreadExecutor();
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
                    "consumer-cancel"
            );
            consumerGateway = new RpcConsumerGatewayManager(
                    new InMemoryRpcRegistryClient(backend),
                    new RpcConsumerChannelFactory(),
                    properties,
                    identity
            );
            consumerGateway.start();
            EchoRpc proxy = new RpcConsumerProxyFactory(
                    new RpcContractValidator(),
                    new GatewayRpcInvocationChannelProvider(consumerGateway),
                    identity,
                    new RpcStatusExceptionMapper(),
                    3000
            ).create(EchoRpc.class, 3000);
            Context.CancellableContext cancellation =
                    Context.current().withCancellation();
            java.util.concurrent.Future<EchoResponse> response =
                    caller.submit(() -> cancellation.call(() ->
                            proxy.echo(EchoRequest.newBuilder()
                                    .setMessage("block")
                                    .build())
                    ));
            assertThat(providerEntered.await(2, TimeUnit.SECONDS)).isTrue();

            cancellation.cancel(null);

            assertThat(providerCancelled.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(assertThatThrownCode(response)).isEqualTo(
                    EgonRpcErrorCode.RPC_CANCELLED
            );
        } finally {
            releaseProvider.countDown();
            caller.shutdownNow();
            if (consumerGateway != null) {
                consumerGateway.stop();
            }
            gateway.close();
            providerRegistry.deregister(leaseIdentity(providerLease));
            provider.shutdownNow().awaitTermination();
        }
    }

    private Server startBlockingProvider(
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
                                    .setProviderId("blocking-provider")
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

    private RpcProviderRegistration providerRegistration(int port) {
        return new RpcProviderRegistration(
                serviceIdentity(),
                new RpcProcessIdentity(
                        "provider-test",
                        "test",
                        "127.0.0.1",
                        1,
                        "blocking-provider"
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

    private EgonRpcErrorCode assertThatThrownCode(
            java.util.concurrent.Future<EchoResponse> response)
            throws Exception {
        try {
            response.get(2, TimeUnit.SECONDS);
            throw new AssertionError("RPC cancellation did not fail the call");
        } catch (ExecutionException exception) {
            assertThat(exception.getCause())
                    .isInstanceOf(EgonRpcException.class);
            return ((EgonRpcException) exception.getCause()).getCode();
        }
    }
}
