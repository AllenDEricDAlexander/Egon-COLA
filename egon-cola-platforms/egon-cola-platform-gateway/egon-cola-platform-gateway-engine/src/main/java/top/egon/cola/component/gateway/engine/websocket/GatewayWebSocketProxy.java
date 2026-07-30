package top.egon.cola.component.gateway.engine.websocket;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import top.egon.cola.component.gateway.engine.transport.GatewayCommitPoint;
import top.egon.cola.component.gateway.engine.transport.GatewayWebSocketIdleTimeoutException;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dedicated two-phase WebSocket proxy; it performs no route or provider choice.
 */
public final class GatewayWebSocketProxy {

    private final WebSocketUpstreamAdapter upstreamAdapter;

    public GatewayWebSocketProxy(
            WebSocketUpstreamAdapter upstreamAdapter) {
        this.upstreamAdapter = Objects.requireNonNull(
                upstreamAdapter,
                "upstreamAdapter"
        );
    }

    public Mono<GatewayWebSocketHandshakeResult> prepare(
            GatewayWebSocketProxyContext context) {
        Objects.requireNonNull(context, "context");
        return upstreamAdapter.prepare(context).map(result -> {
            if (!(result instanceof GatewayWebSocketHandshakeResult.Accepted
                    accepted)) {
                return result;
            }
            GatewayPreparedWebSocketSession session = accepted.session();
            if (!context.acceptsSubprotocol(
                    session.selectedSubprotocol()
            )) {
                session.dispose();
                return GatewayWebSocketHandshakeResult.rejected(
                        502,
                        "GATEWAY_WEBSOCKET_SUBPROTOCOL_MISMATCH",
                        "upstream selected an unoffered subprotocol"
                );
            }
            context.commitGuard().advance(
                    GatewayCommitPoint.UPSTREAM_HANDSHAKE_RECEIVED
            );
            observe(context, "PREPARED");
            return result;
        });
    }

    public Mono<Void> bridge(
            GatewayPreparedWebSocketSession upstream,
            GatewayWebSocketPeer downstream) {
        Objects.requireNonNull(upstream, "upstream");
        Objects.requireNonNull(downstream, "downstream");
        GatewayWebSocketProxyContext context = upstream.context();
        context.commitGuard().advance(
                GatewayCommitPoint.CLIENT_HANDSHAKE_COMMITTED
        );
        observe(context, "BRIDGING");
        Termination termination = new Termination();
        AtomicBoolean cleaned = new AtomicBoolean();
        Sinks.Many<Long> activity = Sinks.many()
                .multicast()
                .directBestEffort();
        AtomicLong activitySequence = new AtomicLong();
        long maxFrameBytes = context.policy()
                .websocketMaxFrameBytes()
                .orElseThrow();
        Duration idleTimeout = context.policy()
                .websocketIdleTimeout()
                .orElseThrow();
        Mono<Void> upstreamToDownstream = forward(
                upstream.upstream(),
                downstream,
                "RESPONSE",
                context,
                termination,
                maxFrameBytes,
                idleTimeout,
                activity,
                activitySequence
        );
        Mono<Void> downstreamToUpstream = forward(
                downstream,
                upstream.upstream(),
                "REQUEST",
                context,
                termination,
                maxFrameBytes,
                idleTimeout,
                activity,
                activitySequence
        );
        return Mono.firstWithSignal(
                        upstreamToDownstream,
                        downstreamToUpstream
                )
                .doOnSuccess(ignored -> finishOnce(
                        SignalType.ON_COMPLETE,
                        upstream,
                        downstream,
                        context,
                        termination,
                        cleaned
                ))
                .doFinally(signal -> finishOnce(
                        signal,
                        upstream,
                        downstream,
                        context,
                        termination,
                        cleaned
                ));
    }

    private Mono<Void> forward(
            GatewayWebSocketPeer source,
            GatewayWebSocketPeer target,
            String direction,
            GatewayWebSocketProxyContext context,
            Termination termination,
            long maxFrameBytes,
            Duration idleTimeout,
            Sinks.Many<Long> activity,
            AtomicLong activitySequence) {
        Flux<GatewayWebSocketFrame> inbound = source.receive()
                .doOnNext(ignored -> activity.tryEmitNext(
                        activitySequence.incrementAndGet()
                ))
                .takeUntilOther(idleDeadline(activity, idleTimeout));
        return inbound.takeUntil(frame -> terminal(
                        frame,
                        maxFrameBytes
                ))
                .concatMap(frame -> process(
                        frame,
                        source,
                        target,
                        direction,
                        context,
                        termination,
                        maxFrameBytes
                ))
                .doOnDiscard(
                        GatewayWebSocketFrame.class,
                        GatewayWebSocketFrame::release
                )
                .then(Mono.defer(() -> {
                    if (termination.win("ABNORMAL_CLOSURE")) {
                        return termination.finish(Mono.empty());
                    }
                    return termination.await();
                }))
                .onErrorResume(
                        GatewayWebSocketIdleTimeoutException.class,
                        failure -> closeBoth(
                                source,
                                target,
                                termination,
                                GatewayWebSocketCloseStatus.goingAway(),
                                "IDLE_TIMEOUT"
                        )
                )
                .onErrorResume(failure -> {
                    if (termination.win("ABNORMAL_CLOSURE")) {
                        return termination.finish(Mono.empty());
                    }
                    return termination.await();
                });
    }

    private Flux<Long> idleDeadline(
            Sinks.Many<Long> activity,
            Duration idleTimeout) {
        return activity.asFlux()
                .startWith(0L)
                .switchMap(ignored -> Mono.delay(idleTimeout)
                        .flatMap(value -> Mono.<Long>error(
                                new GatewayWebSocketIdleTimeoutException()
                        ))
                );
    }

    private Mono<Void> process(
            GatewayWebSocketFrame frame,
            GatewayWebSocketPeer source,
            GatewayWebSocketPeer target,
            String direction,
            GatewayWebSocketProxyContext context,
            Termination termination,
            long maxFrameBytes) {
        if (termination.terminated()) {
            frame.release();
            return termination.await();
        }
        observeFrame(context, direction, frame);
        if (frame.payloadBytesCount() > maxFrameBytes) {
            frame.release();
            if (!termination.win("FRAME_TOO_LARGE")) {
                return termination.await();
            }
            return termination.finish(source.sendClose(
                    GatewayWebSocketCloseStatus.frameTooLarge()
            ));
        }
        if (frame.type() == GatewayWebSocketFrameType.CLOSE) {
            GatewayWebSocketCloseStatus status = frame.closeStatus();
            frame.release();
            if (!termination.win("PEER_CLOSE")) {
                return termination.await();
            }
            Mono<Void> mirror = status == null || !status.sendable()
                    ? Mono.empty()
                    : target.sendClose(status);
            return termination.finish(mirror);
        }
        return target.send(Flux.just(frame))
                .doOnSuccess(ignored -> {
                    context.commitGuard().advance(
                            GatewayCommitPoint.FIRST_FRAME_FORWARDED
                    );
                    observe(context, "FRAME_FORWARDED");
                });
    }

    private Mono<Void> closeBoth(
            GatewayWebSocketPeer first,
            GatewayWebSocketPeer second,
            Termination termination,
            GatewayWebSocketCloseStatus status,
            String reason) {
        if (!termination.win(reason)) {
            return termination.await();
        }
        return termination.finish(Mono.whenDelayError(
                first.sendClose(status),
                second.sendClose(status)
        ));
    }

    private boolean terminal(
            GatewayWebSocketFrame frame,
            long maxFrameBytes) {
        return frame.type() == GatewayWebSocketFrameType.CLOSE
                || frame.payloadBytesCount() > maxFrameBytes;
    }

    private void finish(
            SignalType signal,
            GatewayPreparedWebSocketSession upstream,
            GatewayWebSocketPeer downstream,
            GatewayWebSocketProxyContext context,
            Termination termination) {
        if (!termination.terminated()) {
            termination.win(signal == SignalType.CANCEL
                    ? "CANCELLED"
                    : "ABNORMAL_CLOSURE");
        }
        context.commitGuard().terminate();
        observe(context, termination.reason());
        upstream.dispose();
        downstream.dispose();
    }

    private void finishOnce(
            SignalType signal,
            GatewayPreparedWebSocketSession upstream,
            GatewayWebSocketPeer downstream,
            GatewayWebSocketProxyContext context,
            Termination termination,
            AtomicBoolean cleaned) {
        if (cleaned.compareAndSet(false, true)) {
            finish(signal, upstream, downstream, context, termination);
        }
    }

    private static void observe(
            GatewayWebSocketProxyContext context,
            String reason) {
        try {
            context.observer().observe(
                    "WEBSOCKET",
                    context.commitGuard().current().name(),
                    reason
            );
        } catch (RuntimeException ignored) {
            // Observation is passive and cannot alter transport behavior.
        }
    }

    private static void observeFrame(
            GatewayWebSocketProxyContext context,
            String direction,
            GatewayWebSocketFrame frame) {
        if (!context.policy().bodyLogEnabled()) {
            return;
        }
        try {
            context.observer().observeFrame(
                    direction,
                    frame.type(),
                    frame.payloadBytesCount(),
                    frame.finalFragment()
            );
        } catch (RuntimeException ignored) {
            // Observation is passive and cannot alter transport behavior.
        }
    }

    private static final class Termination {

        private final AtomicBoolean terminated = new AtomicBoolean();

        private final AtomicReference<String> reason =
                new AtomicReference<>("UNKNOWN");

        private final Sinks.Empty<Void> completion = Sinks.empty();

        private boolean win(String value) {
            if (!terminated.compareAndSet(false, true)) {
                return false;
            }
            reason.set(value);
            return true;
        }

        private boolean terminated() {
            return terminated.get();
        }

        private String reason() {
            return reason.get();
        }

        private Mono<Void> finish(Mono<Void> action) {
            return action.onErrorResume(failure -> Mono.empty())
                    .doFinally(ignored -> completion.tryEmitEmpty());
        }

        private Mono<Void> await() {
            return completion.asMono();
        }
    }
}
