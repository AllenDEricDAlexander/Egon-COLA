package top.egon.cola.component.ddc.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.registry.DdcRegistryEvent;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class DdcRegistrySubscriptionManager implements AutoCloseable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DdcRegistrySubscriptionManager.class);

    private static final ObjectMapper EVENT_MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private final DdcServiceRegistryClient loader;

    private final RedissonClient redissonClient;

    private final Clock clock;

    private final ScheduledExecutorService scheduler;

    private final long reconcileIntervalMillis;

    private final Set<ManagedSubscription> subscriptions = ConcurrentHashMap.newKeySet();

    public DdcRegistrySubscriptionManager(DdcServiceRegistryClient loader,
                                          RedissonClient redissonClient,
                                          int reconcileIntervalSeconds) {
        this(
                loader,
                redissonClient,
                Clock.systemUTC(),
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "egon-cola-ddc-registry");
                    thread.setDaemon(true);
                    return thread;
                }),
                TimeUnit.SECONDS.toMillis(reconcileIntervalSeconds)
        );
    }

    DdcRegistrySubscriptionManager(DdcServiceRegistryClient loader,
                                   RedissonClient redissonClient,
                                   Clock clock,
                                   ScheduledExecutorService scheduler,
                                   long reconcileIntervalMillis) {
        if (reconcileIntervalMillis <= 0) {
            throw new IllegalArgumentException("reconcile interval must be positive");
        }
        this.loader = Objects.requireNonNull(loader, "loader");
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.reconcileIntervalMillis = reconcileIntervalMillis;
    }

    public DdcRegistrySubscription subscribe(
            DdcServiceKey serviceKey,
            Consumer<DdcServiceSnapshot> listener) {
        InstanceSubscription subscription =
                new InstanceSubscription(serviceKey, listener);
        subscriptions.add(subscription);
        try {
            subscription.start();
            return subscription;
        } catch (RuntimeException exception) {
            subscription.close();
            throw exception;
        }
    }

    public DdcRegistrySubscription subscribeServices(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener) {
        CatalogSubscription subscription =
                new CatalogSubscription(query, listener);
        subscriptions.add(subscription);
        try {
            subscription.start();
            return subscription;
        } catch (RuntimeException exception) {
            subscription.close();
            throw exception;
        }
    }

    @Override
    public void close() {
        List.copyOf(subscriptions).forEach(ManagedSubscription::close);
        scheduler.shutdownNow();
    }

    private abstract class ManagedSubscription implements DdcRegistrySubscription {

        private final AtomicBoolean closed = new AtomicBoolean();

        private final AtomicBoolean refreshQueued = new AtomicBoolean();

        private RTopic topic;

        private int listenerId = -1;

        private ScheduledFuture<?> reconciliation;

        protected void start(String topicName) {
            topic = redissonClient.getTopic(topicName, StringCodec.INSTANCE);
            listenerId = topic.addListener(String.class, this::onEvent);
            refresh();
            reconciliation = scheduler.scheduleWithFixedDelay(
                    this::safeRefresh,
                    reconcileIntervalMillis,
                    reconcileIntervalMillis,
                    TimeUnit.MILLISECONDS
            );
        }

        protected abstract boolean relevant(DdcRegistryEvent event);

        protected abstract void refresh();

        protected abstract void expireLocal();

        private void onEvent(CharSequence channel, String payload) {
            try {
                DdcRegistryEvent event =
                        EVENT_MAPPER.readValue(payload, DdcRegistryEvent.class);
                if (relevant(event)) {
                    queueRefresh();
                }
            } catch (JsonProcessingException exception) {
                LOGGER.warn("DDC registry event is invalid", exception);
            }
        }

        protected void queueRefresh() {
            if (!closed.get() && refreshQueued.compareAndSet(false, true)) {
                scheduler.execute(() -> {
                    try {
                        safeRefresh();
                    } finally {
                        refreshQueued.set(false);
                    }
                });
            }
        }

        protected void safeRefresh() {
            if (closed.get()) {
                return;
            }
            try {
                refresh();
            } catch (RuntimeException exception) {
                LOGGER.warn("DDC registry reconciliation failed", exception);
                expireLocal();
            }
        }

        protected <T> void notifySafely(Consumer<T> listener, T value) {
            try {
                listener.accept(value);
            } catch (RuntimeException exception) {
                LOGGER.warn("DDC registry listener failed", exception);
            }
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            if (reconciliation != null) {
                reconciliation.cancel(false);
            }
            if (topic != null && listenerId >= 0) {
                topic.removeListener(listenerId);
            }
            subscriptions.remove(this);
        }
    }

    private final class InstanceSubscription extends ManagedSubscription {

        private final DdcServiceKey serviceKey;

        private final Consumer<DdcServiceSnapshot> listener;

        private volatile DdcServiceSnapshot current;

        private InstanceSubscription(DdcServiceKey serviceKey,
                                     Consumer<DdcServiceSnapshot> listener) {
            this.serviceKey = serviceKey;
            this.listener = listener;
        }

        private void start() {
            start(DdcKeys.registryTopic(
                    serviceKey.env(),
                    serviceKey.namespace(),
                    serviceKey.serviceKind(),
                    serviceKey.protocol()
            ));
        }

        @Override
        protected boolean relevant(DdcRegistryEvent event) {
            return event != null && serviceKey.equals(event.serviceKey());
        }

        @Override
        protected synchronized void refresh() {
            publishIfChanged(loader.getInstances(serviceKey));
        }

        @Override
        protected synchronized void expireLocal() {
            DdcServiceSnapshot snapshot = current;
            if (snapshot == null) {
                return;
            }
            var live = snapshot.instances().stream()
                    .filter(instance -> instance.leaseExpireAt() != null
                            && instance.leaseExpireAt().isAfter(clock.instant()))
                    .toList();
            if (live.size() != snapshot.instances().size()) {
                publishIfChanged(new DdcServiceSnapshot(
                        serviceKey,
                        snapshot.revision(),
                        live,
                        clock.instant()
                ));
            }
        }

        private void publishIfChanged(DdcServiceSnapshot next) {
            DdcServiceSnapshot previous = current;
            if (previous != null
                    && previous.revision() == next.revision()
                    && previous.instances().equals(next.instances())) {
                return;
            }
            current = next;
            notifySafely(listener, next);
        }
    }

    private final class CatalogSubscription extends ManagedSubscription {

        private final DdcServiceQuery query;

        private final Consumer<DdcServiceCatalogSnapshot> listener;

        private volatile DdcServiceCatalogSnapshot current;

        private CatalogSubscription(DdcServiceQuery query,
                                    Consumer<DdcServiceCatalogSnapshot> listener) {
            this.query = query;
            this.listener = listener;
        }

        private void start() {
            start(DdcKeys.registryTopic(
                    query.env(),
                    query.namespace(),
                    query.serviceKind(),
                    query.protocol()
            ));
        }

        @Override
        protected boolean relevant(DdcRegistryEvent event) {
            return event != null
                    && event.serviceKey() != null
                    && query.matches(event.serviceKey());
        }

        @Override
        protected synchronized void refresh() {
            DdcServiceCatalogSnapshot next = loader.getServiceKeys(query);
            DdcServiceCatalogSnapshot previous = current;
            if (previous != null
                    && previous.revision() == next.revision()
                    && previous.serviceKeys().equals(next.serviceKeys())) {
                return;
            }
            current = next;
            notifySafely(listener, next);
        }

        @Override
        protected synchronized void expireLocal() {
            // Service catalog membership cannot be inferred safely without Admin.
        }
    }
}
