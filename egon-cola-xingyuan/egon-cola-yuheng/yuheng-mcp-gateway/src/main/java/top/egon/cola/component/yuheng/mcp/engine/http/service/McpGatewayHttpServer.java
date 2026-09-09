package top.egon.cola.component.yuheng.mcp.engine.http.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.mcp.engine.config.McpGatewayEngineProperties;
import top.egon.cola.component.yuheng.runtime.http.domain.GatewayHttpEngineProperties;
import top.egon.cola.component.yuheng.runtime.http.domain.GatewayInboundHttpRequest;
import top.egon.cola.component.yuheng.runtime.http.service.GatewayHttpListener;
import top.egon.cola.component.yuheng.runtime.http.service.GatewayOutboundHttpResponse;
import top.egon.cola.component.yuheng.runtime.security.domain.GatewayTransportSecurity;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 中文说明：MCP 独立监听器，在响应体和 SSE 完成前保留排空计数。
 * English summary: Owns one listener and bounded in-flight shutdown, including unconsumed responses.
 * 用法 / Usage: The MCP lifecycle starts, drains and closes this server; API ingress is never delegated.
 */
@Slf4j
@RequiredArgsConstructor
public final class McpGatewayHttpServer implements AutoCloseable {

    @NonNull
    @Qualifier("mcpGatewayListenerProperties")
    private final McpGatewayEngineProperties.ListenerProperties properties;
    @NonNull
    @Qualifier("mcpGatewayHttpDataPlaneHandlerAdapter")
    private final McpGatewayHttpDataPlaneHandlerAdapter handler;
    private final Object drainMonitor = new Object();
    private final Set<ActiveRequest> activeRequests = new HashSet<>();
    private volatile boolean accepting;
    private GatewayHttpListener listener;

    public synchronized void start() {
        if (listener != null) {
            return;
        }
        var tls = properties.tls();
        var transport = new GatewayTransportSecurity(tls.enabled(), tls.developmentPlaintext(),
                tls.certificateChainPath(), tls.privateKeyPath(), tls.trustCertificateCollectionPath(),
                tls.clientCertificateRequired());
        listener = new GatewayHttpListener(AccessZone.PUBLIC,
                new GatewayHttpEngineProperties.Listener(
                        properties.enabled(), properties.host(), properties.port(), transport), this::handle);
        try {
            listener.start();
            accepting = properties.enabled();
        } catch (RuntimeException failure) {
            close();
            throw failure;
        }
    }

    public void beginDrain() {
        synchronized (drainMonitor) {
            accepting = false;
            drainMonitor.notifyAll();
        }
    }

    public boolean awaitDrain() {
        long deadline = System.nanoTime() + properties.drainTimeout().toNanos();
        Set<ActiveRequest> unfinished;
        synchronized (drainMonitor) {
            long remaining = deadline - System.nanoTime();
            while (!activeRequests.isEmpty() && remaining > 0) {
                try {
                    drainMonitor.wait(remaining / 1_000_000, (int) (remaining % 1_000_000));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
                remaining = deadline - System.nanoTime();
            }
            unfinished = Set.copyOf(activeRequests);
        }
        // Signal outside the monitor: cancellation may complete on another event-loop thread.
        unfinished.forEach(ActiveRequest::force);
        return unfinished.isEmpty();
    }

    public boolean accepting() {
        return accepting;
    }

    public synchronized int publicPort() {
        return listener == null ? properties.port() : listener.port();
    }

    @Override
    public synchronized void close() {
        beginDrain();
        Set<ActiveRequest> unfinished;
        synchronized (drainMonitor) {
            unfinished = Set.copyOf(activeRequests);
        }
        unfinished.forEach(ActiveRequest::force);
        if (listener != null) {
            listener.close();
            listener = null;
        }
    }

    private Mono<GatewayOutboundHttpResponse> handle(AccessZone zone, GatewayInboundHttpRequest request) {
        return Mono.defer(() -> {
            ActiveRequest active;
            synchronized (drainMonitor) {
                if (!accepting) {
                    return Mono.just(GatewayOutboundHttpResponse.text(503, "GATEWAY_ENGINE_DRAINING"));
                }
                active = new ActiveRequest();
                activeRequests.add(active);
            }
            AtomicBoolean handedOff = new AtomicBoolean();
            return Mono.defer(() -> handler.handle(zone, request))
                    .takeUntilOther(active.stop.asMono())
                    .map(response -> {
                        handedOff.set(true);
                        return response.withHeadersAndBody(response.headers(),
                                response.body().takeUntilOther(active.stop.asMono())
                                        .doFinally(ignored -> active.complete()))
                                .onAbandon(active::complete);
                    })
                    .doFinally(ignored -> {
                        if (!handedOff.get()) {
                            active.complete();
                        }
                    });
        });
    }

    private final class ActiveRequest {
        private final Sinks.Empty<Void> stop = Sinks.empty();
        private final AtomicBoolean completed = new AtomicBoolean();

        private void force() {
            stop.tryEmitEmpty();
            complete();
        }

        private void complete() {
            if (completed.compareAndSet(false, true)) {
                synchronized (drainMonitor) {
                    activeRequests.remove(this);
                    drainMonitor.notifyAll();
                }
            }
        }
    }
}
