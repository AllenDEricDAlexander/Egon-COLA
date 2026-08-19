package top.egon.cola.component.gateway.engine.http.websocket.service;

import top.egon.cola.component.gateway.engine.common.transport.service.GatewayWebSocketIdleTimeoutException;

import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketCloseStatus;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrame;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrameType;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketProxyContext;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayCommitPoint;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dedicated two-phase WebSocket proxy; it performs no route or provider choice.
 * 补充说明 / Supplementary summary: {@code GatewayWebSocketProxy} 是类型，位于当前 Gateway 模块的相关包中，负责网关WebSocket代理相关的职责与边界。
 * English supplement: {@code GatewayWebSocketProxy} is a type in the current Gateway module; it owns the gateway web socket proxy-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayWebSocketProxy {

    /**
     * 中文说明：保存 upstreamAdapter 对应的状态、依赖或配置值；字段类型为 {@code WebSocketUpstreamAdapter}，由 {@code GatewayWebSocketProxy} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by upstream adapter; its type is {@code WebSocketUpstreamAdapter}, and {@code GatewayWebSocketProxy} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxy}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final WebSocketUpstreamAdapter upstreamAdapter;

    /**
     * 中文说明：创建 {@code GatewayWebSocketProxy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayWebSocketProxy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     */
    public GatewayWebSocketProxy(
            WebSocketUpstreamAdapter upstreamAdapter) {
        this.upstreamAdapter = Objects.requireNonNull(
                upstreamAdapter,
                "upstreamAdapter"
        );
    }

    /**
     * 中文说明：执行 prepare 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prepare operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.prepare(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 prepare 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 bridge 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bridge operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.bridge(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param upstream 参数 upstream；parameter upstream。
     * @param downstream 参数 downstream；parameter downstream。
     * @return 返回 bridge 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 forward 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the forward operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.forward(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param target 参数 target；parameter target。
     * @param direction 参数 direction；parameter direction。
     * @param context 参数 context；parameter context。
     * @param termination 参数 termination；parameter termination。
     * @param maxFrameBytes 参数 maxFrameBytes；parameter max frame bytes。
     * @param idleTimeout 参数 idle超时；parameter idle timeout。
     * @param activity 参数 activity；parameter activity。
     * @param activitySequence 参数 activitySequence；parameter activity sequence。
     * @return 返回 forward 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 idleDeadline 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the idle deadline operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.idleDeadline(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activity 参数 activity；parameter activity。
     * @param idleTimeout 参数 idle超时；parameter idle timeout。
     * @return 返回 idleDeadline 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 process 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the process operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.process(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param frame 参数 frame；parameter frame。
     * @param source 参数 source；parameter source。
     * @param target 参数 target；parameter target。
     * @param direction 参数 direction；parameter direction。
     * @param context 参数 context；parameter context。
     * @param termination 参数 termination；parameter termination。
     * @param maxFrameBytes 参数 maxFrameBytes；parameter max frame bytes。
     * @return 返回 process 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 closeBoth 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close both operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.closeBoth(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param first 参数 first；parameter first。
     * @param second 参数 second；parameter second。
     * @param termination 参数 termination；parameter termination。
     * @param status 参数 status；parameter status。
     * @param reason 参数 reason；parameter reason。
     * @return 返回 closeBoth 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 terminal 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the terminal operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.terminal(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param frame 参数 frame；parameter frame。
     * @param maxFrameBytes 参数 maxFrameBytes；parameter max frame bytes。
     * @return 返回 terminal 的处理结果；returns the result of the operation.
     */
    private boolean terminal(
            GatewayWebSocketFrame frame,
            long maxFrameBytes) {
        return frame.type() == GatewayWebSocketFrameType.CLOSE
                || frame.payloadBytesCount() > maxFrameBytes;
    }

    /**
     * 中文说明：执行 finish 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the finish operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.finish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param signal 参数 signal；parameter signal。
     * @param upstream 参数 upstream；parameter upstream。
     * @param downstream 参数 downstream；parameter downstream。
     * @param context 参数 context；parameter context。
     * @param termination 参数 termination；parameter termination。
     */
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

    /**
     * 中文说明：执行 finishOnce 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the finish once operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.finishOnce(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param signal 参数 signal；parameter signal。
     * @param upstream 参数 upstream；parameter upstream。
     * @param downstream 参数 downstream；parameter downstream。
     * @param context 参数 context；parameter context。
     * @param termination 参数 termination；parameter termination。
     * @param cleaned 参数 cleaned；parameter cleaned。
     */
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

    /**
     * 中文说明：执行 observe 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observe operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.observe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @param reason 参数 reason；parameter reason。
     */
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

    /**
     * 中文说明：执行 observeFrame 操作；该方法是 {@code GatewayWebSocketProxy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observe frame operation; this method is the invocation entry point on {@code GatewayWebSocketProxy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.observeFrame(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @param direction 参数 direction；parameter direction。
     * @param frame 参数 frame；parameter frame。
     */
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

    /**
     * 中文说明：{@code Termination} 是类型，位于当前 Gateway 模块的相关包中，负责Termination相关的职责与边界。
     * English summary: {@code Termination} is a type in the current Gateway module; it owns the termination-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class Termination {

        /**
         * 中文说明：保存 terminated 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayWebSocketProxy.Termination} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by terminated; its type is {@code AtomicBoolean}, and {@code GatewayWebSocketProxy.Termination} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxy.Termination} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxy.Termination}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean terminated = new AtomicBoolean();

        /**
         * 中文说明：保存 reason 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<String>}，由 {@code GatewayWebSocketProxy.Termination} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by reason; its type is {@code AtomicReference<String>}, and {@code GatewayWebSocketProxy.Termination} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxy.Termination} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxy.Termination}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicReference<String> reason =
                new AtomicReference<>("UNKNOWN");

        /**
         * 中文说明：保存 补全 对应的状态、依赖或配置值；字段类型为 {@code Sinks.Empty<Void>}，由 {@code GatewayWebSocketProxy.Termination} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by completion; its type is {@code Sinks.Empty<Void>}, and {@code GatewayWebSocketProxy.Termination} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxy.Termination} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxy.Termination}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Sinks.Empty<Void> completion = Sinks.empty();

        /**
         * 中文说明：执行 win 操作；该方法是 {@code GatewayWebSocketProxy.Termination} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the win operation; this method is the invocation entry point on {@code GatewayWebSocketProxy.Termination} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.Termination.win(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param value 参数 值；parameter value。
         * @return 返回 win 的处理结果；returns the result of the operation.
         */
        private boolean win(String value) {
            if (!terminated.compareAndSet(false, true)) {
                return false;
            }
            reason.set(value);
            return true;
        }

        /**
         * 中文说明：执行 terminated 操作；该方法是 {@code GatewayWebSocketProxy.Termination} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the terminated operation; this method is the invocation entry point on {@code GatewayWebSocketProxy.Termination} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.Termination.terminated(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 terminated 的处理结果；returns the result of the operation.
         */
        private boolean terminated() {
            return terminated.get();
        }

        /**
         * 中文说明：执行 reason 操作；该方法是 {@code GatewayWebSocketProxy.Termination} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the reason operation; this method is the invocation entry point on {@code GatewayWebSocketProxy.Termination} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.Termination.reason(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 reason 的处理结果；returns the result of the operation.
         */
        private String reason() {
            return reason.get();
        }

        /**
         * 中文说明：执行 finish 操作；该方法是 {@code GatewayWebSocketProxy.Termination} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the finish operation; this method is the invocation entry point on {@code GatewayWebSocketProxy.Termination} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.Termination.finish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param action 参数 action；parameter action。
         * @return 返回 finish 的处理结果；returns the result of the operation.
         */
        private Mono<Void> finish(Mono<Void> action) {
            return action.onErrorResume(failure -> Mono.empty())
                    .doFinally(ignored -> completion.tryEmitEmpty());
        }

        /**
         * 中文说明：执行 await 操作；该方法是 {@code GatewayWebSocketProxy.Termination} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the await operation; this method is the invocation entry point on {@code GatewayWebSocketProxy.Termination} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxy.Termination.await(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 await 的处理结果；returns the result of the operation.
         */
        private Mono<Void> await() {
            return completion.asMono();
        }
    }
}
