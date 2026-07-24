package top.egon.cola.component.ddc.service;

import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.listener.DdcRedisChangeSubscription;
import top.egon.cola.component.ddc.model.dto.DdcDefaultReportRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.vo.DdcFieldBinding;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DdcRuntimeCoordinator implements SmartLifecycle {

    private final DdcProperties properties;

    private final DdcInstanceService instanceService;

    private final DdcAdminClient adminClient;

    private final DdcLocalConfigRepository repository;

    private final DdcRefreshService refreshService;

    private final DdcRedisChangeSubscription subscription;

    private final DdcLeaseSessionHolder sessionHolder;

    private final AtomicReference<DdcRuntimeState> state = new AtomicReference<>(DdcRuntimeState.NEW);

    private final AtomicBoolean running = new AtomicBoolean();

    private volatile ScheduledExecutorService scheduler;

    public DdcRuntimeCoordinator(DdcProperties properties,
                                 DdcInstanceService instanceService,
                                 DdcAdminClient adminClient,
                                 DdcLocalConfigRepository repository,
                                 DdcRefreshService refreshService,
                                 DdcRedisChangeSubscription subscription,
                                 DdcLeaseSessionHolder sessionHolder) {
        this.properties = properties;
        this.instanceService = instanceService;
        this.adminClient = adminClient;
        this.repository = repository;
        this.refreshService = refreshService;
        this.subscription = subscription;
        this.sessionHolder = sessionHolder;
    }

    @Override
    public synchronized void start() {
        validateScope();
        if (!subscription.isActive()) {
            throw new DdcException("DDC Redis subscription is not active");
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        state.set(DdcRuntimeState.STARTING);
        try {
            initialize();
            state.set(DdcRuntimeState.READY);
        } catch (RuntimeException exception) {
            if (properties.getConsistency().isFailFast()) {
                state.set(DdcRuntimeState.FAILED);
                running.set(false);
                throw exception;
            }
            state.set(DdcRuntimeState.RECOVERING);
        }
        startScheduler();
    }

    @Override
    public synchronized void stop() {
        if (!running.getAndSet(false) && state.get() == DdcRuntimeState.STOPPED) {
            return;
        }
        state.set(DdcRuntimeState.STOPPING);
        ScheduledExecutorService currentScheduler = scheduler;
        scheduler = null;
        if (currentScheduler != null) {
            currentScheduler.shutdownNow();
        }
        sessionHolder.current().ifPresent(session -> {
            try {
                instanceService.offline(session);
            } catch (RuntimeException ignored) {
                // Deregistration is best effort during application shutdown.
            }
        });
        subscription.close();
        sessionHolder.current().ifPresent(sessionHolder::compareAndClear);
        state.set(DdcRuntimeState.STOPPED);
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    public DdcRuntimeState state() {
        return state.get();
    }

    public Optional<DdcLeaseSession> currentSession() {
        return sessionHolder.current();
    }

    void heartbeatOnce() {
        if (!running.get() || state.get() == DdcRuntimeState.STOPPING) {
            return;
        }
        try {
            DdcLeaseSession session = sessionHolder.current().orElse(null);
            if (session == null) {
                recover();
                return;
            }
            DdcLeaseOperationResult result = instanceService.heartbeat(session);
            if (result.status() == DdcLeaseOperationStatus.NOT_FOUND
                    || result.status() == DdcLeaseOperationStatus.LEASE_MISMATCH) {
                recover();
            } else if (result.renewed()) {
                state.set(DdcRuntimeState.READY);
            }
        } catch (RuntimeException exception) {
            state.set(DdcRuntimeState.RECOVERING);
        }
    }

    private void initialize() {
        instanceService.register();
        adminClient.reportDefaults(defaultReport());
        adminClient.pull().forEach(refreshService::applySnapshot);
    }

    private void recover() {
        state.set(DdcRuntimeState.RECOVERING);
        initialize();
        state.set(DdcRuntimeState.READY);
    }

    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "egon-cola-ddc-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        int interval = properties.getInstance().getHeartbeatIntervalSeconds();
        scheduler.scheduleWithFixedDelay(this::heartbeatOnce, interval, interval, TimeUnit.SECONDS);
    }

    private DdcDefaultReportRequest defaultReport() {
        DdcDefaultReportRequest request = new DdcDefaultReportRequest();
        request.setAppCode(properties.getAppCode());
        request.setEnv(properties.getEnv());
        request.setNamespace(properties.getNamespace());
        request.setInstanceId(instanceService.identity().instanceId());
        Map<String, DdcFieldBinding> defaults = new LinkedHashMap<>();
        repository.bindings().stream()
                .sorted(Comparator.comparing(DdcFieldBinding::getConfigKey))
                .forEach(binding -> defaults.putIfAbsent(binding.getConfigKey(), binding));
        request.setConfigs(defaults.values().stream().map(this::defaultValue).toList());
        return request;
    }

    private DdcDefaultReportRequest.DdcConfigValueRequest defaultValue(DdcFieldBinding binding) {
        DdcDefaultReportRequest.DdcConfigValueRequest value =
                new DdcDefaultReportRequest.DdcConfigValueRequest();
        value.setConfigKey(binding.getConfigKey());
        value.setDefaultValue(binding.getDefaultValue());
        value.setValueType(binding.getTargetType().getName());
        return value;
    }

    private void validateScope() {
        requireText(properties.getAppCode(), "appCode");
        requireText(properties.getEnv(), "env");
        requireText(properties.getNamespace(), "namespace");
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DdcException(fieldName + " is required");
        }
    }
}
