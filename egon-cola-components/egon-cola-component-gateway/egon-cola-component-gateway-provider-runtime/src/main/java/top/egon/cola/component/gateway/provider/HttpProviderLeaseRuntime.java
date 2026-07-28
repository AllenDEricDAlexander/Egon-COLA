package top.egon.cola.component.gateway.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class HttpProviderLeaseRuntime implements AutoCloseable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HttpProviderLeaseRuntime.class);

    private final DdcServiceRegistryClient registry;

    private final HttpProviderRuntimeProperties properties;

    private final ScheduledExecutorService scheduler;

    private final AtomicReference<HttpProviderRuntimeState> state =
            new AtomicReference<>(HttpProviderRuntimeState.NEW);

    private final AtomicBoolean heartbeatScheduled = new AtomicBoolean();

    private volatile DdcServiceRegistration registration;

    private volatile DdcLeaseSession lease;

    public HttpProviderLeaseRuntime(
            DdcServiceRegistryClient registry,
            HttpProviderRuntimeProperties properties) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "gateway-provider-lease"
            );
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void onHttpServerReady(int actualServerPort) {
        if (!properties.enabled()) {
            state.set(HttpProviderRuntimeState.STOPPED);
            return;
        }
        if (state.get() != HttpProviderRuntimeState.NEW
                && state.get() != HttpProviderRuntimeState.WAITING_SERVER) {
            throw new IllegalStateException(
                    "HTTP provider runtime already initialized"
            );
        }
        state.set(HttpProviderRuntimeState.REGISTERING);
        registration = new DdcServiceRegistration(
                properties.instanceId(),
                new DdcServiceKey(
                        properties.env(),
                        properties.namespace(),
                        DdcServiceKind.HTTP_PROVIDER,
                        properties.serviceName(),
                        properties.group(),
                        properties.version(),
                        properties.protocol()
                ),
                properties.host(),
                properties.resolvedPort(actualServerPort),
                properties.protocol().equals("https"),
                properties.metadata(),
                properties.leaseSeconds(),
                properties.heartbeatIntervalSeconds()
        );
        try {
            register();
        } catch (RuntimeException failure) {
            state.set(properties.failFast()
                    ? HttpProviderRuntimeState.FAILED
                    : HttpProviderRuntimeState.RECOVERING);
            if (properties.failFast()) {
                throw failure;
            }
        } finally {
            if (state.get() != HttpProviderRuntimeState.FAILED) {
                scheduleHeartbeat();
            }
        }
    }

    public synchronized void heartbeatAndRecover() {
        if (registration == null
                || state.get() == HttpProviderRuntimeState.STOPPED
                || state.get() == HttpProviderRuntimeState.FAILED) {
            return;
        }
        if (lease == null) {
            recover();
            return;
        }
        try {
            DdcLeaseOperationResult result = registry.heartbeat(
                    lease.instanceId(),
                    lease.leaseId()
            );
            if (!result.renewed()) {
                lease = null;
                recover();
            } else if (result.leaseExpireAt() != null) {
                renewLeaseExpiry(result);
            }
        } catch (RuntimeException failure) {
            lease = null;
            LOGGER.warn(
                    "HTTP Provider heartbeat failed for {}",
                    properties.instanceId(),
                    failure
            );
            recover();
        }
    }

    public HttpProviderRuntimeState state() {
        return state.get();
    }

    public Optional<DdcLeaseSession> lease() {
        return Optional.ofNullable(lease);
    }

    public String instanceId() {
        return properties.instanceId();
    }

    @Override
    public synchronized void close() {
        state.set(HttpProviderRuntimeState.STOPPED);
        scheduler.shutdownNow();
        DdcLeaseSession current = lease;
        lease = null;
        if (current != null) {
            try {
                registry.deregister(
                        current.instanceId(),
                        current.leaseId()
                );
            } catch (RuntimeException ignored) {
                // TTL provides final cleanup after an unavailable DDC.
            }
        }
    }

    private void heartbeatSafely() {
        try {
            heartbeatAndRecover();
        } catch (RuntimeException ignored) {
            if (state.get() != HttpProviderRuntimeState.FAILED
                    && state.get() != HttpProviderRuntimeState.STOPPED) {
                state.set(HttpProviderRuntimeState.RECOVERING);
            }
        }
    }

    private void scheduleHeartbeat() {
        if (!heartbeatScheduled.compareAndSet(false, true)) {
            return;
        }
        scheduler.scheduleWithFixedDelay(
                this::heartbeatSafely,
                properties.heartbeatIntervalSeconds(),
                properties.heartbeatIntervalSeconds(),
                TimeUnit.SECONDS
        );
    }

    private void renewLeaseExpiry(DdcLeaseOperationResult result) {
        DdcLeaseSession current = lease;
        lease = new DdcLeaseSession(
                current.instanceId(),
                current.leaseId(),
                current.role(),
                current.leaseSeconds(),
                current.heartbeatIntervalSeconds(),
                current.registeredAt(),
                result.leaseExpireAt()
        );
    }

    private void recover() {
        state.set(HttpProviderRuntimeState.RECOVERING);
        try {
            register();
        } catch (RuntimeException failure) {
            LOGGER.warn(
                    "HTTP Provider lease recovery failed for {}",
                    properties.instanceId(),
                    failure
            );
        }
    }

    private void register() {
        lease = registry.register(registration);
        state.set(HttpProviderRuntimeState.REGISTERED);
    }
}
