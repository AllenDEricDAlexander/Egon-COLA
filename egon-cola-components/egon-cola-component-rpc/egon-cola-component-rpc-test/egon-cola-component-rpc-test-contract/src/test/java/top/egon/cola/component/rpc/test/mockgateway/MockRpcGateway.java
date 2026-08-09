package top.egon.cola.component.rpc.test.mockgateway;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.rpc.context.RpcMetadataKeys;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.provider.RpcProviderLease;
import top.egon.cola.component.rpc.provider.RpcProviderLeaseIdentity;
import top.egon.cola.component.rpc.provider.RpcProviderRegistration;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;
import top.egon.cola.component.rpc.test.support.TestRpcRegistry;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MockRpcGateway implements AutoCloseable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MockRpcGateway.class);

    private final TestRpcRegistry registryClient;

    private final String env;

    private final String instanceId;

    private final MockGatewayProperties properties;

    private final MockProviderChannelFactory channelFactory =
            new MockProviderChannelFactory();

    private final MockProviderDirectory directory;

    private final MockDynamicHandlerRegistry handlers =
            new MockDynamicHandlerRegistry();

    private final MockRoundRobinSelector selector =
            new MockRoundRobinSelector();

    private final MockUnaryForwarder forwarder =
            new MockUnaryForwarder(channelFactory);

    private final List<MockGatewayInvocation> invocations =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private Server server;

    private RpcProviderLease lease;

    private ScheduledExecutorService heartbeat;

    public MockRpcGateway(
            TestRpcRegistry registryClient,
            String env,
            String instanceId,
            MockGatewayProperties properties,
            List<String> fullMethodNames) {
        this.registryClient = registryClient;
        this.env = env;
        this.instanceId = instanceId;
        this.properties = properties;
        this.directory = new MockProviderDirectory(
                registryClient,
                env,
                channelFactory::retain
        );
        fullMethodNames.forEach(method -> handlers.register(
                method,
                (payload, observer) -> dispatch(method, payload, observer)
        ));
    }

    public void start() throws Exception {
        directory.start();
        server = NettyServerBuilder.forAddress(new InetSocketAddress(
                        properties.bindHost(),
                        properties.bindPort()
                ))
                .fallbackHandlerRegistry(handlers)
                .intercept(metadataInterceptor())
                .build()
                .start();
        lease = registryClient.registerGateway(new RpcProviderRegistration(
                gatewayServiceIdentity(),
                new RpcProcessIdentity(
                        properties.serviceName(),
                        env,
                        properties.advertisedHost(),
                        ProcessHandle.current().pid(),
                        instanceId
                ),
                properties.advertisedHost(),
                server.getPort(),
                false,
                Map.of(
                        "egon.rpc.transport", "grpc",
                        "egon.rpc.serialization", "protobuf",
                        "egon.rpc.runtime-version", "test"
                ),
                properties.leaseSeconds(),
                properties.heartbeatIntervalSeconds()
        ));
        heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "mock-rpc-gateway-heartbeat"
            );
            thread.setDaemon(true);
            return thread;
        });
        heartbeat.scheduleWithFixedDelay(
                this::heartbeat,
                properties.heartbeatIntervalSeconds(),
                properties.heartbeatIntervalSeconds(),
                TimeUnit.SECONDS
        );
    }

    public int port() {
        return server == null ? -1 : server.getPort();
    }

    List<MockGatewayInvocation> invocations() {
        return List.copyOf(invocations);
    }

    MockProviderDirectory directory() {
        return directory;
    }

    MockProviderChannelFactory channelFactory() {
        return channelFactory;
    }

    @Override
    public void close() {
        if (heartbeat != null) {
            heartbeat.shutdownNow();
            heartbeat = null;
        }
        if (lease != null) {
            try {
                registryClient.deregister(new RpcProviderLeaseIdentity(
                        gatewayServiceIdentity(),
                        lease.instanceId(),
                        lease.leaseId()
                ));
            } finally {
                lease = null;
            }
        }
        if (server != null) {
            server.shutdownNow();
            server = null;
        }
        directory.close();
        channelFactory.close();
    }

    private void dispatch(
            String fullMethodName,
            byte[] payload,
            io.grpc.stub.StreamObserver<byte[]> observer) {
        Metadata metadata = MockGatewayCallContext.METADATA.get();
        String serviceName = fullMethodName.substring(
                0,
                fullMethodName.lastIndexOf('/')
        );
        String group = value(
                metadata,
                RpcMetadataKeys.GROUP,
                "default"
        );
        String version = value(
                metadata,
                RpcMetadataKeys.VERSION,
                "1.0.0"
        );
        RpcServiceIdentity serviceKey = new RpcServiceIdentity(
                serviceName,
                group,
                version
        );
        MockProviderEndpoint endpoint = selector.select(
                directory.cluster(serviceKey).endpoints()
        );
        if (endpoint == null) {
            Metadata trailers = new Metadata();
            trailers.put(
                    Metadata.Key.of(
                            "x-egon-rpc-error-type",
                            Metadata.ASCII_STRING_MARSHALLER
                    ),
                    "service"
            );
            observer.onError(Status.NOT_FOUND
                    .withDescription("mock gateway provider not found")
                    .asRuntimeException(trailers));
            return;
        }
        String invocationId = value(
                metadata,
                RpcMetadataKeys.INVOCATION_ID,
                "missing"
        );
        invocations.add(new MockGatewayInvocation(
                invocationId,
                fullMethodName,
                endpoint.instanceId(),
                endpoint.leaseId()
        ));
        LOGGER.info(
                "RPC_MOCK_GATEWAY_FORWARD invocationId={} providerId={}",
                invocationId,
                endpoint.instanceId()
        );
        forwarder.forward(endpoint, fullMethodName, payload, observer);
    }

    private ServerInterceptor metadataInterceptor() {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                Context context = Context.current().withValue(
                        MockGatewayCallContext.METADATA,
                        headers
                );
                return Contexts.interceptCall(
                        context,
                        call,
                        headers,
                        next
                );
            }
        };
    }

    private RpcServiceIdentity gatewayServiceIdentity() {
        return new RpcServiceIdentity(
                properties.serviceName(),
                properties.group(),
                properties.version()
        );
    }

    private void heartbeat() {
        RpcProviderLease current = lease;
        if (current == null) {
            return;
        }
        try {
            registryClient.heartbeat(new RpcProviderLeaseIdentity(
                    gatewayServiceIdentity(),
                    current.instanceId(),
                    current.leaseId()
            ));
        } catch (RuntimeException exception) {
            LOGGER.warn("Mock Gateway heartbeat failed", exception);
        }
    }

    private String value(
            Metadata metadata,
            Metadata.Key<String> key,
            String fallback) {
        String value = metadata == null ? null : metadata.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
