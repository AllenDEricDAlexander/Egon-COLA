package top.egon.cola.component.gateway.engine.transport;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Request-local monotonic state machine for HTTP or WebSocket commit facts.
 */
public final class GatewayCommitGuard {

    private final GatewayCommitPoint.Flow flow;

    private final AtomicReference<State> state =
            new AtomicReference<>(new State(GatewayCommitPoint.NEW, 0));

    private GatewayCommitGuard(GatewayCommitPoint.Flow flow) {
        this.flow = flow;
    }

    public static GatewayCommitGuard http() {
        return new GatewayCommitGuard(GatewayCommitPoint.Flow.HTTP);
    }

    public static GatewayCommitGuard websocket() {
        return new GatewayCommitGuard(
                GatewayCommitPoint.Flow.WEBSOCKET
        );
    }

    public GatewayCommitPoint current() {
        return state.get().point();
    }

    public boolean advance(GatewayCommitPoint next) {
        Objects.requireNonNull(next, "next");
        if (!next.supports(flow)) {
            throw new IllegalArgumentException(
                    next + " is not valid for " + flow
            );
        }
        while (true) {
            State existing = state.get();
            if (existing.point() == GatewayCommitPoint.TERMINATED
                    || next != GatewayCommitPoint.TERMINATED
                    && next.rank() <= existing.rank()) {
                return false;
            }
            State updated = next == GatewayCommitPoint.TERMINATED
                    ? new State(next, existing.rank())
                    : new State(next, next.rank());
            if (state.compareAndSet(existing, updated)) {
                return true;
            }
        }
    }

    public boolean terminate() {
        return advance(GatewayCommitPoint.TERMINATED);
    }

    public boolean upstreamAccepted() {
        return hasReached(flow == GatewayCommitPoint.Flow.HTTP
                ? GatewayCommitPoint.UPSTREAM_HEADERS_RECEIVED
                : GatewayCommitPoint.UPSTREAM_HANDSHAKE_RECEIVED);
    }

    public boolean downstreamCommitted() {
        return hasReached(flow == GatewayCommitPoint.Flow.HTTP
                ? GatewayCommitPoint.DOWNSTREAM_HEADERS_COMMITTED
                : GatewayCommitPoint.CLIENT_HANDSHAKE_COMMITTED);
    }

    public boolean payloadCommitted() {
        return hasReached(flow == GatewayCommitPoint.Flow.HTTP
                ? GatewayCommitPoint.FIRST_BODY_BUFFER_SENT
                : GatewayCommitPoint.FIRST_FRAME_FORWARDED);
    }

    public boolean terminated() {
        return state.get().point() == GatewayCommitPoint.TERMINATED;
    }

    private boolean hasReached(GatewayCommitPoint point) {
        return state.get().rank() >= point.rank();
    }

    private record State(GatewayCommitPoint point, int rank) {
    }
}
