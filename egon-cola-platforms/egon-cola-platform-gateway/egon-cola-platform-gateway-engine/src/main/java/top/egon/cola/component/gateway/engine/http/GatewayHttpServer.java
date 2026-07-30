package top.egon.cola.component.gateway.engine.http;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import top.egon.cola.component.gateway.engine.websocket.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketCloseStatus;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketPeer;

public final class GatewayHttpServer implements AutoCloseable {

    private final GatewayHttpListener publicListener;

    private final GatewayHttpListener internalListener;

    private final Duration drainTimeout;

    private final AtomicBoolean accepting = new AtomicBoolean();

    private final Object drainMonitor = new Object();

    private long inFlightRequests;

    private final Set<ActiveRequest> activeRequests =
            ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<GatewayPreparedWebSocketSession,
            ActiveRequest> webSockets = new ConcurrentHashMap<>();

    public GatewayHttpServer(
            GatewayHttpEngineProperties properties,
            GatewayHttpDataPlaneHandler handler) {
        Objects.requireNonNull(properties, "properties");
        this.drainTimeout = properties.drainTimeout();
        this.publicListener = new GatewayHttpListener(
                AccessZone.PUBLIC,
                properties.publicListener(),
                guarded(handler)
        );
        this.internalListener = new GatewayHttpListener(
                AccessZone.INTERNAL,
                properties.internalListener(),
                guarded(handler)
        );
    }

    public void start() {
        try {
            publicListener.start();
            internalListener.start();
            accepting.set(true);
        } catch (RuntimeException failure) {
            close();
            throw failure;
        }
    }

    public void beginDrain() {
        synchronized (drainMonitor) {
            accepting.set(false);
            if (inFlightRequests == 0) {
                drainMonitor.notifyAll();
            }
        }
    }

    public boolean awaitDrain() {
        long deadline = System.nanoTime() + drainTimeout.toNanos();
        synchronized (drainMonitor) {
            long remainingNanos = deadline - System.nanoTime();
            while (inFlightRequests > 0 && remainingNanos > 0) {
                try {
                    long millis = remainingNanos / 1_000_000;
                    int nanos = (int) (remainingNanos % 1_000_000);
                    drainMonitor.wait(millis, nanos);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                remainingNanos = deadline - System.nanoTime();
            }
            boolean drained = inFlightRequests == 0;
            if (!drained) {
                activeRequests.forEach(ActiveRequest::force);
            }
            return drained;
        }
    }

    public boolean accepting() {
        return accepting.get();
    }

    public int publicPort() {
        return publicListener.port();
    }

    public int internalPort() {
        return internalListener.port();
    }

    public synchronized void reloadTransportSecurity() {
        beginDrain();
        awaitDrain();
        publicListener.close();
        internalListener.close();
        try {
            publicListener.start();
            internalListener.start();
            accepting.set(true);
        } catch (RuntimeException failure) {
            publicListener.close();
            internalListener.close();
            throw failure;
        }
    }

    @Override
    public void close() {
        beginDrain();
        publicListener.close();
        internalListener.close();
    }

    private GatewayHttpDataPlaneHandler guarded(
            GatewayHttpDataPlaneHandler delegate) {
        Objects.requireNonNull(delegate, "handler");
        return new GatewayHttpDataPlaneHandler() {
            @Override
            public Mono<GatewayOutboundHttpResponse> handle(
                    AccessZone zone,
                    GatewayInboundHttpRequest request) {
                return Mono.defer(() -> {
                    ActiveRequest active = requestStarted();
                    if (active == null) {
                        return Mono.just(GatewayOutboundHttpResponse.text(
                                503,
                                "GATEWAY_ENGINE_DRAINING"
                        ));
                    }
                    AtomicBoolean responseHandedOff = new AtomicBoolean();
                    try {
                        return Objects.requireNonNull(
                                        delegate.handle(zone, request),
                                        "handler result"
                                )
                                .takeUntilOther(active.stop())
                                .map(response -> {
                                    responseHandedOff.set(true);
                                    return response.withHeadersAndBody(
                                            response.headers(),
                                            response.body()
                                                    .takeUntilOther(
                                                            active.stop()
                                                    )
                                                    .doFinally(ignored ->
                                                            active.complete()
                                                    )
                                    ).onAbandon(active::complete);
                                })
                                .doFinally(ignored -> {
                                    if (!responseHandedOff.get()) {
                                        active.complete();
                                    }
                                });
                    } catch (RuntimeException failure) {
                        active.complete();
                        throw failure;
                    }
                });
            }

            @Override
            public Mono<GatewayWebSocketHandshakeResult> prepareWebSocket(
                    AccessZone zone,
                    GatewayInboundHttpRequest request) {
                return Mono.defer(() -> {
                    ActiveRequest active = requestStarted();
                    if (active == null) {
                        return Mono.just(
                                GatewayWebSocketHandshakeResult.rejected(
                                        503,
                                        "GATEWAY_ENGINE_DRAINING",
                                        "gateway engine is draining"
                                )
                        );
                    }
                    AtomicBoolean handedOff = new AtomicBoolean();
                    return delegate.prepareWebSocket(zone, request)
                            .takeUntilOther(active.stop())
                            .map(result -> {
                                if (result instanceof
                                        GatewayWebSocketHandshakeResult
                                                .Rejected) {
                                    active.complete();
                                    return result;
                                }
                                GatewayPreparedWebSocketSession session =
                                        ((GatewayWebSocketHandshakeResult
                                                .Accepted) result).session();
                                GatewayPreparedWebSocketSession managed =
                                        session.onDispose(active::complete);
                                webSockets.put(managed, active);
                                active.onForce(managed::dispose);
                                handedOff.set(true);
                                return new GatewayWebSocketHandshakeResult
                                        .Accepted(managed);
                            })
                            .doFinally(ignored -> {
                                if (!handedOff.get()) {
                                    active.complete();
                                }
                            });
                });
            }

            @Override
            public Mono<Void> bridgeWebSocket(
                    GatewayPreparedWebSocketSession upstream,
                    GatewayWebSocketPeer downstream) {
                ActiveRequest active = webSockets.get(upstream);
                if (active != null) {
                    active.onForce(() -> Mono.whenDelayError(
                                    upstream.upstream().sendClose(
                                            GatewayWebSocketCloseStatus
                                                    .goingAway()
                                    ),
                                    downstream.sendClose(
                                            GatewayWebSocketCloseStatus
                                                    .goingAway()
                                    )
                            )
                            .onErrorResume(ignored -> Mono.empty())
                            .doFinally(ignored -> {
                                upstream.dispose();
                                downstream.dispose();
                            })
                            .subscribe());
                }
                return delegate.bridgeWebSocket(upstream, downstream)
                        .doFinally(ignored -> {
                            webSockets.remove(upstream);
                            upstream.dispose();
                            if (active != null) {
                                active.complete();
                            }
                        });
            }
        };
    }

    private ActiveRequest requestStarted() {
        synchronized (drainMonitor) {
            if (!accepting.get()) {
                return null;
            }
            inFlightRequests++;
            ActiveRequest active = new ActiveRequest();
            activeRequests.add(active);
            return active;
        }
    }

    private void requestCompleted() {
        synchronized (drainMonitor) {
            inFlightRequests--;
            if (inFlightRequests == 0) {
                drainMonitor.notifyAll();
            }
        }
    }

    private final class ActiveRequest {

        private final AtomicBoolean completed = new AtomicBoolean();

        private final AtomicBoolean forced = new AtomicBoolean();

        private final Sinks.Empty<Void> stop = Sinks.empty();

        private final AtomicReference<Runnable> forceAction =
                new AtomicReference<>(() -> {
                });

        private Mono<Void> stop() {
            return stop.asMono();
        }

        private void onForce(Runnable action) {
            forceAction.set(Objects.requireNonNull(action, "action"));
            if (forced.get()) {
                action.run();
            }
        }

        private void force() {
            if (forced.compareAndSet(false, true)) {
                stop.tryEmitEmpty();
                forceAction.get().run();
            }
        }

        private void complete() {
            if (completed.compareAndSet(false, true)) {
                activeRequests.remove(this);
                requestCompleted();
            }
        }
    }
}
