package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.provider.ProviderServiceRegistry;
import top.egon.cola.component.gateway.core.provider.ProviderServiceSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderSubscription;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class ProviderDirectory implements AutoCloseable {

    private final ProviderServiceRegistry registry;

    private final Clock clock;

    private final AtomicReference<Map<ProviderServiceKey, ProviderServiceSnapshot>>
            snapshot = new AtomicReference<>(Map.of());

    private final Map<ProviderServiceKey, SubscriptionRef> subscriptions =
            new LinkedHashMap<>();

    public ProviderDirectory(ProviderServiceRegistry registry, Clock clock) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized void activate(Set<ProviderServiceKey> serviceKeys) {
        Set<ProviderServiceKey> required = Set.copyOf(serviceKeys);
        Map<ProviderServiceKey, ProviderServiceSnapshot> prepared =
                new LinkedHashMap<>(snapshot.get());
        Map<ProviderServiceKey, ProviderSubscription> created =
                new LinkedHashMap<>();
        try {
            for (ProviderServiceKey key : required) {
                SubscriptionRef existing = subscriptions.get(key);
                if (existing != null) {
                    existing.references++;
                    continue;
                }
                ProviderServiceSnapshot initial = registry.getInstances(key);
                prepared.put(key, initial);
                ProviderSubscription subscription = registry.subscribe(
                        key,
                        this::onSnapshot
                );
                created.put(key, subscription);
            }
        } catch (RuntimeException failure) {
            created.values().forEach(ProviderSubscription::close);
            throw failure;
        }
        created.forEach((key, subscription) -> subscriptions.put(
                key,
                new SubscriptionRef(subscription, 1)
        ));
        snapshot.set(Map.copyOf(prepared));
    }

    public synchronized void release(Set<ProviderServiceKey> serviceKeys) {
        Map<ProviderServiceKey, ProviderServiceSnapshot> updated =
                new LinkedHashMap<>(snapshot.get());
        for (ProviderServiceKey key : Set.copyOf(serviceKeys)) {
            SubscriptionRef reference = subscriptions.get(key);
            if (reference == null) {
                continue;
            }
            reference.references--;
            if (reference.references == 0) {
                reference.subscription.close();
                subscriptions.remove(key);
                updated.remove(key);
            }
        }
        snapshot.set(Map.copyOf(updated));
    }

    public List<ProviderInstance> available(ProviderServiceKey key) {
        Instant now = clock.instant();
        return instances(key).stream()
                .filter(instance -> instance.availableAt(now))
                .toList();
    }

    public List<ProviderInstance> instances(ProviderServiceKey key) {
        ProviderServiceSnapshot service = snapshot.get().get(key);
        if (service == null) {
            return List.of();
        }
        return service.instances();
    }

    public Map<ProviderServiceKey, ProviderServiceSnapshot> snapshot() {
        return snapshot.get();
    }

    public boolean allAvailable(Set<ProviderServiceKey> serviceKeys) {
        return Set.copyOf(serviceKeys).stream()
                .allMatch(key -> !available(key).isEmpty());
    }

    public synchronized int referenceCount(ProviderServiceKey key) {
        SubscriptionRef reference = subscriptions.get(key);
        return reference == null ? 0 : reference.references;
    }

    @Override
    public synchronized void close() {
        subscriptions.values().forEach(
                reference -> reference.subscription.close()
        );
        subscriptions.clear();
        snapshot.set(Map.of());
    }

    private void onSnapshot(ProviderServiceSnapshot updated) {
        snapshot.updateAndGet(current -> {
            if (!subscriptions.containsKey(updated.serviceKey())) {
                return current;
            }
            ProviderServiceSnapshot existing = current.get(
                    updated.serviceKey()
            );
            if (existing != null && updated.revision() < existing.revision()) {
                return current;
            }
            Map<ProviderServiceKey, ProviderServiceSnapshot> copy =
                    new LinkedHashMap<>(current);
            copy.put(updated.serviceKey(), updated);
            return Map.copyOf(copy);
        });
    }

    private static final class SubscriptionRef {

        private final ProviderSubscription subscription;

        private int references;

        private SubscriptionRef(
                ProviderSubscription subscription,
                int references) {
            this.subscription = subscription;
            this.references = references;
        }
    }
}
