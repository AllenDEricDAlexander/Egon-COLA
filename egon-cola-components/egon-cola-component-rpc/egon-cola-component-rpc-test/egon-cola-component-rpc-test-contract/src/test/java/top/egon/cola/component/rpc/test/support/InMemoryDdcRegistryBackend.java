package top.egon.cola.component.rpc.test.support;

import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;

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

public final class InMemoryDdcRegistryBackend {

    private final Map<String, ActiveRegistration> registrations =
            new LinkedHashMap<>();

    private final Map<DdcServiceKey, CopyOnWriteArrayList<
            Consumer<DdcServiceSnapshot>>> instanceListeners =
            new LinkedHashMap<>();

    private final CopyOnWriteArrayList<CatalogListener> catalogListeners =
            new CopyOnWriteArrayList<>();

    private final AtomicLong revision = new AtomicLong();

    private Instant now = Instant.now();

    synchronized DdcLeaseSession register(
            DdcServiceRegistration registration) {
        expire();
        ActiveRegistration existing =
                registrations.get(registration.instanceId());
        if (existing != null
                && !existing.registration().serviceKey()
                .equals(registration.serviceKey())) {
            throw new IllegalStateException(
                    "test registry instance ID conflicts with another service"
            );
        }
        Instant registeredAt = now;
        String leaseId = UUID.randomUUID().toString();
        DdcLeaseSession session = new DdcLeaseSession(
                registration.instanceId(),
                leaseId,
                registration.serviceKey().serviceKind().leaseRole(),
                registration.leaseSeconds(),
                registration.heartbeatIntervalSeconds(),
                registeredAt,
                registeredAt.plusSeconds(registration.leaseSeconds())
        );
        registrations.put(
                registration.instanceId(),
                new ActiveRegistration(
                        registration,
                        session,
                        registeredAt,
                        revision.incrementAndGet()
                )
        );
        publishAll();
        return session;
    }

    synchronized DdcLeaseOperationResult heartbeat(
            String instanceId,
            String leaseId) {
        expire();
        ActiveRegistration active = registrations.get(instanceId);
        if (active == null) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.NOT_FOUND,
                    null
            );
        }
        if (!active.session().leaseId().equals(leaseId)) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.LEASE_MISMATCH,
                    active.session().leaseExpireAt()
            );
        }
        Instant expiry = now.plusSeconds(
                active.registration().leaseSeconds()
        );
        DdcLeaseSession renewed = new DdcLeaseSession(
                active.session().instanceId(),
                active.session().leaseId(),
                active.session().role(),
                active.session().leaseSeconds(),
                active.session().heartbeatIntervalSeconds(),
                active.session().registeredAt(),
                expiry
        );
        registrations.put(
                instanceId,
                new ActiveRegistration(
                        active.registration(),
                        renewed,
                        now,
                        revision.incrementAndGet()
                )
        );
        publish(active.registration().serviceKey());
        return new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.RENEWED,
                expiry
        );
    }

    synchronized DdcLeaseOperationResult deregister(
            String instanceId,
            String leaseId) {
        expire();
        ActiveRegistration active = registrations.get(instanceId);
        if (active == null) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.NOT_FOUND,
                    null
            );
        }
        if (!active.session().leaseId().equals(leaseId)) {
            return new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.LEASE_MISMATCH,
                    active.session().leaseExpireAt()
            );
        }
        registrations.remove(instanceId);
        revision.incrementAndGet();
        publishAll();
        return new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.DELETED,
                now
        );
    }

    synchronized DdcServiceSnapshot instances(DdcServiceKey serviceKey) {
        expire();
        return snapshot(serviceKey);
    }

    synchronized DdcServiceCatalogSnapshot catalog(DdcServiceQuery query) {
        expire();
        List<DdcServiceKey> keys = registrations.values().stream()
                .map(active -> active.registration().serviceKey())
                .distinct()
                .filter(query::matches)
                .sorted()
                .toList();
        return new DdcServiceCatalogSnapshot(
                query,
                revision.get(),
                keys,
                now
        );
    }

    synchronized DdcRegistrySubscription subscribe(
            DdcServiceKey key,
            Consumer<DdcServiceSnapshot> listener) {
        CopyOnWriteArrayList<Consumer<DdcServiceSnapshot>> listeners =
                instanceListeners.computeIfAbsent(
                        key,
                        ignored -> new CopyOnWriteArrayList<>()
                );
        listeners.add(listener);
        listener.accept(snapshot(key));
        return () -> listeners.remove(listener);
    }

    synchronized DdcRegistrySubscription subscribeServices(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener) {
        CatalogListener registered = new CatalogListener(query, listener);
        catalogListeners.add(registered);
        listener.accept(catalog(query));
        return () -> catalogListeners.remove(registered);
    }

    public synchronized void advance(Duration duration) {
        now = now.plus(duration);
        expire();
        publishAll();
    }

    public synchronized List<DdcServiceInstance> allInstances() {
        expire();
        return registrations.values().stream()
                .map(this::instance)
                .sorted()
                .toList();
    }

    private void expire() {
        boolean removed = registrations.entrySet().removeIf(
                entry -> !entry.getValue().session().leaseExpireAt()
                        .isAfter(now)
        );
        if (removed) {
            revision.incrementAndGet();
        }
    }

    private DdcServiceSnapshot snapshot(DdcServiceKey key) {
        List<DdcServiceInstance> instances = registrations.values().stream()
                .filter(active -> active.registration().serviceKey().equals(key))
                .map(this::instance)
                .sorted()
                .toList();
        return new DdcServiceSnapshot(
                key,
                revision.get(),
                instances,
                now
        );
    }

    private DdcServiceInstance instance(ActiveRegistration active) {
        DdcServiceRegistration registration = active.registration();
        DdcLeaseSession session = active.session();
        return new DdcServiceInstance(
                registration.instanceId(),
                session.leaseId(),
                registration.serviceKey(),
                registration.host(),
                registration.port(),
                registration.secure(),
                registration.metadata(),
                registration.leaseSeconds(),
                registration.heartbeatIntervalSeconds(),
                session.registeredAt(),
                active.lastHeartbeatAt(),
                session.leaseExpireAt(),
                "UP",
                active.revision()
        );
    }

    private void publishAll() {
        new ArrayList<>(instanceListeners.keySet()).forEach(this::publish);
        catalogListeners.forEach(listener ->
                listener.listener().accept(catalog(listener.query()))
        );
    }

    private void publish(DdcServiceKey key) {
        DdcServiceSnapshot snapshot = snapshot(key);
        instanceListeners.getOrDefault(
                key,
                new CopyOnWriteArrayList<>()
        ).forEach(listener -> listener.accept(snapshot));
    }

    private record ActiveRegistration(
            DdcServiceRegistration registration,
            DdcLeaseSession session,
            Instant lastHeartbeatAt,
            long revision
    ) {
    }

    private record CatalogListener(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener
    ) {
    }
}
