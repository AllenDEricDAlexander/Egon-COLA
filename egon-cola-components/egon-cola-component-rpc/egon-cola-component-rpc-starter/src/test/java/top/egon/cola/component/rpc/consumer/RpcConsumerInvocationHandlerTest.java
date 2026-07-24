package top.egon.cola.component.rpc.consumer;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcMetadataKeys;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.RpcContractValidator;
import top.egon.cola.component.rpc.exception.RpcStatusExceptionMapper;
import top.egon.cola.component.rpc.support.TestGrpcDescriptorFixtures.UnaryFixtureGrpc;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class RpcConsumerInvocationHandlerTest {

    @Test
    void shouldCallGatewayWithGeneratedDescriptorAndFrameworkMetadata()
            throws Exception {
        AtomicReference<String> observedService = new AtomicReference<>();
        Server server = startGateway(observedService);
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
                    identity
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

            assertThat(response.getValue()).isEqualTo("gateway:hello");
            assertThat(observedService.get())
                    .isEqualTo("egon.rpc.fixture.v1.UnaryFixtureService");
            assertThat(proxy.toString()).contains(EchoConsumer.class.getName());
        } finally {
            if (manager != null) {
                manager.stop();
            }
            server.shutdownNow().awaitTermination();
        }
    }

    private Server startGateway(AtomicReference<String> observedService)
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

    private static final class GatewayRegistry
            implements DdcServiceRegistryClient {

        private final int port;

        private GatewayRegistry(int port) {
            this.port = port;
        }

        @Override
        public DdcRegistrySubscription subscribe(
                DdcServiceKey serviceKey,
                Consumer<DdcServiceSnapshot> listener) {
            Instant now = Instant.now();
            listener.accept(new DdcServiceSnapshot(
                    serviceKey,
                    1,
                    List.of(new DdcServiceInstance(
                            "gateway-1",
                            "lease-1",
                            serviceKey,
                            "127.0.0.1",
                            port,
                            false,
                            java.util.Map.of(),
                            30,
                            10,
                            now,
                            now,
                            now.plusSeconds(30),
                            "UP",
                            1
                    )),
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
}
