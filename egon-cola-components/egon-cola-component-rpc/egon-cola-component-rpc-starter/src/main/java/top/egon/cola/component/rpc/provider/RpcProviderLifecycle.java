package top.egon.cola.component.rpc.provider;

import io.grpc.Server;
import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.RpcProviderServerInterceptor;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RpcProviderLifecycle implements SmartLifecycle {

    private static final long HEARTBEAT_STOP_TIMEOUT_SECONDS = 5;

    private final RpcProviderBeanScanner scanner;

    private final RpcServerServiceDefinitionFactory definitionFactory;

    private final RpcProviderServerFactory serverFactory;

    private final RpcProviderLeaseManager leaseManager;

    private final RpcProviderAvailabilityRegistry availability;

    private final RpcProviderServerInterceptor interceptor;

    private final EgonRpcProperties.Provider properties;

    private final RpcProcessIdentity processIdentity;

    private volatile boolean running;

    private volatile Server server;

    private volatile ScheduledExecutorService heartbeatExecutor;

    public RpcProviderLifecycle(
            RpcProviderBeanScanner scanner,
            RpcServerServiceDefinitionFactory definitionFactory,
            RpcProviderServerFactory serverFactory,
            RpcProviderLeaseManager leaseManager,
            RpcProviderAvailabilityRegistry availability,
            RpcProviderServerInterceptor interceptor,
            EgonRpcProperties rpcProperties,
            RpcProcessIdentity processIdentity) {
        this.scanner = scanner;
        this.definitionFactory = definitionFactory;
        this.serverFactory = serverFactory;
        this.leaseManager = leaseManager;
        this.availability = availability;
        this.interceptor = interceptor;
        this.properties = rpcProperties.getProvider();
        this.processIdentity = processIdentity;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        validateProperties();
        RpcProviderMethodRegistry registry = scanner.scan();
        List<RpcProviderBinding> providers = registry.providers();
        if (providers.isEmpty()) {
            throw startFailed("no RPC Provider bean was found", null);
        }
        try {
            server = serverFactory.create(
                    properties.getBindAddress(),
                    properties.getPort(),
                    definitionFactory.create(registry),
                    interceptor
            ).start();
            String advertisedHost = advertisedHost();
            int advertisedPort = advertisedPort(server.getPort());
            leaseManager.prepare(providers, advertisedHost, advertisedPort);
            leaseManager.enableRecovery();
            try {
                leaseManager.registerAll();
            } catch (RuntimeException exception) {
                if (properties.isRegistrationFailFast()) {
                    throw exception;
                }
            }
            startHeartbeat();
            running = true;
        } catch (IOException | RuntimeException exception) {
            stopInfrastructure();
            throw startFailed("RPC Provider startup failed", exception);
        }
    }

    @Override
    public synchronized void stop() {
        if (!running && server == null) {
            return;
        }
        availability.clear();
        // Recovery is disabled before deregistration so shutdown cannot publish
        // a replacement lease after the exact active lease was removed.
        leaseManager.disableRecovery();
        stopHeartbeat();
        leaseManager.deregisterAll();
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
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
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
        if (properties.getLeaseSeconds() <= 0
                || properties.getHeartbeatIntervalSeconds() <= 0
                || properties.getHeartbeatIntervalSeconds()
                >= properties.getLeaseSeconds()) {
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
                leaseManager::heartbeatAndRecover,
                properties.getHeartbeatIntervalSeconds(),
                properties.getHeartbeatIntervalSeconds(),
                TimeUnit.SECONDS
        );
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
        leaseManager.disableRecovery();
        stopHeartbeat();
        leaseManager.deregisterAll();
        Server current = server;
        server = null;
        if (current != null) {
            current.shutdownNow();
        }
        running = false;
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
