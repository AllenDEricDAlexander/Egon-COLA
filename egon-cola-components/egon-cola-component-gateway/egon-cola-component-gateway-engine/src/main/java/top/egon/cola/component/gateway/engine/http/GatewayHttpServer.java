package top.egon.cola.component.gateway.engine.http;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GatewayHttpServer implements AutoCloseable {

    private final GatewayHttpListener publicListener;

    private final GatewayHttpListener internalListener;

    private final AtomicBoolean accepting = new AtomicBoolean();

    public GatewayHttpServer(
            GatewayHttpEngineProperties properties,
            GatewayHttpDataPlaneHandler handler) {
        Objects.requireNonNull(properties, "properties");
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
        accepting.set(false);
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
        accepting.set(false);
        publicListener.close();
        internalListener.close();
    }

    private GatewayHttpDataPlaneHandler guarded(
            GatewayHttpDataPlaneHandler delegate) {
        Objects.requireNonNull(delegate, "handler");
        return (zone, request) -> accepting.get()
                ? delegate.handle(zone, request)
                : reactor.core.publisher.Mono.just(
                        GatewayOutboundHttpResponse.text(
                                503,
                                "GATEWAY_ENGINE_DRAINING"
                        )
                );
    }
}
