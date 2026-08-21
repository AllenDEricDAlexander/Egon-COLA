package top.egon.cola.component.rpc.provider.lifecycle;

import io.grpc.Server;
import io.grpc.ServerInterceptor;
import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBinding;
import top.egon.cola.component.rpc.provider.binding.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLeaseManager;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistrationMode;
import top.egon.cola.component.rpc.provider.server.RpcProviderServerFactory;
import top.egon.cola.component.rpc.provider.server.RpcServerServiceDefinitionFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RpcProviderLifecycle implements SmartLifecycle {

    private static final long HEARTBEAT_STOP_TIMEOUT_SECONDS = 5;

    private final RpcProviderMethodRegistry registry;

    private final RpcServerServiceDefinitionFactory definitionFactory;

    private final RpcProviderServerFactory serverFactory;

    private final RpcProviderLeaseManager leaseManager;

    private final RpcProviderAvailabilityRegistry availability;

    private final List<ServerInterceptor> interceptors;

    private final EgonRpcProperties.Provider properties;

    private final RpcProcessIdentity processIdentity;

    private volatile boolean running;

    private volatile RpcProviderRuntimeState state = RpcProviderRuntimeState.NEW;

    private final AtomicBoolean stopCallbackInvoked = new AtomicBoolean();

    private volatile Server server;

    private volatile ScheduledExecutorService heartbeatExecutor;

    public RpcProviderLifecycle(
            RpcProviderMethodRegistry registry,
            RpcServerServiceDefinitionFactory definitionFactory,
            RpcProviderServerFactory serverFactory,
            RpcProviderLeaseManager leaseManager,
            RpcProviderAvailabilityRegistry availability,
            List<ServerInterceptor> interceptors,
            EgonRpcProperties rpcProperties,
            RpcProcessIdentity processIdentity) {
        this.registry = registry;
        this.definitionFactory = definitionFactory;
        this.serverFactory = serverFactory;
        this.leaseManager = leaseManager;
        this.availability = availability;
        this.interceptors = List.copyOf(interceptors);
        this.properties = rpcProperties.getProvider();
        this.processIdentity = processIdentity;
    }

    @Override
    public synchronized void start() {
        if (state == RpcProviderRuntimeState.READY
                || state == RpcProviderRuntimeState.DEGRADED) {
            return;
        }
        if (state == RpcProviderRuntimeState.DRAINING
                || state == RpcProviderRuntimeState.FAILED
                || state == RpcProviderRuntimeState.STOPPED) {
            throw startFailed("RPC Provider is not restartable after shutdown/failure", null);
        }
        List<RpcProviderBinding> providers;
        try {
            validateProperties();
            providers = registry.providers();
            if (providers.isEmpty()) {
                throw startFailed("no RPC Provider bean was found", null);
            }
            state = RpcProviderRuntimeState.STARTING;
        } catch (RuntimeException exception) {
            state = RpcProviderRuntimeState.FAILED;
            throw exception;
        }
        try {
            Server preparedServer = serverFactory.create(
                    properties.getBindAddress(),
                    properties.getPort(),
                    definitionFactory.create(registry),
                    interceptors
            );
            if (!registrationEnabled()) {
                providers.forEach(binding -> availability.available(
                        binding.serviceIdentity()
                ));
            }
            server = preparedServer.start();
            if (registrationEnabled()) {
                String advertisedHost = advertisedHost();
                int advertisedPort = advertisedPort(server.getPort());
                leaseManager.prepare(
                        providers,
                        advertisedHost,
                        advertisedPort
                );
                leaseManager.enableRecovery();
                try {
                    leaseManager.registerAll();
                } catch (RuntimeException exception) {
                    if (properties.isRegistrationFailFast()) {
                        throw exception;
                    }
                }
                startHeartbeat();
            }
            state = registrationEnabled()
                    && leaseManager != null
                    && !leaseManager.allPreparedLeasesActive()
                    ? RpcProviderRuntimeState.DEGRADED
                    : RpcProviderRuntimeState.READY;
            running = state.servingNewCalls();
        } catch (IOException | RuntimeException exception) {
            stopInfrastructure();
            state = RpcProviderRuntimeState.FAILED;
            throw startFailed("RPC Provider startup failed", exception);
        }
    }

    @Override
    public synchronized void stop() {
        stop(null);
    }

    @Override
    public synchronized void stop(Runnable callback) {
        if (state == RpcProviderRuntimeState.STOPPED) {
            runOnce(callback);
            return;
        }
        state = RpcProviderRuntimeState.DRAINING;
        running = false;
        availability.clear();
        // Recovery is disabled before deregistration so shutdown cannot publish
        // a replacement lease after the exact active lease was removed.
        if (registrationEnabled()) {
            leaseManager.disableRecovery();
            stopHeartbeat();
            leaseManager.deregisterAll();
        }
        Server current = server;
        server = null;
        if (current != null) {
            current.shutdown();
            try {
                if (!current.awaitTermination(
                        properties.getGracefulShutdownTimeoutMs(),
                        TimeUnit.MILLISECONDS
                )) {
                    current.shutdownNow();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                current.shutdownNow();
            }
        }
        state = RpcProviderRuntimeState.STOPPED;
        runOnce(callback);
    }

    @Override
    public boolean isRunning() {
        return state.servingNewCalls();
    }

    public RpcProviderRuntimeState state() {
        return state;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    public int boundPort() {
        Server current = server;
        return current == null ? -1 : current.getPort();
    }

    private void validateProperties() {
        if (properties.getBindAddress() == null
                || properties.getBindAddress().isBlank()) {
            throw startFailed("RPC Provider bind address is required", null);
        }
        if (properties.getPort() < 0 || properties.getPort() > 65535) {
            throw startFailed("RPC Provider port is invalid", null);
        }
        if (properties.getRegistrationMode() == null) {
            throw startFailed(
                    "RPC Provider registration mode is required",
                    null
            );
        }
        if (registrationEnabled() && leaseManager == null) {
            throw startFailed(
                    "RpcProviderRegistry is required; configure an adapter "
                            + "or set egon.cola.component.rpc.provider."
                            + "registration-mode=disabled",
                    null
            );
        }
        if (registrationEnabled()
                && (properties.getLeaseSeconds() <= 0
                || properties.getHeartbeatIntervalSeconds() <= 0
                || properties.getHeartbeatIntervalSeconds()
                >= properties.getLeaseSeconds())) {
            throw startFailed("RPC Provider lease settings are invalid", null);
        }
        if (properties.getGracefulShutdownTimeoutMs() < 0) {
            throw startFailed("RPC Provider shutdown timeout is invalid", null);
        }
    }

    private String advertisedHost() {
        String configured = properties.getAdvertisedHost();
        String resolved = configured == null || configured.isBlank()
                ? processIdentity.host()
                : configured;
        if ("0.0.0.0".equals(resolved) || "::".equals(resolved)) {
            throw startFailed(
                    "RPC Provider advertised host must be routable",
                    null
            );
        }
        return resolved;
    }

    private int advertisedPort(int boundPort) {
        Integer configured = properties.getAdvertisedPort();
        int resolved = configured == null ? boundPort : configured;
        if (resolved <= 0 || resolved > 65535) {
            throw startFailed("RPC Provider advertised port is invalid", null);
        }
        return resolved;
    }

    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "egon-rpc-provider-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeatExecutor.scheduleWithFixedDelay(
                this::heartbeatAndRefreshState,
                properties.getHeartbeatIntervalSeconds(),
                properties.getHeartbeatIntervalSeconds(),
                TimeUnit.SECONDS
        );
    }

    private void heartbeatAndRefreshState() {
        if (state == RpcProviderRuntimeState.DRAINING
                || state == RpcProviderRuntimeState.STOPPED) {
            return;
        }
        try {
            leaseManager.heartbeatAndRecover();
            state = leaseManager.allPreparedLeasesActive()
                    ? RpcProviderRuntimeState.READY
                    : RpcProviderRuntimeState.DEGRADED;
            running = state.servingNewCalls();
        } catch (RuntimeException exception) {
            state = RpcProviderRuntimeState.DEGRADED;
            running = true;
        }
    }

    private void stopHeartbeat() {
        ScheduledExecutorService executor = heartbeatExecutor;
        heartbeatExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(
                        HEARTBEAT_STOP_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                )) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void stopInfrastructure() {
        availability.clear();
        if (registrationEnabled() && leaseManager != null) {
            leaseManager.disableRecovery();
            stopHeartbeat();
            leaseManager.deregisterAll();
        }
        Server current = server;
        server = null;
        if (current != null) {
            current.shutdownNow();
        }
        running = false;
    }

    private void runOnce(Runnable callback) {
        if (callback != null && stopCallbackInvoked.compareAndSet(false, true)) {
            callback.run();
        }
    }

    private boolean registrationEnabled() {
        return properties.getRegistrationMode()
                == RpcProviderRegistrationMode.REQUIRED;
    }

    private EgonRpcException startFailed(String message, Throwable cause) {
        return cause == null
                ? new EgonRpcException(
                        EgonRpcErrorCode.RPC_PROVIDER_START_FAILED,
                        message
                )
                : new EgonRpcException(
                        EgonRpcErrorCode.RPC_PROVIDER_START_FAILED,
                        message,
                        cause
                );
    }
}
