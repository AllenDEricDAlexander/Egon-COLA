package top.egon.cola.component.rpc.test.mockgateway;

import top.egon.cola.component.rpc.provider.RpcServiceIdentity;
import top.egon.cola.component.rpc.test.support.TestRpcServiceSnapshot;
import top.egon.cola.component.rpc.test.support.TestRpcRegistry;
import top.egon.cola.component.rpc.test.support.TestRpcSubscription;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class MockProviderDirectory implements AutoCloseable {

    private final TestRpcRegistry registryClient;

    private final String env;

    private final Consumer<Collection<MockProviderEndpoint>> endpointListener;

    private final Map<RpcServiceIdentity, MockProviderClusterSnapshot> clusters =
            new ConcurrentHashMap<>();

    private final Map<RpcServiceIdentity, TestRpcSubscription> subscriptions =
            new ConcurrentHashMap<>();

    private volatile TestRpcSubscription catalogSubscription;

    MockProviderDirectory(
            TestRpcRegistry registryClient,
            String env,
            Consumer<Collection<MockProviderEndpoint>> endpointListener) {
        this.registryClient = registryClient;
        this.env = env;
        this.endpointListener = endpointListener;
    }

    void start() {
        acceptCatalog(registryClient.getServiceIdentities(env));
        catalogSubscription = registryClient.subscribeServices(
                env,
                this::acceptCatalog
        );
    }

    MockProviderClusterSnapshot cluster(RpcServiceIdentity key) {
        MockProviderClusterSnapshot snapshot = clusters.get(key);
        if (snapshot == null) {
            return new MockProviderClusterSnapshot(key, 0, List.of());
        }
        List<MockProviderEndpoint> active = snapshot.endpoints().stream()
                .filter(endpoint -> endpoint.activeAt(Instant.now()))
                .toList();
        return new MockProviderClusterSnapshot(
                key,
                snapshot.revision(),
                active
        );
    }

    List<MockProviderClusterSnapshot> clusters() {
        return clusters.keySet().stream()
                .sorted((left, right) -> left.registrySuffix().compareTo(
                        right.registrySuffix()
                ))
                .map(this::cluster)
                .toList();
    }

    @Override
    public void close() {
        if (catalogSubscription != null) {
            catalogSubscription.close();
            catalogSubscription = null;
        }
        subscriptions.values().forEach(TestRpcSubscription::close);
        subscriptions.clear();
        clusters.clear();
        endpointListener.accept(List.of());
    }

    private synchronized void acceptCatalog(
            List<RpcServiceIdentity> catalog) {
        List<RpcServiceIdentity> keys = catalog == null
                ? List.of()
                : catalog;
        subscriptions.keySet().stream()
                .filter(key -> !keys.contains(key))
                .toList()
                .forEach(key -> {
                    subscriptions.remove(key).close();
                    clusters.remove(key);
                });
        for (RpcServiceIdentity key : keys) {
            if (!subscriptions.containsKey(key)) {
                acceptSnapshot(registryClient.getInstances(key));
                subscriptions.put(
                        key,
                        registryClient.subscribeService(
                                key,
                                this::acceptSnapshot
                        )
                );
            }
        }
        publishEndpoints();
    }

    private void acceptSnapshot(TestRpcServiceSnapshot snapshot) {
        List<MockProviderEndpoint> endpoints = snapshot.instances().stream()
                .map(MockProviderEndpoint::from)
                .toList();
        clusters.put(
                snapshot.serviceIdentity(),
                new MockProviderClusterSnapshot(
                        snapshot.serviceIdentity(),
                        snapshot.revision(),
                        endpoints
                )
        );
        publishEndpoints();
    }

    private void publishEndpoints() {
        Map<String, MockProviderEndpoint> unique = new LinkedHashMap<>();
        clusters().stream()
                .flatMap(cluster -> cluster.endpoints().stream())
                .forEach(endpoint -> unique.put(
                        endpoint.channelKey(),
                        endpoint
                ));
        endpointListener.accept(new ArrayList<>(unique.values()));
    }
}
