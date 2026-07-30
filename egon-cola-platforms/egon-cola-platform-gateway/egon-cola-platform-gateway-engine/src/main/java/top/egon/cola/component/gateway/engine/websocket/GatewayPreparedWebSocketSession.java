package top.egon.cola.component.gateway.engine.websocket;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An already accepted upstream session held before the downstream 101.
 */
public final class GatewayPreparedWebSocketSession implements AutoCloseable {

    private final GatewayWebSocketProxyContext context;

    private final GatewayWebSocketPeer upstream;

    private final String selectedSubprotocol;

    private final AtomicBoolean disposed = new AtomicBoolean();

    public GatewayPreparedWebSocketSession(
            GatewayWebSocketProxyContext context,
            GatewayWebSocketPeer upstream,
            String selectedSubprotocol) {
        this.context = Objects.requireNonNull(context, "context");
        this.upstream = Objects.requireNonNull(upstream, "upstream");
        this.selectedSubprotocol = selectedSubprotocol == null
                || selectedSubprotocol.isBlank()
                ? null
                : selectedSubprotocol;
    }

    public GatewayWebSocketProxyContext context() {
        return context;
    }

    public GatewayWebSocketPeer upstream() {
        return upstream;
    }

    public String selectedSubprotocol() {
        return selectedSubprotocol;
    }

    public boolean dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return false;
        }
        upstream.dispose();
        return true;
    }

    @Override
    public void close() {
        dispose();
    }
}
