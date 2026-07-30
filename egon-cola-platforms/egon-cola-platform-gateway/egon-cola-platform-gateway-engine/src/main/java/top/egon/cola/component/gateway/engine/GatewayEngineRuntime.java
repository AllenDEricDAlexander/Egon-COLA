package top.egon.cola.component.gateway.engine;

import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.gateway.engine.discovery.ProviderDirectory;
import top.egon.cola.component.gateway.engine.http.GatewayHttpServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewayServer;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewaySlotRuntime;
import top.egon.cola.component.gateway.engine.rpc.RpcGatewaySubsystemState;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleActivationApplier;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class GatewayEngineRuntime implements SmartLifecycle {

    private final GatewayEngineRuntimeProperties properties;

    private final GatewayHttpServer httpServer;

    private final RpcGatewayServer rpcServer;

    private final RpcGatewaySlotRuntime rpcSlot;

    private final GatewayRuleActivationApplier activation;

    private final ProviderDirectory providerDirectory;

    private volatile boolean running;

    private volatile boolean ready;

    private ScheduledExecutorService coordinator;

    public GatewayEngineRuntime(
            GatewayEngineRuntimeProperties properties,
            GatewayHttpServer httpServer,
            RpcGatewayServer rpcServer,
            RpcGatewaySlotRuntime rpcSlot,
            GatewayRuleActivationApplier activation,
            ProviderDirectory providerDirectory) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.httpServer = Objects.requireNonNull(httpServer, "httpServer");
        this.rpcServer = Objects.requireNonNull(rpcServer, "rpcServer");
        this.rpcSlot = Objects.requireNonNull(rpcSlot, "rpcSlot");
        this.activation = Objects.requireNonNull(activation, "activation");
        this.providerDirectory = Objects.requireNonNull(
                providerDirectory,
                "providerDirectory"
        );
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        activation.restoreLkg();
        httpServer.start();
        if (properties.getRpc().isEnabled()) {
            rpcServer.start();
            rpcSlot.listenerStarted(rpcServer.port());
        }
        running = true;
        coordinator = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "gateway-engine-readiness"
            );
            thread.setDaemon(true);
            return thread;
        });
        refreshReadiness();
        coordinator.scheduleWithFixedDelay(
                this::refreshReadinessSafely,
                250,
                250,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public synchronized void stop() {
        ready = false;
        running = false;
        if (coordinator != null) {
            coordinator.shutdownNow();
            coordinator = null;
        }
        httpServer.beginDrain();
        rpcSlot.beginDrain();
        httpServer.awaitDrain();
        rpcServer.close();
        httpServer.close();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public boolean running() {
        return running;
    }

    public boolean ready() {
        return ready;
    }

    public RpcGatewaySubsystemState rpcState() {
        return rpcSlot.state();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    private synchronized void refreshReadiness() {
        boolean rulesReady = activation.active() != null
                && activation.status().ready();
        boolean providersReady = rulesReady
                && providerDirectory.allAvailable(
                activation.active().providerServices()
        );
        if (rulesReady
                && providersReady
                && properties.getRpc().isEnabled()
                && rpcSlot.state()
                == RpcGatewaySubsystemState.LISTENING_NOT_REGISTERED) {
            rpcSlot.engineReady();
        }
        boolean rpcReady = !properties.getRpc().isEnabled()
                || rpcSlot.state()
                == RpcGatewaySubsystemState.REGISTERED_READY;
        ready = running
                && httpServer.accepting()
                && rulesReady
                && providersReady
                && rpcReady;
    }

    private void refreshReadinessSafely() {
        try {
            refreshReadiness();
        } catch (RuntimeException failure) {
            ready = false;
        }
    }
}
