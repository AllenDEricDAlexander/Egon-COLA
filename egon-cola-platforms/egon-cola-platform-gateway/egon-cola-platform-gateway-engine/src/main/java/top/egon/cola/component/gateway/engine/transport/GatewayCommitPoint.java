package top.egon.cola.component.gateway.engine.transport;

/**
 * Immutable names for externally observable transport commit facts.
 */
public enum GatewayCommitPoint {
    NEW(Flow.BOTH, 0),
    REQUEST_STREAMING(Flow.HTTP, 1),
    UPSTREAM_HEADERS_RECEIVED(Flow.HTTP, 2),
    DOWNSTREAM_HEADERS_COMMITTED(Flow.HTTP, 3),
    FIRST_BODY_BUFFER_SENT(Flow.HTTP, 4),
    UPSTREAM_HANDSHAKE_RECEIVED(Flow.WEBSOCKET, 1),
    CLIENT_HANDSHAKE_COMMITTED(Flow.WEBSOCKET, 2),
    FIRST_FRAME_FORWARDED(Flow.WEBSOCKET, 3),
    TERMINATED(Flow.BOTH, Integer.MAX_VALUE);

    private final Flow flow;

    private final int rank;

    GatewayCommitPoint(Flow flow, int rank) {
        this.flow = flow;
        this.rank = rank;
    }

    boolean supports(Flow expected) {
        return flow == Flow.BOTH || flow == expected;
    }

    int rank() {
        return rank;
    }

    enum Flow {
        BOTH,
        HTTP,
        WEBSOCKET
    }
}
