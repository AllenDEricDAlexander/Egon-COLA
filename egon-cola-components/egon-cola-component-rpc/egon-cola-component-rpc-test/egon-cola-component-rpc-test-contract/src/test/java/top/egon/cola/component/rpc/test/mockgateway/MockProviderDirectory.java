package top.egon.cola.component.rpc.test.mockgateway;

import top.egon.cola.component.ddc.registry.model.DdcServiceKind;
import top.egon.cola.component.ddc.registry.model.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.registry.model.DdcServiceKey;
import top.egon.cola.component.ddc.registry.model.DdcServiceQuery;
import top.egon.cola.component.ddc.registry.model.DdcServiceSnapshot;
import top.egon.cola.component.ddc.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class MockProviderDirectory implements AutoCloseable {

    private final DdcServiceRegistryClient registryClient;

    private final DdcServiceQuery query;

    private final Consumer<Collection<MockProviderEndpoint>> endpointListener;

    private final Map<DdcServiceKey, MockProviderClusterSnapshot> clusters =
            new ConcurrentHashMap<>();

    private final Map<DdcServiceKey, DdcRegistrySubscription> subscriptions =
            new ConcurrentHashMap<>();

    private volatile DdcRegistrySubscription catalogSubscription;

    MockProviderDirectory(
            DdcServiceRegistryClient registryClient,
            String env,
            Consumer<Collection<MockProviderEndpoint>> endpointListener) {
        this.registryClient = registryClient;
        this.query = new DdcServiceQuery(
                "test-biz",
                env,
                "test-app",
                DdcServiceKind.RPC_PROVIDER,
                "grpc",
                null,
                null,
                null
        );
        this.endpointListener = endpointListener;
    }

    void start() {
        acceptCatalog(registryClient.getServiceKeys(query));
        catalogSubscription = registryClient.subscribeServices(
                query,
                this::acceptCatalog
        );
    }

    MockProviderClusterSnapshot cluster(DdcServiceKey key) {
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
                .sorted()
                .map(this::cluster)
                .toList();
    }

    @Override
    public void close() {
        if (catalogSubscription != null) {
            catalogSubscription.close();
            catalogSubscription = null;
        }
        subscriptions.values().forEach(DdcRegistrySubscription::close);
        subscriptions.clear();
        clusters.clear();
        endpointListener.accept(List.of());
    }

    private synchronized void acceptCatalog(
            DdcServiceCatalogSnapshot catalog) {
        List<DdcServiceKey> keys = catalog == null
                ? List.of()
                : catalog.serviceKeys();
        subscriptions.keySet().stream()
                .filter(key -> !keys.contains(key))
                .toList()
                .forEach(key -> {
                    subscriptions.remove(key).close();
                    clusters.remove(key);
                });
        for (DdcServiceKey key : keys) {
            if (!subscriptions.containsKey(key)) {
                acceptSnapshot(registryClient.getInstances(key));
                subscriptions.put(
                        key,
                        registryClient.subscribe(key, this::acceptSnapshot)
                );
            }
        }
        publishEndpoints();
    }

    private void acceptSnapshot(DdcServiceSnapshot snapshot) {
        List<MockProviderEndpoint> endpoints = snapshot.instances().stream()
                .filter(instance -> instance.status() == null
                        || "UP".equalsIgnoreCase(instance.status()))
                .map(MockProviderEndpoint::from)
                .toList();
        clusters.put(
                snapshot.serviceKey(),
                new MockProviderClusterSnapshot(
                        snapshot.serviceKey(),
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
