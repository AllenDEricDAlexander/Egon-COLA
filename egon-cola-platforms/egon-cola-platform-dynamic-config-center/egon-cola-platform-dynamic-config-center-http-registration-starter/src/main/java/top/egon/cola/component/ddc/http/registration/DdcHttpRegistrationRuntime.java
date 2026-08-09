package top.egon.cola.component.ddc.http.registration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class DdcHttpRegistrationRuntime implements AutoCloseable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DdcHttpRegistrationRuntime.class);

    private final DdcServiceRegistryClient registry;

    private final DdcServiceKeyFactory serviceKeyFactory;

    private final DdcHttpRegistrationRuntimeProperties properties;

    private final ScheduledExecutorService scheduler;

    private final AtomicReference<DdcHttpRegistrationState> state =
            new AtomicReference<>(DdcHttpRegistrationState.NEW);

    private final AtomicBoolean heartbeatScheduled = new AtomicBoolean();

    private volatile DdcServiceRegistration registration;

    private volatile DdcLeaseSession lease;

    public DdcHttpRegistrationRuntime(
            DdcServiceRegistryClient registry,
            DdcServiceKeyFactory serviceKeyFactory,
            DdcHttpRegistrationRuntimeProperties properties) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.serviceKeyFactory = Objects.requireNonNull(
                serviceKeyFactory,
                "serviceKeyFactory"
        );
        this.properties = Objects.requireNonNull(properties, "properties");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "ddc-http-registration-lease"
            );
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void onHttpServerReady(int actualServerPort) {
        if (!properties.enabled()) {
            state.set(DdcHttpRegistrationState.STOPPED);
            return;
        }
        if (state.get() != DdcHttpRegistrationState.NEW
                && state.get() != DdcHttpRegistrationState.WAITING_SERVER) {
            throw new IllegalStateException(
                    "DDC HTTP registration runtime already initialized"
            );
        }
        state.set(DdcHttpRegistrationState.REGISTERING);
        registration = new DdcServiceRegistration(
                properties.instanceId(),
                serviceKeyFactory.fromScope(
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
                    ? DdcHttpRegistrationState.FAILED
                    : DdcHttpRegistrationState.RECOVERING);
            if (properties.failFast()) {
                throw failure;
            }
        } finally {
            if (state.get() != DdcHttpRegistrationState.FAILED) {
                scheduleHeartbeat();
            }
        }
    }

    public synchronized void heartbeatAndRecover() {
        if (registration == null
                || state.get() == DdcHttpRegistrationState.STOPPED
                || state.get() == DdcHttpRegistrationState.FAILED) {
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
                    "DDC HTTP registration heartbeat failed for {}",
                    properties.instanceId(),
                    failure
            );
            recover();
        }
    }

    public DdcHttpRegistrationState state() {
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
        state.set(DdcHttpRegistrationState.STOPPED);
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
            if (state.get() != DdcHttpRegistrationState.FAILED
                    && state.get() != DdcHttpRegistrationState.STOPPED) {
                state.set(DdcHttpRegistrationState.RECOVERING);
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
        state.set(DdcHttpRegistrationState.RECOVERING);
        try {
            register();
        } catch (RuntimeException failure) {
            LOGGER.warn(
                    "DDC HTTP registration lease recovery failed for {}",
                    properties.instanceId(),
                    failure
            );
        }
    }

    private void register() {
        lease = registry.register(registration);
        state.set(DdcHttpRegistrationState.REGISTERED);
    }
}
