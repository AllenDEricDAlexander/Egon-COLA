package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.http.domain.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.http.domain.GatewayInboundHttpRequest;
import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketCloseStatus;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketPeer;

/**
 * 中文说明：{@code GatewayHttpServer} 是类型，位于当前 Gateway 模块的相关包中，负责网关Http服务器相关的职责与边界。
 * English summary: {@code GatewayHttpServer} is a type in the current Gateway module; it owns the gateway http server-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayHttpServer implements AutoCloseable {

    /**
     * 中文说明：保存 public监听器 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpListener}，由 {@code GatewayHttpServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by public listener; its type is {@code GatewayHttpListener}, and {@code GatewayHttpServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpListener publicListener;

    /**
     * 中文说明：保存 internal监听器 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpListener}，由 {@code GatewayHttpServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by internal listener; its type is {@code GatewayHttpListener}, and {@code GatewayHttpServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpListener internalListener;

    /**
     * 中文说明：保存 drain超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayHttpServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drain timeout; its type is {@code Duration}, and {@code GatewayHttpServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration drainTimeout;

    /**
     * 中文说明：保存 accepting 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayHttpServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by accepting; its type is {@code AtomicBoolean}, and {@code GatewayHttpServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean accepting = new AtomicBoolean();

    /**
     * 中文说明：保存 drain监控器 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code GatewayHttpServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drain monitor; its type is {@code Object}, and {@code GatewayHttpServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Object drainMonitor = new Object();

    /**
     * 中文说明：保存 inFlightRequests 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayHttpServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by in flight requests; its type is {@code long}, and {@code GatewayHttpServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private long inFlightRequests;

    /**
     * 中文说明：保存 activeRequests 对应的状态、依赖或配置值；字段类型为 {@code Set<ActiveRequest>}，由 {@code GatewayHttpServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by active requests; its type is {@code Set<ActiveRequest>}, and {@code GatewayHttpServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Set<ActiveRequest> activeRequests =
            ConcurrentHashMap.newKeySet();

    /**
     * 中文说明：保存 webSockets 对应的状态、依赖或配置值；字段类型为 {@code ConcurrentHashMap<GatewayPreparedWebSocketSession, ActiveRequest>}，由 {@code GatewayHttpServer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by web sockets; its type is {@code ConcurrentHashMap<GatewayPreparedWebSocketSession, ActiveRequest>}, and {@code GatewayHttpServer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpServer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ConcurrentHashMap<GatewayPreparedWebSocketSession,
            ActiveRequest> webSockets = new ConcurrentHashMap<>();

    /**
     * 中文说明：创建 {@code GatewayHttpServer} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayHttpServer} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param properties 参数 properties；parameter properties。
     * @param handler 参数 处理器；parameter handler。
     */
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

    /**
     * 中文说明：执行 start 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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

    /**
     * 中文说明：执行 beginDrain 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the begin drain operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.beginDrain(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public void beginDrain() {
        synchronized (drainMonitor) {
            accepting.set(false);
            if (inFlightRequests == 0) {
                drainMonitor.notifyAll();
            }
        }
    }

    /**
     * 中文说明：执行 awaitDrain 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the await drain operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.awaitDrain(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 awaitDrain 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 accepting 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the accepting operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.accepting(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 accepting 的处理结果；returns the result of the operation.
     */
    public boolean accepting() {
        return accepting.get();
    }

    /**
     * 中文说明：执行 publicPort 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the public port operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.publicPort(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 publicPort 的处理结果；returns the result of the operation.
     */
    public int publicPort() {
        return publicListener.port();
    }

    /**
     * 中文说明：执行 internalPort 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the internal port operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.internalPort(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 internalPort 的处理结果；returns the result of the operation.
     */
    public int internalPort() {
        return internalListener.port();
    }

    /**
     * 中文说明：执行 reload传输安全 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reload transport security operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.reloadTransportSecurity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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

    /**
     * 中文说明：执行 close 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void close() {
        beginDrain();
        publicListener.close();
        internalListener.close();
    }

    /**
     * 中文说明：执行 guarded 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the guarded operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.guarded(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param delegate 参数 delegate；parameter delegate。
     * @return 返回 guarded 的处理结果；returns the result of the operation.
     */
    private GatewayHttpDataPlaneHandler guarded(
            GatewayHttpDataPlaneHandler delegate) {
        Objects.requireNonNull(delegate, "handler");
        return new GatewayHttpDataPlaneHandler() {
            /**
             * 中文说明：执行 handle 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the handle operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param zone 参数 zone；parameter zone。
             * @param request 参数 请求；parameter request。
             * @return 返回 handle 的处理结果；returns the result of the operation.
             */
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

            /**
             * 中文说明：执行 prepareWebSocket 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the prepare web socket operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.prepareWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param zone 参数 zone；parameter zone。
             * @param request 参数 请求；parameter request。
             * @return 返回 prepareWebSocket 的处理结果；returns the result of the operation.
             */
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

            /**
             * 中文说明：执行 bridgeWebSocket 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the bridge web socket operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.bridgeWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param upstream 参数 upstream；parameter upstream。
             * @param downstream 参数 downstream；parameter downstream。
             * @return 返回 bridgeWebSocket 的处理结果；returns the result of the operation.
             */
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

    /**
     * 中文说明：执行 请求Started 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the request started operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.requestStarted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 请求Started 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 请求Completed 操作；该方法是 {@code GatewayHttpServer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the request completed operation; this method is the invocation entry point on {@code GatewayHttpServer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.requestCompleted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    private void requestCompleted() {
        synchronized (drainMonitor) {
            inFlightRequests--;
            if (inFlightRequests == 0) {
                drainMonitor.notifyAll();
            }
        }
    }

    /**
     * 中文说明：{@code ActiveRequest} 是类型，位于当前 Gateway 模块的相关包中，负责Active请求相关的职责与边界。
     * English summary: {@code ActiveRequest} is a type in the current Gateway module; it owns the active request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private final class ActiveRequest {

        /**
         * 中文说明：保存 completed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayHttpServer.ActiveRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by completed; its type is {@code AtomicBoolean}, and {@code GatewayHttpServer.ActiveRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpServer.ActiveRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer.ActiveRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean completed = new AtomicBoolean();

        /**
         * 中文说明：保存 forced 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayHttpServer.ActiveRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by forced; its type is {@code AtomicBoolean}, and {@code GatewayHttpServer.ActiveRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpServer.ActiveRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer.ActiveRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean forced = new AtomicBoolean();

        /**
         * 中文说明：保存 stop 对应的状态、依赖或配置值；字段类型为 {@code Sinks.Empty<Void>}，由 {@code GatewayHttpServer.ActiveRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by stop; its type is {@code Sinks.Empty<Void>}, and {@code GatewayHttpServer.ActiveRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpServer.ActiveRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer.ActiveRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Sinks.Empty<Void> stop = Sinks.empty();

        /**
         * 中文说明：保存 forceAction 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<Runnable>}，由 {@code GatewayHttpServer.ActiveRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by force action; its type is {@code AtomicReference<Runnable>}, and {@code GatewayHttpServer.ActiveRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpServer.ActiveRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpServer.ActiveRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicReference<Runnable> forceAction =
                new AtomicReference<>(() -> {
                });

        /**
         * 中文说明：执行 stop 操作；该方法是 {@code GatewayHttpServer.ActiveRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the stop operation; this method is the invocation entry point on {@code GatewayHttpServer.ActiveRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.ActiveRequest.stop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 stop 的处理结果；returns the result of the operation.
         */
        private Mono<Void> stop() {
            return stop.asMono();
        }

        /**
         * 中文说明：执行 onForce 操作；该方法是 {@code GatewayHttpServer.ActiveRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the on force operation; this method is the invocation entry point on {@code GatewayHttpServer.ActiveRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.ActiveRequest.onForce(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param action 参数 action；parameter action。
         */
        private void onForce(Runnable action) {
            forceAction.set(Objects.requireNonNull(action, "action"));
            if (forced.get()) {
                action.run();
            }
        }

        /**
         * 中文说明：执行 force 操作；该方法是 {@code GatewayHttpServer.ActiveRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the force operation; this method is the invocation entry point on {@code GatewayHttpServer.ActiveRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.ActiveRequest.force(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void force() {
            if (forced.compareAndSet(false, true)) {
                stop.tryEmitEmpty();
                forceAction.get().run();
            }
        }

        /**
         * 中文说明：执行 complete 操作；该方法是 {@code GatewayHttpServer.ActiveRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the complete operation; this method is the invocation entry point on {@code GatewayHttpServer.ActiveRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpServer.ActiveRequest.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void complete() {
            if (completed.compareAndSet(false, true)) {
                activeRequests.remove(this);
                requestCompleted();
            }
        }
    }
}
