package top.egon.cola.component.rpc.test.support;

import top.egon.cola.component.rpc.consumer.RpcGatewayEndpoint;
import top.egon.cola.component.rpc.consumer.RpcGatewayQuery;
import top.egon.cola.component.rpc.consumer.RpcGatewaySnapshot;
import top.egon.cola.component.rpc.provider.RpcLeaseOperationResult;
import top.egon.cola.component.rpc.provider.RpcProviderLease;
import top.egon.cola.component.rpc.provider.RpcProviderLeaseIdentity;
import top.egon.cola.component.rpc.provider.RpcProviderRegistration;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class InMemoryRpcRegistryBackend {

    private final Map<LeaseKey, ActiveRegistration> registrations =
            new LinkedHashMap<>();

    private final Map<RpcServiceIdentity, CopyOnWriteArrayList<
            Consumer<TestRpcServiceSnapshot>>> instanceListeners =
            new LinkedHashMap<>();

    private final CopyOnWriteArrayList<CatalogListener> catalogListeners =
            new CopyOnWriteArrayList<>();

    private final AtomicLong revision = new AtomicLong();

    private Instant now = Instant.now();

    synchronized RpcProviderLease register(
            RpcProviderRegistration registration) {
        return register(registration, false);
    }

    synchronized RpcProviderLease registerGateway(
            RpcProviderRegistration registration) {
        return register(registration, true);
    }

    private RpcProviderLease register(
            RpcProviderRegistration registration,
            boolean gateway) {
        expire();
        LeaseKey key = new LeaseKey(
                registration.serviceIdentity(),
                registration.processIdentity().instanceId()
        );
        Instant registeredAt = now;
        RpcProviderLease lease = new RpcProviderLease(
                registration.processIdentity().instanceId(),
                UUID.randomUUID().toString(),
                registeredAt,
                registeredAt.plusSeconds(registration.leaseSeconds())
        );
        registrations.put(
                key,
                new ActiveRegistration(
                        registration,
                        lease,
                        gateway,
                        revision.incrementAndGet()
                )
        );
        publishAll();
        return lease;
    }

    synchronized RpcLeaseOperationResult heartbeat(
            RpcProviderLeaseIdentity identity) {
        expire();
        LeaseKey key = LeaseKey.from(identity);
        ActiveRegistration active = registrations.get(key);
        if (active == null) {
            return RpcLeaseOperationResult.notFound();
        }
        if (!active.lease().leaseId().equals(identity.leaseId())) {
            return RpcLeaseOperationResult.leaseMismatch();
        }
        Instant expiry = now.plusSeconds(
                active.registration().leaseSeconds()
        );
        RpcProviderLease renewed = new RpcProviderLease(
                active.lease().instanceId(),
                active.lease().leaseId(),
                active.lease().registeredAt(),
                expiry
        );
        registrations.put(
                key,
                new ActiveRegistration(
                        active.registration(),
                        renewed,
                        active.gateway(),
                        revision.incrementAndGet()
                )
        );
        publish(active.registration().serviceIdentity());
        return RpcLeaseOperationResult.renewed(expiry);
    }

    synchronized RpcLeaseOperationResult deregister(
            RpcProviderLeaseIdentity identity) {
        expire();
        LeaseKey key = LeaseKey.from(identity);
        ActiveRegistration active = registrations.get(key);
        if (active == null) {
            return RpcLeaseOperationResult.notFound();
        }
        if (!active.lease().leaseId().equals(identity.leaseId())) {
            return RpcLeaseOperationResult.leaseMismatch();
        }
        registrations.remove(key);
        revision.incrementAndGet();
        publishAll();
        return RpcLeaseOperationResult.deleted();
    }

    synchronized TestRpcServiceSnapshot instances(
            RpcServiceIdentity serviceIdentity) {
        expire();
        return snapshot(serviceIdentity);
    }

    synchronized List<RpcServiceIdentity> serviceIdentities(String env) {
        expire();
        return registrations.values().stream()
                .filter(active -> !active.gateway())
                .filter(active -> active.registration()
                        .processIdentity().env().equals(env))
                .map(active -> active.registration().serviceIdentity())
                .distinct()
                .sorted((left, right) -> left.registrySuffix().compareTo(
                        right.registrySuffix()
                ))
                .toList();
    }

    synchronized TestRpcSubscription subscribe(
            RpcServiceIdentity identity,
            Consumer<TestRpcServiceSnapshot> listener) {
        CopyOnWriteArrayList<Consumer<TestRpcServiceSnapshot>> listeners =
                instanceListeners.computeIfAbsent(
                        identity,
                        ignored -> new CopyOnWriteArrayList<>()
                );
        listeners.add(listener);
        listener.accept(snapshot(identity));
        return () -> listeners.remove(listener);
    }

    synchronized TestRpcSubscription subscribeServices(
            String env,
            Consumer<List<RpcServiceIdentity>> listener) {
        CatalogListener registered = new CatalogListener(env, listener);
        catalogListeners.add(registered);
        listener.accept(serviceIdentities(env));
        return () -> catalogListeners.remove(registered);
    }

    synchronized RpcGatewaySnapshot gateways(RpcGatewayQuery query) {
        RpcServiceIdentity identity = new RpcServiceIdentity(
                query.serviceName(),
                query.group(),
                query.version()
        );
        List<RpcGatewayEndpoint> endpoints = registrations.values().stream()
                .filter(ActiveRegistration::gateway)
                .filter(active -> active.registration()
                        .serviceIdentity().equals(identity))
                .filter(active -> active.registration()
                        .processIdentity().env().equals(query.env()))
                .map(active -> new RpcGatewayEndpoint(
                        active.lease().instanceId(),
                        active.lease().leaseId(),
                        active.registration().host(),
                        active.registration().port(),
                        active.registration().secure(),
                        active.lease().leaseExpireAt()
                ))
                .sorted((left, right) -> left.instanceId().compareTo(
                        right.instanceId()
                ))
                .toList();
        return new RpcGatewaySnapshot(revision.get(), now, endpoints);
    }

    public synchronized void advance(Duration duration) {
        now = now.plus(duration);
        expire();
        publishAll();
    }

    public synchronized List<TestRpcServiceInstance> allInstances() {
        expire();
        return registrations.values().stream()
                .map(this::instance)
                .sorted()
                .toList();
    }

    private void expire() {
        boolean removed = registrations.entrySet().removeIf(
                entry -> !entry.getValue().lease().leaseExpireAt()
                        .isAfter(now)
        );
        if (removed) {
            revision.incrementAndGet();
        }
    }

    private TestRpcServiceSnapshot snapshot(RpcServiceIdentity identity) {
        List<TestRpcServiceInstance> instances = registrations.values().stream()
                .filter(active -> active.registration()
                        .serviceIdentity().equals(identity))
                .map(this::instance)
                .sorted()
                .toList();
        return new TestRpcServiceSnapshot(
                identity,
                revision.get(),
                instances,
                now
        );
    }

    private TestRpcServiceInstance instance(ActiveRegistration active) {
        return TestRpcServiceInstance.from(
                active.registration(),
                active.lease(),
                active.revision()
        );
    }

    private void publishAll() {
        new ArrayList<>(instanceListeners.keySet()).forEach(this::publish);
        catalogListeners.forEach(listener -> listener.listener().accept(
                serviceIdentities(listener.env())
        ));
    }

    private void publish(RpcServiceIdentity identity) {
        TestRpcServiceSnapshot snapshot = snapshot(identity);
        instanceListeners.getOrDefault(
                identity,
                new CopyOnWriteArrayList<>()
        ).forEach(listener -> listener.accept(snapshot));
    }

    private record LeaseKey(
            RpcServiceIdentity serviceIdentity,
            String instanceId
    ) {

        private static LeaseKey from(RpcProviderLeaseIdentity identity) {
            return new LeaseKey(
                    identity.serviceIdentity(),
                    identity.instanceId()
            );
        }
    }

    private record ActiveRegistration(
            RpcProviderRegistration registration,
            RpcProviderLease lease,
            boolean gateway,
            long revision
    ) {
    }

    private record CatalogListener(
            String env,
            Consumer<List<RpcServiceIdentity>> listener
    ) {
    }
}
