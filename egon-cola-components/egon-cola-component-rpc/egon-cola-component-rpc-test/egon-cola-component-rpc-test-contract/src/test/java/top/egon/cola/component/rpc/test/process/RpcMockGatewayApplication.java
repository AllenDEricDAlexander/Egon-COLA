package top.egon.cola.component.rpc.test.process;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.listener.registry.DdcRegistrySubscriptionCoordinator;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.rpc.consumer.RpcGatewayEndpoint;
import top.egon.cola.component.rpc.consumer.RpcGatewayQuery;
import top.egon.cola.component.rpc.consumer.RpcGatewaySnapshot;
import top.egon.cola.component.rpc.consumer.RpcGatewaySubscription;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;
import top.egon.cola.component.rpc.ddc.client.registry.RpcDdcServiceRegistryClient;
import top.egon.cola.component.rpc.ddc.registry.RpcDdcRegistrySnapshotLoader;
import top.egon.cola.component.rpc.provider.RpcLeaseOperationResult;
import top.egon.cola.component.rpc.provider.RpcProviderLease;
import top.egon.cola.component.rpc.provider.RpcProviderLeaseIdentity;
import top.egon.cola.component.rpc.provider.RpcProviderRegistration;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;
import top.egon.cola.component.rpc.test.contract.proto.EchoServiceGrpc;
import top.egon.cola.component.rpc.test.mockgateway.MockGatewayProperties;
import top.egon.cola.component.rpc.test.mockgateway.MockRpcGateway;
import top.egon.cola.component.rpc.test.support.TestRpcRegistry;
import top.egon.cola.component.rpc.test.support.TestRpcServiceInstance;
import top.egon.cola.component.rpc.test.support.TestRpcServiceSnapshot;
import top.egon.cola.component.rpc.test.support.TestRpcSubscription;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class RpcMockGatewayApplication {

    private RpcMockGatewayApplication() {
    }

    public static void main(String[] arguments) throws Exception {
        Map<String, String> values = parse(arguments);
        DdcProperties properties = properties(values);
        RegistryResources registry = registry(values, properties);
        MockRpcGateway gateway = new MockRpcGateway(
                new ProcessDdcRpcRegistry(registry.client(), properties.getEnv()),
                properties.getEnv(),
                "mock-gateway:" + ProcessHandle.current().pid(),
                new MockGatewayProperties(
                        "egon-internal-rpc-gateway",
                        "default",
                        "1.0.0",
                        "127.0.0.1",
                        Integer.parseInt(required(values, "gateway.port")),
                        "127.0.0.1",
                        15,
                        3
                ),
                List.of(
                        EchoServiceGrpc.getEchoMethod().getFullMethodName()
                )
        );
        CountDownLatch shutdown = new CountDownLatch(1);
        AtomicBoolean closed = new AtomicBoolean();
        Runnable cleanup = () -> {
            if (closed.compareAndSet(false, true)) {
                gateway.close();
                registry.close();
            }
            shutdown.countDown();
        };
        Runtime.getRuntime().addShutdownHook(new Thread(
                cleanup,
                "rpc-mock-gateway-shutdown"
        ));
        try {
            gateway.start();
            System.out.printf(
                    "RPC_MOCK_GATEWAY_READY port=%d%n",
                    gateway.port()
            );
            shutdown.await();
        } finally {
            cleanup.run();
        }
    }

    private static DdcProperties properties(Map<String, String> values) {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("test-biz");
        properties.setAppCode("test-app");
        properties.getRedis().setHost(
                required(values, "ddc.redis.host")
        );
        properties.getRedis().setPort(Integer.parseInt(
                required(values, "ddc.redis.port")
        ));
        properties.getRedis().setPassword(
                values.get("ddc.redis.password")
        );
        properties.setEnv(required(values, "ddc.env"));
        properties.setNamespace(required(values, "ddc.namespace"));
        properties.getRegistry().setReconcileIntervalSeconds(1);
        return properties;
    }

    private static RegistryResources registry(
            Map<String, String> values,
            DdcProperties properties) {
        RedissonClient redisson = redisson(properties);
        DdcRpcProperties rpc = new DdcRpcProperties();
        rpc.setTarget(required(values, "ddc.target"));
        rpc.getTls().setDevelopmentPlaintext(true);
        rpc.getAuth().getRegistry().setAccessKey(
                required(values, "ddc.access-key")
        );
        rpc.getAuth().getRegistry().setSecretKey(
                required(values, "ddc.secret-key")
        );
        DdcRpcClientHandle<DdcServiceRegistryClient> handle =
                new DdcRpcClientFactory(
                        rpc,
                        properties,
                        new RpcProcessIdentity(
                                "rpc-mock-gateway",
                                properties.getEnv(),
                                "127.0.0.1",
                                ProcessHandle.current().pid(),
                                "mock-gateway:" + ProcessHandle.current().pid()
                        )
                ).registryClient();
        RpcDdcServiceRegistryClient client =
                (RpcDdcServiceRegistryClient) handle.client();
        DdcRegistrySubscriptionCoordinator subscriptions =
                new DdcRegistrySubscriptionCoordinator(
                        new RpcDdcRegistrySnapshotLoader(client),
                        redisson,
                        properties.getRegistry().getReconcileIntervalSeconds()
                );
        client.subscriptions(new RpcDdcServiceRegistryClient.RegistrySubscriptions() {
            @Override
            public DdcRegistrySubscription subscribe(
                    DdcServiceKey key,
                    Consumer<DdcServiceSnapshot> listener) {
                return subscriptions.subscribe(key, listener);
            }

            @Override
            public DdcRegistrySubscription subscribeServices(
                    DdcServiceQuery query,
                    Consumer<DdcServiceCatalogSnapshot> listener) {
                return subscriptions.subscribeServices(query, listener);
            }
        });
        return new RegistryResources(client, handle, subscriptions, redisson);
    }

    private static RedissonClient redisson(DdcProperties properties) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(
                        "redis://"
                                + properties.getRedis().getHost()
                                + ":"
                                + properties.getRedis().getPort()
                );
        String password = properties.getRedis().getPassword();
        if (password != null && !password.isBlank()) {
            config.useSingleServer().setPassword(password);
        }
        return Redisson.create(config);
    }

    private record RegistryResources(
            DdcServiceRegistryClient client,
            DdcRpcClientHandle<DdcServiceRegistryClient> handle,
            DdcRegistrySubscriptionCoordinator subscriptions,
            RedissonClient redisson
    ) implements AutoCloseable {

        @Override
        public void close() {
            subscriptions.close();
            handle.close();
            redisson.shutdown();
        }
    }

    private static final class ProcessDdcRpcRegistry
            implements TestRpcRegistry {

        private final DdcServiceRegistryClient delegate;

        private final String env;

        private final ConcurrentMap<String, DdcServiceKey> activeServices =
                new ConcurrentHashMap<>();

        private ProcessDdcRpcRegistry(
                DdcServiceRegistryClient delegate,
                String env) {
            this.delegate = delegate;
            this.env = env;
        }

        @Override
        public RpcProviderLease register(
                RpcProviderRegistration registration) {
            return register(registration, DdcServiceKind.RPC_PROVIDER);
        }

        @Override
        public RpcProviderLease registerGateway(
                RpcProviderRegistration registration) {
            return register(registration, DdcServiceKind.INTERNAL_GATEWAY);
        }

        private RpcProviderLease register(
                RpcProviderRegistration registration,
                DdcServiceKind kind) {
            DdcServiceKey serviceKey = key(
                    registration.serviceIdentity(),
                    kind
            );
            var session = delegate.register(new DdcServiceRegistration(
                    registration.processIdentity().instanceId(),
                    serviceKey,
                    registration.host(),
                    registration.port(),
                    registration.secure(),
                    registration.metadata(),
                    registration.leaseSeconds(),
                    registration.heartbeatIntervalSeconds(),
                    "test-admission-ticket"
            ));
            activeServices.put(session.leaseId(), serviceKey);
            return new RpcProviderLease(
                    session.instanceId(),
                    session.leaseId(),
                    session.registeredAt(),
                    session.leaseExpireAt()
            );
        }

        @Override
        public RpcLeaseOperationResult heartbeat(
                RpcProviderLeaseIdentity lease) {
            DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
            request.setServiceKey(activeServices.get(lease.leaseId()));
            request.setInstanceId(lease.instanceId());
            request.setLeaseId(lease.leaseId());
            request.setAdmissionTicket("test-admission-ticket");
            return result(delegate.heartbeat(request));
        }

        @Override
        public RpcLeaseOperationResult deregister(
                RpcProviderLeaseIdentity lease) {
            RpcLeaseOperationResult result = result(delegate.deregister(
                    lease.instanceId(),
                    lease.leaseId()
            ));
            activeServices.remove(lease.leaseId());
            return result;
        }

        @Override
        public RpcGatewaySubscription subscribe(
                RpcGatewayQuery query,
                Consumer<RpcGatewaySnapshot> listener) {
            DdcServiceKey key = key(
                    new RpcServiceIdentity(
                            query.serviceName(),
                            query.group(),
                            query.version()
                    ),
                    DdcServiceKind.INTERNAL_GATEWAY
            );
            var subscription = delegate.subscribe(
                    key,
                    snapshot -> listener.accept(new RpcGatewaySnapshot(
                            snapshot.revision(),
                            snapshot.observedAt(),
                            snapshot.instances().stream()
                                    .map(this::gatewayEndpoint)
                                    .toList()
                    ))
            );
            return subscription::close;
        }

        @Override
        public TestRpcServiceSnapshot getInstances(
                RpcServiceIdentity identity) {
            return snapshot(delegate.getInstances(key(
                    identity,
                    DdcServiceKind.RPC_PROVIDER
            )));
        }

        @Override
        public List<RpcServiceIdentity> getServiceIdentities(String env) {
            return delegate.getServiceKeys(query(env)).serviceKeys().stream()
                    .map(this::identity)
                    .toList();
        }

        @Override
        public TestRpcSubscription subscribeService(
                RpcServiceIdentity identity,
                Consumer<TestRpcServiceSnapshot> listener) {
            var subscription = delegate.subscribe(
                    key(identity, DdcServiceKind.RPC_PROVIDER),
                    value -> listener.accept(snapshot(value))
            );
            return subscription::close;
        }

        @Override
        public TestRpcSubscription subscribeServices(
                String env,
                Consumer<List<RpcServiceIdentity>> listener) {
            var subscription = delegate.subscribeServices(
                    query(env),
                    value -> listener.accept(value.serviceKeys().stream()
                            .map(this::identity)
                            .toList())
            );
            return subscription::close;
        }

        private DdcServiceQuery query(String targetEnv) {
            return new DdcServiceQuery(
                    "test-biz",
                    targetEnv,
                    "test-app",
                    DdcServiceKind.RPC_PROVIDER,
                    "grpc",
                    null,
                    null,
                    null
            );
        }

        private DdcServiceKey key(
                RpcServiceIdentity identity,
                DdcServiceKind kind) {
            return new DdcServiceKey(
                    "test-biz",
                    env,
                    "test-app",
                    kind,
                    identity.serviceName(),
                    identity.group(),
                    identity.version(),
                    "grpc"
            );
        }

        private RpcServiceIdentity identity(DdcServiceKey key) {
            return new RpcServiceIdentity(
                    key.serviceName(),
                    key.group(),
                    key.version()
            );
        }

        private TestRpcServiceSnapshot snapshot(DdcServiceSnapshot value) {
            return new TestRpcServiceSnapshot(
                    identity(value.serviceKey()),
                    value.revision(),
                    value.instances().stream()
                            .map(this::serviceInstance)
                            .toList(),
                    value.observedAt()
            );
        }

        private TestRpcServiceInstance serviceInstance(
                DdcServiceInstance value) {
            return new TestRpcServiceInstance(
                    identity(value.serviceKey()),
                    value.instanceId(),
                    value.leaseId(),
                    value.host(),
                    value.port(),
                    value.secure(),
                    value.leaseExpireAt(),
                    value.revision()
            );
        }

        private RpcGatewayEndpoint gatewayEndpoint(
                DdcServiceInstance value) {
            return new RpcGatewayEndpoint(
                    value.instanceId(),
                    value.leaseId(),
                    value.host(),
                    value.port(),
                    value.secure(),
                    value.leaseExpireAt()
            );
        }

        private RpcLeaseOperationResult result(
                top.egon.cola.component.ddc.model.lease
                        .DdcLeaseOperationResult value) {
            return new RpcLeaseOperationResult(
                    RpcLeaseOperationResult.Status.valueOf(
                            value.status().name()
                    ),
                    value.leaseExpireAt()
            );
        }
    }

    private static Map<String, String> parse(String[] arguments) {
        Map<String, String> values = new LinkedHashMap<>();
        Arrays.stream(arguments).forEach(argument -> {
            if (!argument.startsWith("--") || !argument.contains("=")) {
                throw new IllegalArgumentException(
                        "invalid process argument"
                );
            }
            int separator = argument.indexOf('=');
            values.put(
                    argument.substring(2, separator),
                    argument.substring(separator + 1)
            );
        });
        return values;
    }

    private static String required(
            Map<String, String> values,
            String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "missing process argument " + name
            );
        }
        return value;
    }
}
