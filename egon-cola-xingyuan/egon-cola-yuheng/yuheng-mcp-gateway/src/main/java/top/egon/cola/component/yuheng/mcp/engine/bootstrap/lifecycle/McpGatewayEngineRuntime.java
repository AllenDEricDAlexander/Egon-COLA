package top.egon.cola.component.yuheng.mcp.engine.bootstrap.lifecycle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.SmartLifecycle;
import top.egon.cola.component.yuheng.mcp.engine.http.service.McpGatewayHttpServer;
import top.egon.cola.component.yuheng.mcp.engine.mcp.service.McpRuntimeHealthIndicator;
import top.egon.cola.component.yuheng.mcp.engine.rule.domain.McpGatewayCompiledRulesDTO;
import top.egon.cola.component.yuheng.runtime.provider.service.ProviderDirectory;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleActivationApplier;

import java.util.concurrent.Executors;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：MCP 独立生命周期和就绪状态，不启动或等待 RPC 入口/槽位。
 * English summary: Role-local LKG recovery, readiness and bounded ingress drain.
 * 用法 / Usage: Spring owns lifecycle order; shared resources remain owned by this process context.
 */
@Slf4j
@RequiredArgsConstructor
public final class McpGatewayEngineRuntime implements SmartLifecycle {

    private static final Set<String> SERVING_HEALTH_CODES = Set.of(Status.UP.getCode(), "DEGRADED");

    @NonNull
    @Qualifier("mcpGatewayHttpServer")
    private final McpGatewayHttpServer server;
    @NonNull
    @Qualifier("gatewayRuleActivationApplier")
    private final GatewayRuleActivationApplier<McpGatewayCompiledRulesDTO> activation;
    @NonNull
    @Qualifier("gatewayProviderDirectory")
    private final ProviderDirectory directory;
    @NonNull
    @Qualifier("gatewayMcpRuntimeHealthIndicator")
    private final McpRuntimeHealthIndicator health;
    private volatile boolean running;
    private volatile boolean ready;
    private ScheduledExecutorService coordinator;

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        restoreRulesSafely();
        server.start();
        running = true;
        coordinator = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "gateway-mcp-engine-readiness");
            thread.setDaemon(true);
            return thread;
        });
        refreshReadiness();
        coordinator.scheduleWithFixedDelay(this::refreshReadiness, 250, 250, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void stop() {
        running = false;
        ready = false;
        if (coordinator != null) {
            coordinator.shutdownNow();
            coordinator = null;
        }
        server.beginDrain();
        server.awaitDrain();
        server.close();
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

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    private synchronized void refreshReadiness() {
        try {
            restoreRulesSafely();
            var active = activation.active();
            // A valid LKG/previous release remains serving while its degradation is reported separately.
            ready = running && server.accepting() && active != null && activation.status().ready()
                    && directory.allAvailable(active.providerServices())
                    && SERVING_HEALTH_CODES.contains(health.health().getStatus().getCode());
        } catch (RuntimeException failure) {
            ready = false;
        }
    }

    private void restoreRulesSafely() {
        if (activation.active() == null) {
            try {
                activation.restoreLkg();
            } catch (RuntimeException failure) {
                ready = false;
            }
        }
    }
}
