package top.egon.cola.component.ddc.http.registration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 在 HTTP 服务监听成功后，使用 IdP 准入票据维护其 DDC 服务租约。
 * / Maintains an HTTP service's DDC lease with IdP admission tickets after the server starts listening.
 */
public final class DdcHttpRegistrationRuntime implements AutoCloseable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DdcHttpRegistrationRuntime.class);

    private final DdcServiceRegistryClient registry;

    private final DdcServiceKeyFactory serviceKeyFactory;

    private final DdcHttpRegistrationRuntimeProperties properties;

    /** 为每次注册和心跳提供精确绑定的短期票据。 / Supplies an exactly bound short-lived ticket for each registration and heartbeat. */
    private final DdcAdmissionTicketSupplier admissionTickets;

    private final ScheduledExecutorService scheduler;

    private final AtomicReference<DdcHttpRegistrationState> state =
            new AtomicReference<>(DdcHttpRegistrationState.NEW);

    private final AtomicBoolean heartbeatScheduled = new AtomicBoolean();

    private volatile DdcServiceRegistration registration;

    /** Web Server 实际监听端口。 / Actual port bound by the web server. */
    private volatile Integer actualServerPort;

    private volatile DdcLeaseSession lease;

    /**
     * 创建默认 Fail Closed 的 HTTP 注册运行时。
     * / Creates the HTTP registration runtime, which fails closed when a ticket cannot be obtained.
     *
     * @param registry DDC 服务注册客户端 / DDC service-registry client
     * @param serviceKeyFactory 服务键工厂 / service-key factory
     * @param properties HTTP 注册参数 / HTTP registration settings
     * @param admissionTickets 准入票据端口 / admission-ticket port
     */
    public DdcHttpRegistrationRuntime(
            DdcServiceRegistryClient registry,
            DdcServiceKeyFactory serviceKeyFactory,
            DdcHttpRegistrationRuntimeProperties properties,
            DdcAdmissionTicketSupplier admissionTickets) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.serviceKeyFactory = Objects.requireNonNull(
                serviceKeyFactory,
                "serviceKeyFactory"
        );
        this.properties = Objects.requireNonNull(properties, "properties");
        this.admissionTickets = Objects.requireNonNull(
                admissionTickets,
                "admissionTickets"
        );
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "ddc-http-registration-lease"
            );
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 记录实际监听端口并执行首次准入注册。
     * / Records the actual listening port and performs the initial admitted registration.
     *
     * @param actualServerPort Web Server 实际端口 / actual web-server port
     */
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
        this.actualServerPort = actualServerPort;
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

    /**
     * 携带新鲜准入票据续约；续约失败时重新注册。
     * / Renews with a fresh admission ticket and re-registers after renewal failure.
     */
    public synchronized void heartbeatAndRecover() {
        if (actualServerPort == null
                || state.get() == DdcHttpRegistrationState.STOPPED
                || state.get() == DdcHttpRegistrationState.FAILED) {
            return;
        }
        if (lease == null) {
            recover();
            return;
        }
        try {
            DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
            request.setServiceKey(registration.serviceKey());
            request.setInstanceId(lease.instanceId());
            request.setLeaseId(lease.leaseId());
            request.setAdmissionTicket(admissionTicket(
                    registration.serviceKey()
            ));
            DdcLeaseOperationResult result = registry.heartbeat(request);
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

    /** 使用实际端口和新鲜票据建立新租约。 / Establishes a new lease with the actual port and a fresh ticket. */
    private void register() {
        DdcServiceKey serviceKey = serviceKeyFactory.fromScope(
                DdcServiceKind.HTTP_PROVIDER,
                properties.serviceName(),
                properties.group(),
                properties.version(),
                properties.protocol()
        );
        registration = new DdcServiceRegistration(
                properties.instanceId(),
                serviceKey,
                properties.host(),
                properties.resolvedPort(actualServerPort),
                properties.protocol().equals("https"),
                properties.metadata(),
                properties.leaseSeconds(),
                properties.heartbeatIntervalSeconds(),
                admissionTicket(serviceKey)
        );
        lease = registry.register(registration);
        state.set(DdcHttpRegistrationState.REGISTERED);
    }

    /**
     * 为当前 HTTP 实例和精确物理作用域取得原始票据。
     * / Obtains the raw ticket for the current HTTP instance and exact physical scope.
     *
     * @param serviceKey 实际发送的服务键 / service key actually sent
     * @return 原始短期准入 JWT / raw short-lived admission JWT
     */
    private String admissionTicket(DdcServiceKey serviceKey) {
        return admissionTickets.getTicket(
                serviceKey.bizCode(),
                serviceKey.appCode(),
                serviceKey.env(),
                properties.instanceId()
        ).value();
    }
}
