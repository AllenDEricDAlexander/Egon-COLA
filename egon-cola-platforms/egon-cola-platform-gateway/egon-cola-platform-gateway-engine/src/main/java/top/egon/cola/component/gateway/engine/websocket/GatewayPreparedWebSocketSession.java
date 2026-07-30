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

    private final Runnable disposeAction;

    private final AtomicBoolean disposed = new AtomicBoolean();

    public GatewayPreparedWebSocketSession(
            GatewayWebSocketProxyContext context,
            GatewayWebSocketPeer upstream,
            String selectedSubprotocol) {
        this(context, upstream, selectedSubprotocol, () -> {
        });
    }

    private GatewayPreparedWebSocketSession(
            GatewayWebSocketProxyContext context,
            GatewayWebSocketPeer upstream,
            String selectedSubprotocol,
            Runnable disposeAction) {
        this.context = Objects.requireNonNull(context, "context");
        this.upstream = Objects.requireNonNull(upstream, "upstream");
        this.selectedSubprotocol = selectedSubprotocol == null
                || selectedSubprotocol.isBlank()
                ? null
                : selectedSubprotocol;
        this.disposeAction = Objects.requireNonNull(
                disposeAction,
                "disposeAction"
        );
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
        try {
            upstream.dispose();
        } finally {
            disposeAction.run();
        }
        return true;
    }

    public GatewayPreparedWebSocketSession onDispose(Runnable action) {
        Objects.requireNonNull(action, "action");
        return new GatewayPreparedWebSocketSession(
                context,
                upstream,
                selectedSubprotocol,
                () -> {
                    try {
                        dispose();
                    } finally {
                        action.run();
                    }
                }
        );
    }

    @Override
    public void close() {
        dispose();
    }
}
