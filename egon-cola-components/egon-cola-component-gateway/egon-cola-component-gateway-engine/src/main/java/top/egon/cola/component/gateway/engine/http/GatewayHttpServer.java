package top.egon.cola.component.gateway.engine.http;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GatewayHttpServer implements AutoCloseable {

    private final GatewayHttpListener publicListener;

    private final GatewayHttpListener internalListener;

    private final Duration drainTimeout;

    private final AtomicBoolean accepting = new AtomicBoolean();

    private final Object drainMonitor = new Object();

    private long inFlightRequests;

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
            return inFlightRequests == 0;
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

    @Override
    public void close() {
        beginDrain();
        publicListener.close();
        internalListener.close();
    }

    private GatewayHttpDataPlaneHandler guarded(
            GatewayHttpDataPlaneHandler delegate) {
        Objects.requireNonNull(delegate, "handler");
        return (zone, request) -> reactor.core.publisher.Mono.defer(() -> {
            if (!requestStarted()) {
                return reactor.core.publisher.Mono.just(
                        GatewayOutboundHttpResponse.text(
                                503,
                                "GATEWAY_ENGINE_DRAINING"
                        )
                );
            }
            try {
                return Objects.requireNonNull(
                        delegate.handle(zone, request),
                        "handler result"
                ).doFinally(ignored -> requestCompleted());
            } catch (RuntimeException failure) {
                requestCompleted();
                throw failure;
            }
        });
    }

    private boolean requestStarted() {
        synchronized (drainMonitor) {
            if (!accepting.get()) {
                return false;
            }
            inFlightRequests++;
            return true;
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
}
