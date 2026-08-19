package top.egon.cola.component.gateway.engine.http.service;

import top.egon.cola.component.gateway.engine.common.traffic.service.GatewayTrafficContext;

import top.egon.cola.component.gateway.engine.common.provider.service.ProviderCallOutcomeRecorder;
import top.egon.cola.component.gateway.engine.http.adapter.HttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.http.cors.GatewayCorsException;
import top.egon.cola.component.gateway.engine.http.cors.GatewayCorsProcessor;
import top.egon.cola.component.gateway.engine.http.domain.GatewayInboundHttpRequest;
import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.domain.GatewayRequestBodyTooLargeException;
import top.egon.cola.component.gateway.engine.http.domain.GatewayResponseBodyTooLargeException;
import top.egon.cola.component.gateway.engine.http.security.GatewayHttpSecurityProcessor;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;
import top.egon.cola.component.gateway.core.context.GatewayContext;
import top.egon.cola.component.gateway.core.context.GatewayStage;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.filter.GatewayFilterChain;
import top.egon.cola.component.gateway.core.http.GatewayRequestRejectedException;
import top.egon.cola.component.gateway.core.http.HttpRequestNormalizer;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.route.CompiledHttpRouteIndex;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.http.cors.RuntimeCorsPolicy;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderCallOutcome;
import top.egon.cola.component.gateway.engine.common.provider.service.ProviderSelector;
import top.egon.cola.component.gateway.engine.http.common.buffer.GatewayDataBufferOwnership;
import top.egon.cola.component.gateway.engine.http.common.logging.GatewayBodyLogEvent;
import top.egon.cola.component.gateway.engine.http.common.logging.GatewayBodyLogTap;
import top.egon.cola.component.gateway.engine.http.proxy.service.AggregatedHttpProxyStrategy;
import top.egon.cola.component.gateway.engine.http.proxy.service.GatewayHttpAttemptCoordinator;
import top.egon.cola.component.gateway.engine.http.proxy.domain.GatewayHttpProxyContext;
import top.egon.cola.component.gateway.engine.http.proxy.service.GatewayHttpProxyStrategySelector;
import top.egon.cola.component.gateway.engine.http.proxy.service.StreamingHttpProxyStrategy;
import top.egon.cola.component.gateway.engine.http.common.logging.GatewayCallAccessLogger;
import top.egon.cola.component.gateway.engine.common.observability.service.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.common.observability.domain.GatewayCallObservation;
import top.egon.cola.component.gateway.engine.common.observability.domain.GatewayTelemetry;
import top.egon.cola.component.gateway.engine.operation.adapter.HttpRpcUpstreamAdapter;
import top.egon.cola.component.gateway.engine.common.security.domain.GatewaySecurityException;
import top.egon.cola.component.gateway.engine.common.security.service.TrustedIdentitySanitizer;
import top.egon.cola.component.gateway.engine.common.traffic.service.GatewayRequestResourceGuard;
import top.egon.cola.component.gateway.engine.common.traffic.domain.GatewayResourceLimits;
import top.egon.cola.component.gateway.engine.rule.service.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.common.traffic.domain.GatewayTrafficRejectedException;
import top.egon.cola.component.gateway.engine.common.traffic.domain.ProviderCallClassification;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayCommitGuard;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayCommitPoint;
import top.egon.cola.component.gateway.engine.http.service.GatewayTransportDispatcher;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayPreparedWebSocketSession;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketFrameType;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketHandshakeResult;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketObserver;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketPeer;
import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketProxy;
import top.egon.cola.component.gateway.engine.http.websocket.domain.GatewayWebSocketProxyContext;
import top.egon.cola.component.gateway.engine.http.websocket.adapter.ReactorNettyWebSocketUpstreamAdapter;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 中文说明：{@code DefaultGatewayHttpDataPlaneHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责Default网关HttpDataPlane处理器相关的职责与边界。
 * English summary: {@code DefaultGatewayHttpDataPlaneHandler} is a default gateway http data plane handler handler in the current Gateway module; it owns the default gateway http data plane handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class DefaultGatewayHttpDataPlaneHandler
        implements GatewayHttpDataPlaneHandler {

    /**
     * 中文说明：表示 缓冲区工厂 这一固定值；它属于 {@code DefaultGatewayHttpDataPlaneHandler} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value buffer factory; it is a state, type, or protocol value of {@code DefaultGatewayHttpDataPlaneHandler} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final DefaultDataBufferFactory BUFFER_FACTORY =
            DefaultDataBufferFactory.sharedInstance;

    /**
     * 中文说明：保存 normalizer 对应的状态、依赖或配置值；字段类型为 {@code HttpRequestNormalizer}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by normalizer; its type is {@code HttpRequestNormalizer}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final HttpRequestNormalizer normalizer;

    /**
     * 中文说明：保存 路由索引 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledHttpRouteIndex>}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by route index; its type is {@code Supplier<CompiledHttpRouteIndex>}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledHttpRouteIndex> routeIndex;

    /**
     * 中文说明：保存 提供方Selector 对应的状态、依赖或配置值；字段类型为 {@code ProviderSelector}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by provider selector; its type is {@code ProviderSelector}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderSelector providerSelector;

    /**
     * 中文说明：保存 upstreamAdapter 对应的状态、依赖或配置值；字段类型为 {@code HttpUpstreamAdapter}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by upstream adapter; its type is {@code HttpUpstreamAdapter}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final HttpUpstreamAdapter upstreamAdapter;

    /**
     * 中文说明：保存 maxBodyBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by max body bytes; its type is {@code long}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final long maxBodyBytes;

    /**
     * 中文说明：保存 max响应Bytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by max response bytes; its type is {@code long}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final long maxResponseBytes = 4 * 1024 * 1024;

    /**
     * 中文说明：保存 upstream超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by upstream timeout; its type is {@code Duration}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration upstreamTimeout;

    /**
     * 中文说明：保存 资源Guard 对应的状态、依赖或配置值；字段类型为 {@code GatewayRequestResourceGuard}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by resource guard; its type is {@code GatewayRequestResourceGuard}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRequestResourceGuard resourceGuard;

    /**
     * 中文说明：保存 安全Processor 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpSecurityProcessor}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security processor; its type is {@code GatewayHttpSecurityProcessor}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpSecurityProcessor securityProcessor;

    /**
     * 中文说明：保存 补全监听器 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallCompletionListener}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by completion listener; its type is {@code GatewayCallCompletionListener}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCallCompletionListener completionListener;

    /**
     * 中文说明：保存 遥测 对应的状态、依赖或配置值；字段类型为 {@code GatewayTelemetry}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by telemetry; its type is {@code GatewayTelemetry}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTelemetry telemetry;

    /**
     * 中文说明：保存 流量Governance 对应的状态、依赖或配置值；字段类型为 {@code GatewayTrafficGovernance}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by traffic governance; its type is {@code GatewayTrafficGovernance}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTrafficGovernance trafficGovernance;

    /**
     * 中文说明：保存 httpRpcUpstream 对应的状态、依赖或配置值；字段类型为 {@code HttpRpcUpstreamAdapter}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by http rpc upstream; its type is {@code HttpRpcUpstreamAdapter}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final HttpRpcUpstreamAdapter httpRpcUpstream;

    /**
     * 中文说明：保存 outcomeRecorder 对应的状态、依赖或配置值；字段类型为 {@code ProviderCallOutcomeRecorder}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by outcome recorder; its type is {@code ProviderCallOutcomeRecorder}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderCallOutcomeRecorder outcomeRecorder;

    /**
     * 中文说明：保存 attemptCoordinator 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpAttemptCoordinator}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by attempt coordinator; its type is {@code GatewayHttpAttemptCoordinator}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpAttemptCoordinator attemptCoordinator =
            new GatewayHttpAttemptCoordinator();

    /**
     * 中文说明：保存 传输分发器 对应的状态、依赖或配置值；字段类型为 {@code GatewayTransportDispatcher}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transport dispatcher; its type is {@code GatewayTransportDispatcher}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTransportDispatcher transportDispatcher;

    /**
     * 中文说明：保存 bodyLogSampleBytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by body log sample bytes; its type is {@code int}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int bodyLogSampleBytes;

    /**
     * 中文说明：保存 bodyLogObserver 对应的状态、依赖或配置值；字段类型为 {@code Consumer<GatewayBodyLogEvent>}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by body log observer; its type is {@code Consumer<GatewayBodyLogEvent>}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Consumer<GatewayBodyLogEvent> bodyLogObserver;

    /**
     * 中文说明：保存 accessLogger 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallAccessLogger}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by access logger; its type is {@code GatewayCallAccessLogger}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCallAccessLogger accessLogger =
            new GatewayCallAccessLogger();

    /**
     * 中文说明：保存 引擎NodeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by engine node id; its type is {@code String}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String engineNodeId;

    /**
     * 中文说明：保存 引擎Env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by engine env; its type is {@code String}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String engineEnv;

    /**
     * 中文说明：保存 引擎命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by engine namespace; its type is {@code String}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String engineNamespace;

    /**
     * 中文说明：保存 身份Sanitizer 对应的状态、依赖或配置值；字段类型为 {@code TrustedIdentitySanitizer}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by identity sanitizer; its type is {@code TrustedIdentitySanitizer}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final TrustedIdentitySanitizer identitySanitizer =
            new TrustedIdentitySanitizer();

    /**
     * 中文说明：保存 bodySizeLimiter 对应的状态、依赖或配置值；字段类型为 {@code GatewayBodySizeLimiter}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by body size limiter; its type is {@code GatewayBodySizeLimiter}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayBodySizeLimiter bodySizeLimiter =
            new GatewayBodySizeLimiter();

    /**
     * 中文说明：保存 executionPipeline 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpExecutionPipeline}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by execution pipeline; its type is {@code GatewayHttpExecutionPipeline}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHttpExecutionPipeline executionPipeline =
            new GatewayHttpExecutionPipeline();

    /**
     * 中文说明：保存 corsProcessor 对应的状态、依赖或配置值；字段类型为 {@code GatewayCorsProcessor}，由 {@code DefaultGatewayHttpDataPlaneHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by cors processor; its type is {@code GatewayCorsProcessor}, and {@code DefaultGatewayHttpDataPlaneHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCorsProcessor corsProcessor;

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                (zone, request, normalized, route, traceId) ->
                        Mono.just(
                                GatewayHttpSecurityProcessor.Outcome.anonymous()
                ),
                GatewayCallCompletionListener.noop(),
                "unknown-engine",
                GatewayTrafficGovernance.noop()
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                GatewayCallCompletionListener.noop(),
                "unknown-engine",
                GatewayTrafficGovernance.noop()
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                GatewayTrafficGovernance.noop(),
                null,
                ProviderCallOutcomeRecorder.noop()
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param engineEnv 参数 引擎Env；parameter engine env。
     * @param engineNamespace 参数 引擎命名空间；parameter engine namespace。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            String engineEnv,
            String engineNamespace) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                GatewayTrafficGovernance.noop(),
                null,
                ProviderCallOutcomeRecorder.noop(),
                Map::of,
                GatewayTelemetry.noop(),
                engineEnv,
                engineNamespace
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                null,
                ProviderCallOutcomeRecorder.noop()
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param httpRpcUpstream 参数 httpRpcUpstream；parameter http rpc upstream。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                httpRpcUpstream,
                ProviderCallOutcomeRecorder.noop()
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param httpRpcUpstream 参数 httpRpcUpstream；parameter http rpc upstream。
     * @param outcomeRecorder 参数 outcomeRecorder；parameter outcome recorder。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream,
            ProviderCallOutcomeRecorder outcomeRecorder) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                httpRpcUpstream,
                outcomeRecorder,
                Map::of
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param httpRpcUpstream 参数 httpRpcUpstream；parameter http rpc upstream。
     * @param outcomeRecorder 参数 outcomeRecorder；parameter outcome recorder。
     * @param corsPolicies 参数 corsPolicies；parameter cors policies。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream,
            ProviderCallOutcomeRecorder outcomeRecorder,
            Supplier<Map<String, RuntimeCorsPolicy>> corsPolicies) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                httpRpcUpstream,
                outcomeRecorder,
                corsPolicies,
                GatewayTelemetry.noop()
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param httpRpcUpstream 参数 httpRpcUpstream；parameter http rpc upstream。
     * @param outcomeRecorder 参数 outcomeRecorder；parameter outcome recorder。
     * @param corsPolicies 参数 corsPolicies；parameter cors policies。
     * @param telemetry 参数 遥测；parameter telemetry。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream,
            ProviderCallOutcomeRecorder outcomeRecorder,
            Supplier<Map<String, RuntimeCorsPolicy>> corsPolicies,
            GatewayTelemetry telemetry) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                httpRpcUpstream,
                outcomeRecorder,
                corsPolicies,
                telemetry,
                "",
                ""
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param httpRpcUpstream 参数 httpRpcUpstream；parameter http rpc upstream。
     * @param outcomeRecorder 参数 outcomeRecorder；parameter outcome recorder。
     * @param corsPolicies 参数 corsPolicies；parameter cors policies。
     * @param telemetry 参数 遥测；parameter telemetry。
     * @param engineEnv 参数 引擎Env；parameter engine env。
     * @param engineNamespace 参数 引擎命名空间；parameter engine namespace。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream,
            ProviderCallOutcomeRecorder outcomeRecorder,
            Supplier<Map<String, RuntimeCorsPolicy>> corsPolicies,
            GatewayTelemetry telemetry,
            String engineEnv,
            String engineNamespace) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                httpRpcUpstream,
                outcomeRecorder,
                corsPolicies,
                telemetry,
                engineEnv,
                engineNamespace,
                defaultTransportDispatcher()
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param httpRpcUpstream 参数 httpRpcUpstream；parameter http rpc upstream。
     * @param outcomeRecorder 参数 outcomeRecorder；parameter outcome recorder。
     * @param corsPolicies 参数 corsPolicies；parameter cors policies。
     * @param telemetry 参数 遥测；parameter telemetry。
     * @param engineEnv 参数 引擎Env；parameter engine env。
     * @param engineNamespace 参数 引擎命名空间；parameter engine namespace。
     * @param transportDispatcher 参数 传输分发器；parameter transport dispatcher。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream,
            ProviderCallOutcomeRecorder outcomeRecorder,
            Supplier<Map<String, RuntimeCorsPolicy>> corsPolicies,
            GatewayTelemetry telemetry,
            String engineEnv,
            String engineNamespace,
            GatewayTransportDispatcher transportDispatcher) {
        this(
                normalizer,
                routeIndex,
                providerSelector,
                upstreamAdapter,
                maxBodyBytes,
                upstreamTimeout,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                httpRpcUpstream,
                outcomeRecorder,
                corsPolicies,
                telemetry,
                engineEnv,
                engineNamespace,
                transportDispatcher,
                GatewayBodyLogTap.DEFAULT_SAMPLE_BYTES,
                null
        );
    }

    /**
     * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param normalizer 参数 normalizer；parameter normalizer。
     * @param routeIndex 参数 路由索引；parameter route index。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param upstreamAdapter 参数 upstreamAdapter；parameter upstream adapter。
     * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
     * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param httpRpcUpstream 参数 httpRpcUpstream；parameter http rpc upstream。
     * @param outcomeRecorder 参数 outcomeRecorder；parameter outcome recorder。
     * @param corsPolicies 参数 corsPolicies；parameter cors policies。
     * @param telemetry 参数 遥测；parameter telemetry。
     * @param engineEnv 参数 引擎Env；parameter engine env。
     * @param engineNamespace 参数 引擎命名空间；parameter engine namespace。
     * @param transportDispatcher 参数 传输分发器；parameter transport dispatcher。
     * @param bodyLogSampleBytes 参数 bodyLogSampleBytes；parameter body log sample bytes。
     * @param bodyLogObserver 参数 bodyLogObserver；parameter body log observer。
     */
    public DefaultGatewayHttpDataPlaneHandler(
            HttpRequestNormalizer normalizer,
            Supplier<CompiledHttpRouteIndex> routeIndex,
            ProviderSelector providerSelector,
            HttpUpstreamAdapter upstreamAdapter,
            long maxBodyBytes,
            Duration upstreamTimeout,
            GatewayHttpSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            HttpRpcUpstreamAdapter httpRpcUpstream,
            ProviderCallOutcomeRecorder outcomeRecorder,
            Supplier<Map<String, RuntimeCorsPolicy>> corsPolicies,
            GatewayTelemetry telemetry,
            String engineEnv,
            String engineNamespace,
            GatewayTransportDispatcher transportDispatcher,
            int bodyLogSampleBytes,
            Consumer<GatewayBodyLogEvent> bodyLogObserver) {
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer");
        this.routeIndex = Objects.requireNonNull(routeIndex, "routeIndex");
        this.providerSelector = Objects.requireNonNull(
                providerSelector,
                "providerSelector"
        );
        this.upstreamAdapter = Objects.requireNonNull(
                upstreamAdapter,
                "upstreamAdapter"
        );
        this.transportDispatcher = Objects.requireNonNull(
                transportDispatcher,
                "transportDispatcher"
        );
        if (bodyLogSampleBytes < 1
                || bodyLogSampleBytes > GatewayBodyLogTap.MAX_SAMPLE_BYTES) {
            throw new IllegalArgumentException(
                    "bodyLogSampleBytes must be between 1 and 64 KiB"
            );
        }
        this.bodyLogSampleBytes = bodyLogSampleBytes;
        this.bodyLogObserver = bodyLogObserver == null
                ? accessLogger::onBody
                : bodyLogObserver;
        this.maxBodyBytes = maxBodyBytes;
        this.upstreamTimeout = Objects.requireNonNull(
                upstreamTimeout,
                "upstreamTimeout"
        );
        this.securityProcessor = Objects.requireNonNull(
                securityProcessor,
                "securityProcessor"
        );
        this.completionListener = Objects.requireNonNull(
                completionListener,
                "completionListener"
        );
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.engineNodeId = Objects.requireNonNull(
                engineNodeId,
                "engineNodeId"
        );
        this.engineEnv = Objects.requireNonNull(engineEnv, "engineEnv");
        this.engineNamespace = Objects.requireNonNull(
                engineNamespace,
                "engineNamespace"
        );
        this.trafficGovernance = Objects.requireNonNull(
                trafficGovernance,
                "trafficGovernance"
        );
        this.httpRpcUpstream = httpRpcUpstream;
        this.outcomeRecorder = Objects.requireNonNull(
                outcomeRecorder,
                "outcomeRecorder"
        );
        corsProcessor = new GatewayCorsProcessor(
                Objects.requireNonNull(corsPolicies, "corsPolicies")
        );
        resourceGuard = new GatewayRequestResourceGuard(
                new GatewayResourceLimits(
                        128,
                        64,
                        64 * 1024,
                        maxBodyBytes,
                        4 * 1024 * 1024
                )
        );
    }

    /**
     * 中文说明：执行 default传输分发器 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the default transport dispatcher operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.defaultTransportDispatcher(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 default传输分发器 的处理结果；returns the result of the operation.
     */
    private static GatewayTransportDispatcher defaultTransportDispatcher() {
        return new GatewayTransportDispatcher(
                new GatewayHttpProxyStrategySelector(
                        new AggregatedHttpProxyStrategy(),
                        new StreamingHttpProxyStrategy()
                ),
                new GatewayWebSocketProxy(
                        new ReactorNettyWebSocketUpstreamAdapter(
                                HttpClient.create()
                        )
                )
        );
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param request 参数 请求；parameter request。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<GatewayOutboundHttpResponse> handle(
            AccessZone accessZone,
            GatewayInboundHttpRequest request) {
        GatewayTraceContext selectedTrace = traceContext(
                request.headers()
        );
        GatewayCallObservation observation = GatewayCallObservation.start(
                selectedTrace,
                "HTTP",
                accessZone.name(),
                engineNodeId,
                telemetry
        );
        observation.scope(engineEnv, engineNamespace);
        GatewayTraceContext trace = observation.trace();
        try {
            NormalizedHttpRequest normalized = normalizer.normalize(
                    request.method(),
                    request.host(),
                    request.uri(),
                    request.headers()
            );
            resourceGuard.validate(normalized);
            String routeMethod = routeMethod(request, normalized.method());
            HttpRouteMatch match = routeIndex.get().match(
                    normalized.host(),
                    routeMethod,
                    normalized.normalizedPath(),
                    accessZone
            ).orElse(null);
            if (match == null) {
                return Mono.just(observed(
                        error(
                                404,
                                "GATEWAY_ROUTE_NOT_FOUND",
                                trace.traceId()
                        ),
                        observation,
                        "ROUTE",
                        "REJECTED",
                        "GATEWAY_ROUTE_NOT_FOUND"
                ));
            }
            observation.route(
                    routeMethod,
                    match.route().pathPattern(),
                    match.route().gatewayGroupId(),
                    match.route().metadata().get("releaseId"),
                    match.route().operationId(),
                    match.route().routeId()
            );
            observation.scope(
                    match.route().upstream().env(),
                    match.route().upstream().namespace()
            );
            if (accessZone == AccessZone.PUBLIC
                    && !match.route().externalAccessible()) {
                return Mono.just(observed(
                        error(
                                404,
                                "GATEWAY_ROUTE_NOT_FOUND",
                                trace.traceId()
                        ),
                        observation,
                        "EXPOSURE",
                        "REJECTED",
                        "GATEWAY_ROUTE_NOT_FOUND"
                ));
            }
            HttpStageExchange exchange = new HttpStageExchange(
                    accessZone,
                    request,
                    normalized,
                    match,
                    routeMethod,
                    trace,
                    observation
            );
            return executionPipeline.execute(exchange)
                    .map(response -> exchange.failed()
                            ? response
                            : observed(
                            response,
                            observation,
                            "COMPLETE",
                            category(response.status()),
                            response.status() >= 400
                                    ? "GATEWAY_UPSTREAM_STATUS"
                                    : null
                    ))
                    .doOnCancel(() -> publish(
                            observation,
                            "CLIENT",
                            "CANCELLED",
                            "GATEWAY_CLIENT_CANCELLED",
                            null
                    ));
        } catch (GatewayRequestRejectedException rejected) {
            return Mono.just(observed(
                    error(
                            rejected.status(),
                            rejected.code(),
                            trace.traceId()
                    ),
                    observation,
                    "NORMALIZE",
                    "REJECTED",
                    rejected.code()
            ));
        } catch (RuntimeException error) {
            return Mono.just(observed(
                    error(
                            500,
                            "GATEWAY_INTERNAL_ERROR",
                            trace.traceId()
                    ),
                    observation,
                    "INTERNAL",
                    "ERROR",
                    "GATEWAY_INTERNAL_ERROR"
            ));
        }
    }

    /**
     * 中文说明：执行 prepareWebSocket 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prepare web socket operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.prepareWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param request 参数 请求；parameter request。
     * @return 返回 prepareWebSocket 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<GatewayWebSocketHandshakeResult> prepareWebSocket(
            AccessZone accessZone,
            GatewayInboundHttpRequest request) {
        GatewayTraceContext selectedTrace = traceContext(request.headers());
        GatewayCallObservation observation = GatewayCallObservation.start(
                selectedTrace,
                "WEBSOCKET",
                accessZone.name(),
                engineNodeId,
                telemetry
        );
        observation.scope(engineEnv, engineNamespace);
        GatewayTraceContext trace = observation.trace();
        try {
            NormalizedHttpRequest normalized = normalizer.normalize(
                    request.method(),
                    request.host(),
                    request.uri(),
                    request.headers()
            );
            resourceGuard.validate(normalized);
            HttpRouteMatch match = routeIndex.get().match(
                    normalized.host(),
                    normalized.method(),
                    normalized.normalizedPath(),
                    accessZone
            ).orElse(null);
            if (match == null || accessZone == AccessZone.PUBLIC
                    && !match.route().externalAccessible()) {
                return Mono.just(GatewayWebSocketHandshakeResult.rejected(
                        404,
                        "GATEWAY_ROUTE_NOT_FOUND",
                        "gateway route was not found"
                ));
            }
            observation.route(
                    normalized.method(),
                    match.route().pathPattern(),
                    match.route().gatewayGroupId(),
                    match.route().metadata().get("releaseId"),
                    match.route().operationId(),
                    match.route().routeId()
            );
            observation.scope(
                    match.route().upstream().env(),
                    match.route().upstream().namespace()
            );
            return executionPipeline.executeWebSocket(
                    new WebSocketStageExchange(
                            accessZone,
                            request,
                            normalized,
                            match,
                            trace,
                            observation
                    )
            ).doOnCancel(() -> publish(
                    observation,
                    "CLIENT",
                    "CANCELLED",
                    "GATEWAY_CLIENT_CANCELLED",
                    null
            ));
        } catch (GatewayRequestRejectedException rejected) {
            return Mono.just(GatewayWebSocketHandshakeResult.rejected(
                    rejected.status(),
                    rejected.code(),
                    "gateway WebSocket request was rejected"
            ));
        } catch (RuntimeException failure) {
            return Mono.just(GatewayWebSocketHandshakeResult.rejected(
                    500,
                    "GATEWAY_INTERNAL_ERROR",
                    "gateway WebSocket preparation failed"
            ));
        }
    }

    /**
     * 中文说明：执行 bridgeWebSocket 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bridge web socket operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.bridgeWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param upstream 参数 upstream；parameter upstream。
     * @param downstream 参数 downstream；parameter downstream。
     * @return 返回 bridgeWebSocket 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Void> bridgeWebSocket(
            GatewayPreparedWebSocketSession upstream,
            GatewayWebSocketPeer downstream) {
        return transportDispatcher.bridgeWebSocket(upstream, downstream);
    }

    /**
     * 中文说明：执行 路由方法 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the route method operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.routeMethod(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param method 参数 方法；parameter method。
     * @return 返回 路由方法 的处理结果；returns the result of the operation.
     */
    private String routeMethod(
            GatewayInboundHttpRequest request,
            String method) {
        if (!"OPTIONS".equalsIgnoreCase(method)) {
            return method;
        }
        String requested = firstHeader(
                request.headers(),
                "access-control-request-method"
        );
        return requested == null || requested.isBlank()
                ? method
                : requested.trim().toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * 中文说明：执行 invokeUpstream 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke upstream operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.invokeUpstream(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param match 参数 match；parameter match。
     * @param normalized 参数 normalized；parameter normalized。
     * @param request 参数 请求；parameter request。
     * @param security 参数 安全；parameter security。
     * @param trace 参数 trace；parameter trace。
     * @param observation 参数 观测；parameter observation。
     * @param permit 参数 permit；parameter permit。
     * @return 返回 invokeUpstream 的处理结果；returns the result of the operation.
     */
    private Mono<GatewayOutboundHttpResponse> invokeUpstream(
            HttpRouteMatch match,
            NormalizedHttpRequest normalized,
            GatewayInboundHttpRequest request,
            GatewayHttpSecurityProcessor.Outcome security,
            GatewayTraceContext trace,
            GatewayCallObservation observation,
            GatewayTrafficGovernance.RequestPermit permit) {
        long requestLimit = permit.requestSizeLimit(
                match.route().transportPolicy().maxRequestBodyBytes()
        );
        bodySizeLimiter.validateRequestHeaders(request.headers(), requestLimit);
        GatewayCommitGuard commitGuard = GatewayCommitGuard.http();
        observeTransport(
                observation,
                match,
                commitGuard,
                "STARTED"
        );
        boolean aggregate = match.route().upstream().protocolType()
                == ProviderProtocolType.RPC
                || match.route().transportPolicy().requestBodyMode()
                == GatewayRequestBodyMode.AGGREGATED;
        Mono<RequestBody> preparedBody = aggregate
                ? bodySizeLimiter.aggregateRequest(request.body(), requestLimit)
                .doOnNext(body -> observation.addRequestBytes(body.length))
                .map(body -> new RequestBody(
                        Flux.defer(() -> Flux.just(BUFFER_FACTORY.wrap(body))),
                        body,
                        true
                ))
                : Mono.just(new RequestBody(
                        request.body().doOnNext(buffer ->
                                observation.addRequestBytes(
                                        buffer.readableByteCount()
                                )
                        ),
                        null,
                        false
                ));
        return preparedBody.flatMap(body -> {
            AtomicInteger attempts = new AtomicInteger();
            Set<String> failedProviders = new LinkedHashSet<>();
            observation.governance(
                    "APPLIED",
                    permit.retryPolicy().enabled()
                            && match.route().transportPolicy().retryAllowed()
                            ? "RETRY_ENABLED"
                            : "RETRY_DISABLED",
                    "ALLOW"
            );
            return attemptCoordinator.execute(
                    match.route().transportPolicy(),
                    permit.retryPolicy(),
                    commitGuard,
                    idempotent(match, normalized),
                    body.replayable(),
                    permit.timeout(),
                    () -> invokeAttempt(
                            match,
                            normalized,
                            body,
                            security,
                            trace,
                            observation,
                            permit,
                            attempts.incrementAndGet(),
                            failedProviders,
                            commitGuard
                    ),
                    this::retryable,
                    RetryableHttpStatusException.class::isInstance
            );
        }).map(response -> {
            commitGuard.advance(
                    GatewayCommitPoint.DOWNSTREAM_HEADERS_COMMITTED
            );
            observeTransport(
                    observation,
                    match,
                    commitGuard,
                    "DOWNSTREAM_HEADERS"
            );
            AtomicBoolean firstBodyObserved = new AtomicBoolean();
            return response.withBody(response.body()
                    .doOnNext(ignored -> {
                        if (!firstBodyObserved.compareAndSet(false, true)) {
                            return;
                        }
                        commitGuard.advance(
                                GatewayCommitPoint.FIRST_BODY_BUFFER_SENT
                        );
                        observeTransport(
                                observation,
                                match,
                                commitGuard,
                                "BODY_STREAMING"
                        );
                    })
                    .doFinally(signal -> {
                        commitGuard.terminate();
                        observeTransport(
                                observation,
                                match,
                                commitGuard,
                                signal.toString()
                        );
                    }));
        }).doOnError(failure -> {
            commitGuard.terminate();
            observeTransport(
                    observation,
                    match,
                    commitGuard,
                    "ERROR"
            );
        }).doOnCancel(() -> {
            commitGuard.terminate();
            observeTransport(
                    observation,
                    match,
                    commitGuard,
                    "CANCELLED"
            );
        });
    }

    /**
     * 中文说明：执行 invokeAttempt 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke attempt operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.invokeAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param match 参数 match；parameter match。
     * @param normalized 参数 normalized；parameter normalized。
     * @param body 参数 body；parameter body。
     * @param security 参数 安全；parameter security。
     * @param trace 参数 trace；parameter trace。
     * @param observation 参数 观测；parameter observation。
     * @param requestPermit 参数 请求Permit；parameter request permit。
     * @param attemptNumber 参数 attemptNumber；parameter attempt number。
     * @param failedProviders 参数 failedProviders；parameter failed providers。
     * @param commitGuard 参数 commitGuard；parameter commit guard。
     * @return 返回 invokeAttempt 的处理结果；returns the result of the operation.
     */
    private Mono<GatewayOutboundHttpResponse> invokeAttempt(
            HttpRouteMatch match,
            NormalizedHttpRequest normalized,
            RequestBody body,
            GatewayHttpSecurityProcessor.Outcome security,
            GatewayTraceContext trace,
            GatewayCallObservation observation,
            GatewayTrafficGovernance.RequestPermit requestPermit,
            int attemptNumber,
            Set<String> failedProviders,
            GatewayCommitGuard commitGuard) {
        ProviderSelectionHandle selection = providerSelector.select(
                match.route().upstream(),
                match.route().policyRefs(),
                failedProviders
        );
        ProviderInstance provider = selection.instance();
        GatewayTrafficGovernance.AttemptPermit attemptPermit;
        try {
            attemptPermit = requestPermit.acquireAttempt(provider);
        } catch (RuntimeException failure) {
            selection.close();
            return Mono.error(failure);
        }
        long attemptStartedAt = System.currentTimeMillis();
        long attemptStartedNanos = System.nanoTime();
        GatewayTelemetry.AttemptTrace attemptTrace =
                observation.beginAttempt(
                        attemptNumber,
                        provider.instanceId(),
                        provider.serviceKey().protocolType().name()
                );
        String attemptSpanId = attemptTrace.spanId();
        AttemptLifecycle lifecycle = new AttemptLifecycle(
                selection,
                attemptPermit,
                provider,
                observation,
                attemptNumber,
                attemptSpanId,
                attemptStartedAt,
                attemptStartedNanos,
                failedProviders
        );
        observation.provider(provider.instanceId(), Map.of(
                "serviceKey",
                provider.serviceKey().serviceName(),
                "protocol",
                provider.serviceKey().protocolType().name(),
                "version",
                provider.serviceKey().version(),
                "group",
                provider.serviceKey().group()
        ));
        Map<String, List<String>> headers = forwardedHeaders(
                normalized.headers(),
                trace,
                attemptTrace,
                security,
                match.route().transportPolicy()
                        .authorizationForwardingAllowed(),
                provider.serviceKey().protocolType()
                        == ProviderProtocolType.HTTP
                        || provider.serviceKey().protocolType()
                        == ProviderProtocolType.RPC,
                security.routeSecurityType()
                        == top.egon.cola.component.gateway.core.security.GatewayRouteSecurityType.PUBLIC_PROTOCOL
        );
        Mono<GatewayOutboundHttpResponse> invocation;
        if (provider.serviceKey().protocolType()
                == ProviderProtocolType.RPC) {
            if (httpRpcUpstream == null) {
                invocation = Mono.error(new IllegalStateException(
                        "GATEWAY_HTTP_RPC_BRIDGE_UNAVAILABLE"
                ));
            } else {
                invocation = httpRpcUpstream.invoke(
                                match,
                                provider,
                                normalized,
                                Objects.requireNonNull(
                                        body.aggregated(),
                                        "RPC body"
                                ),
                                headers,
                                requestPermit.timeout()
                        )
                        .onErrorResume(
                                HttpRpcUpstreamAdapter
                                        .HttpRpcUpstreamException.class,
                                failure -> Mono.just(rpcError(
                                        failure,
                                        trace.traceId()
                                ))
                        );
            }
        } else {
            if (!body.replayable()) {
                commitGuard.advance(GatewayCommitPoint.REQUEST_STREAMING);
                observeTransport(
                        observation,
                        match,
                        commitGuard,
                        "REQUEST_STREAMING"
                );
            }
            invocation = transportDispatcher.dispatchHttp(
                    new GatewayHttpProxyContext(
                            upstreamAdapter,
                            provider,
                            normalized.method(),
                            normalized.normalizedPath()
                                    + (normalized.rawQuery().isEmpty()
                                    ? ""
                                    : "?" + normalized.rawQuery()),
                            headers,
                            body.publisher(),
                            match.route().transportPolicy(),
                            bodyLogSampleBytes,
                            bodyLogObserver
                    )
            );
        }
        Mono<GatewayOutboundHttpResponse> attemptResponse = invocation
                .map(response -> provider.serviceKey().protocolType()
                        == ProviderProtocolType.RPC
                        ? bodySizeLimiter.limitResponse(
                                response,
                                requestPermit.responseSizeLimit(
                                        maxResponseBytes
                                )
                        )
                        : response)
                .doOnNext(ignored -> {
                    commitGuard.advance(
                            GatewayCommitPoint.UPSTREAM_HEADERS_RECEIVED
                    );
                    observeTransport(
                            observation,
                            match,
                            commitGuard,
                            "UPSTREAM_HEADERS"
                    );
                })
                .flatMap(response -> {
                    GatewayOutboundHttpResponse tracked =
                            trackAttemptResponse(response, lifecycle);
                    boolean retryStatus = requestPermit.retryPolicy()
                            .retryableHttpStatus(response.status())
                            && attemptCoordinator.canRetryLegacyStatus(
                            match.route().transportPolicy(),
                            requestPermit.retryPolicy(),
                            commitGuard,
                            idempotent(match, normalized),
                            body.replayable(),
                            attemptNumber
                    );
                    if (retryStatus) {
                        return tracked.body()
                                .doOnNext(
                                        GatewayDataBufferOwnership::release
                                )
                                .then(Mono.error(
                                        new RetryableHttpStatusException(
                                                response.status()
                                        )
                                ));
                    }
                    return Mono.just(tracked);
                })
                .doOnError(lifecycle::fail);
        return handoffAttemptResponse(attemptResponse, lifecycle);
    }

    /**
     * 中文说明：执行 handoffAttempt响应 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handoff attempt response operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.handoffAttemptResponse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @param lifecycle 参数 生命周期；parameter lifecycle。
     * @return 返回 handoffAttempt响应 的处理结果；returns the result of the operation.
     */
    private Mono<GatewayOutboundHttpResponse> handoffAttemptResponse(
            Mono<GatewayOutboundHttpResponse> response,
            AttemptLifecycle lifecycle) {
        return Mono.create(sink -> {
            // MonoSink makes cancellation and response ownership transfer
            // mutually exclusive before the streamed body takes ownership.
            Disposable.Swap upstream = Disposables.swap();
            sink.onCancel(() -> {
                lifecycle.cancel();
                upstream.dispose();
            });
            Disposable subscription = response
                    .doOnDiscard(
                            GatewayOutboundHttpResponse.class,
                            GatewayOutboundHttpResponse::abandon
                    )
                    .contextWrite(sink.currentContext())
                    .subscribe(
                            sink::success,
                            sink::error,
                            sink::success
                    );
            upstream.update(subscription);
        });
    }

    /**
     * 中文说明：执行 trackAttempt响应 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the track attempt response operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.trackAttemptResponse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @param lifecycle 参数 生命周期；parameter lifecycle。
     * @return 返回 trackAttempt响应 的处理结果；returns the result of the operation.
     */
    private GatewayOutboundHttpResponse trackAttemptResponse(
            GatewayOutboundHttpResponse response,
            AttemptLifecycle lifecycle) {
        return response.withBody(
                response.body()
                        .doOnComplete(() ->
                                lifecycle.complete(response.status()))
                        .doOnError(lifecycle::fail)
                        .doOnCancel(lifecycle::cancel)
        ).onAbandon(lifecycle::cancel);
    }

    /**
     * 中文说明：{@code RequestBody} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责请求Body相关的职责与边界。
     * English summary: {@code RequestBody} is an immutable data carrier in the current Gateway module; it owns the request body-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param publisher 参数 发布器；parameter publisher。
     * @param aggregated 参数 aggregated；parameter aggregated。
     * @param replayable 参数 replayable；parameter replayable。
     */
    private record RequestBody(
            /**
             * 中文说明：保存 发布器 对应的状态、依赖或配置值；字段类型为 {@code Flux<org.springframework.core.io.buffer.DataBuffer>}，由 {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by publisher; its type is {@code Flux<org.springframework.core.io.buffer.DataBuffer>}, and {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.RequestBody}; do not couple callers to its representation when the owning type exposes an API.
             */
            Flux<org.springframework.core.io.buffer.DataBuffer> publisher,
            /**
             * 中文说明：保存 aggregated 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by aggregated; its type is {@code byte[]}, and {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.RequestBody}; do not couple callers to its representation when the owning type exposes an API.
             */
            byte[] aggregated,
            /**
             * 中文说明：保存 replayable 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by replayable; its type is {@code boolean}, and {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.RequestBody}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean replayable
    ) {

        /**
         * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler.RequestBody} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param publisher 参数 发布器；parameter publisher。
         * @param aggregated 参数 aggregated；parameter aggregated。
         * @param replayable 参数 replayable；parameter replayable。
         */
        private RequestBody {
            publisher = Objects.requireNonNull(publisher, "publisher");
        }
    }

    /**
     * 中文说明：执行 forwardedHeaders 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the forwarded headers operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.forwardedHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param trace 参数 trace；parameter trace。
     * @param attemptTrace 参数 attemptTrace；parameter attempt trace。
     * @param security 参数 安全；parameter security。
     * @param authorizationForwardingAllowed 参数 授权ForwardingAllowed；parameter authorization forwarding allowed。
     * @param forwardHttpCredential 参数 forwardHttp凭证；parameter forward http credential。
     * @return 返回 forwardedHeaders 的处理结果；returns the result of the operation.
     */
    private Map<String, List<String>> forwardedHeaders(
            Map<String, List<String>> source,
            GatewayTraceContext trace,
            GatewayTelemetry.AttemptTrace attemptTrace,
            GatewayHttpSecurityProcessor.Outcome security,
            boolean authorizationForwardingAllowed,
            boolean forwardHttpCredential,
            boolean publicProtocol) {
        Map<String, List<String>> sanitized =
                identitySanitizer.sanitizeHttp(
                source,
                security.fieldsToRemove(),
                security.trustedIdentity(),
                authorizationForwardingAllowed
        );
        Map<String, List<String>> result = new LinkedHashMap<>(sanitized);
        if (publicProtocol) {
            protocolCookie(source).ifPresent(cookie ->
                    result.put("cookie", List.of(cookie)));
        }
        restoreOriginalBearer(
                result,
                security,
                authorizationForwardingAllowed && forwardHttpCredential
        );
        result.put(
                "traceparent",
                List.of(attemptTrace.traceparent())
        );
        result.put(
                "x-egon-request-id",
                List.of(trace.requestId())
        );
        if (attemptTrace.tracestate() != null) {
            result.put(
                    "tracestate",
                    List.of(attemptTrace.tracestate())
            );
        }
        return Map.copyOf(result);
    }

    private static final Set<String> PROTOCOL_COOKIE_NAMES = Set.of(
            "__host-egon_user_at",
            "__host-egon_user_rt",
            "egon_user_at_local",
            "egon_user_rt_local"
    );

    private static java.util.Optional<String> protocolCookie(
            Map<String, List<String>> source) {
        List<String> values = source.entrySet().stream()
                .filter(entry -> "cookie".equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .toList();
        List<String> allowed = new java.util.ArrayList<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            for (String part : value.split(";")) {
                String candidate = part.trim();
                int equals = candidate.indexOf('=');
                if (equals <= 0) {
                    continue;
                }
                String name = candidate.substring(0, equals).trim();
                if (PROTOCOL_COOKIE_NAMES.contains(name.toLowerCase(
                        java.util.Locale.ROOT))) {
                    allowed.add(candidate);
                }
            }
        }
        return allowed.isEmpty()
                ? java.util.Optional.empty()
                : java.util.Optional.of(String.join("; ", allowed));
    }

    private GatewayOutboundHttpResponse withSecurityHeaders(
            GatewayOutboundHttpResponse response,
            Map<String, List<String>> securityHeaders) {
        if (securityHeaders == null || securityHeaders.isEmpty()) {
            return response;
        }
        Map<String, List<String>> merged = new LinkedHashMap<>(
                response.headers());
        securityHeaders.forEach((name, values) -> merged.merge(
                name.toLowerCase(java.util.Locale.ROOT),
                List.copyOf(values),
                (existing, replacement) -> {
                    List<String> combined = new java.util.ArrayList<>(existing);
                    combined.addAll(replacement);
                    return List.copyOf(combined);
                }));
        return response.withHeadersAndBody(merged, response.body());
    }

    /**
     * 中文说明：执行 restoreOriginalBearer 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the restore original bearer operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.restoreOriginalBearer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sanitized 参数 sanitized；parameter sanitized。
     * @param security 参数 安全；parameter security。
     * @param forwardHttpCredential 参数 forwardHttp凭证；parameter forward http credential。
     */
    static void restoreOriginalBearer(
            Map<String, List<String>> sanitized,
            GatewayHttpSecurityProcessor.Outcome security,
            boolean forwardHttpCredential
    ) {
        if (forwardHttpCredential && security.forwardingCredential() != null) {
            sanitized.put(
                    "authorization",
                    List.of("Bearer "
                            + security.forwardingCredential().tokenReference())
            );
        }
    }

    /**
     * 中文说明：执行 error 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the error operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.error(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @param code 参数 code；parameter code。
     * @param traceId 参数 traceId；parameter trace id。
     * @return 返回 error 的处理结果；returns the result of the operation.
     */
    private GatewayOutboundHttpResponse error(
            int status,
            String code,
            String traceId) {
        return error(status, code, traceId, Map.of());
    }

    private GatewayOutboundHttpResponse error(
            int status,
            String code,
            String traceId,
            Map<String, List<String>> extraHeaders) {
        String body = "{\"success\":false,\"code\":\""
                + code
                + "\",\"traceId\":\""
                + traceId
                + "\"}";
        GatewayOutboundHttpResponse response = new GatewayOutboundHttpResponse(
                status,
                Map.of(
                        "content-type",
                        List.of("application/json; charset=UTF-8")
                ),
                reactor.core.publisher.Flux.just(
                        BUFFER_FACTORY.wrap(body.getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        ))
                )
        );
        return withSecurityHeaders(response, extraHeaders);
    }

    /**
     * 中文说明：执行 流量Error 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the traffic error operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.trafficError(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param rejected 参数 rejected；parameter rejected。
     * @param traceId 参数 traceId；parameter trace id。
     * @return 返回 流量Error 的处理结果；returns the result of the operation.
     */
    private GatewayOutboundHttpResponse trafficError(
            GatewayTrafficRejectedException rejected,
            String traceId) {
        GatewayOutboundHttpResponse response = error(
                rejected.httpStatus(),
                rejected.code(),
                traceId
        );
        if (rejected.retryAfterMillis() == 0) {
            return response;
        }
        Map<String, List<String>> headers = new LinkedHashMap<>(
                response.headers()
        );
        headers.put(
                "retry-after",
                List.of(Long.toString(Math.max(
                        1,
                        (rejected.retryAfterMillis() + 999) / 1000
                )))
        );
        return response.withHeadersAndBody(
                headers,
                response.body()
        );
    }

    /**
     * 中文说明：执行 rpcError 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rpc error operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.rpcError(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     * @param traceId 参数 traceId；parameter trace id。
     * @return 返回 rpcError 的处理结果；returns the result of the operation.
     */
    private GatewayOutboundHttpResponse rpcError(
            HttpRpcUpstreamAdapter.HttpRpcUpstreamException failure,
            String traceId) {
        int status = switch (failure.status().getCode()) {
            case INVALID_ARGUMENT, FAILED_PRECONDITION -> 400;
            case UNAUTHENTICATED -> 401;
            case PERMISSION_DENIED -> 403;
            case NOT_FOUND -> 404;
            case ALREADY_EXISTS, ABORTED -> 409;
            case RESOURCE_EXHAUSTED -> 429;
            case DEADLINE_EXCEEDED -> 504;
            case UNAVAILABLE -> 503;
            default -> 502;
        };
        return error(
                status,
                "GATEWAY_RPC_UPSTREAM_"
                        + failure.status().getCode().name(),
                traceId
        );
    }

    /**
     * 中文说明：执行 observed 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observed operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.observed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @param observation 参数 观测；parameter observation。
     * @param terminalStage 参数 terminalStage；parameter terminal stage。
     * @param category 参数 category；parameter category。
     * @param code 参数 code；parameter code。
     * @return 返回 observed 的处理结果；returns the result of the operation.
     */
    private GatewayOutboundHttpResponse observed(
            GatewayOutboundHttpResponse response,
            GatewayCallObservation observation,
            String terminalStage,
            String category,
            String code) {
        Map<String, List<String>> headers = new LinkedHashMap<>(
                response.headers()
        );
        headers.put(
                "traceparent",
                List.of(observation.trace().engineTraceparent())
        );
        headers.put(
                "x-egon-request-id",
                List.of(observation.trace().requestId())
        );
        return response.withHeadersAndBody(
                headers,
                response.body()
                        .doOnNext(buffer -> observation.addResponseBytes(
                                buffer.readableByteCount()
                        ))
                        .doOnComplete(() -> publish(
                                observation,
                                terminalStage,
                                category,
                                code,
                                response.status()
                        ))
                        .doOnError(failure -> publish(
                                observation,
                                "RESPONSE",
                                "ERROR",
                                "GATEWAY_RESPONSE_STREAM_ERROR",
                                response.status()
                        ))
                        .doOnCancel(() -> publish(
                                observation,
                                "RESPONSE",
                                "CANCELLED",
                                "GATEWAY_CLIENT_CANCELLED",
                                response.status()
                        ))
        );
    }

    /**
     * 中文说明：执行 publish 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the publish operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.publish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param observation 参数 观测；parameter observation。
     * @param stage 参数 stage；parameter stage。
     * @param category 参数 category；parameter category。
     * @param code 参数 code；parameter code。
     * @param status 参数 status；parameter status。
     */
    private void publish(
            GatewayCallObservation observation,
            String stage,
            String category,
            String code,
            Integer status) {
        observation.complete(stage, category, code, status, null)
                .ifPresent(completionListener::onComplete);
    }

    /**
     * 中文说明：执行 traceContext 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace context operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.traceContext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 traceContext 的处理结果；returns the result of the operation.
     */
    private GatewayTraceContext traceContext(
            Map<String, List<String>> headers) {
        return GatewayTraceContext.fromHeaders(
                firstHeader(headers, "traceparent"),
                firstHeader(headers, "tracestate"),
                firstHeader(headers, "x-egon-request-id")
        );
    }

    /**
     * 中文说明：执行 firstHeader 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the first header operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.firstHeader(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @param expected 参数 expected；parameter expected。
     * @return 返回 firstHeader 的处理结果；returns the result of the operation.
     */
    private String firstHeader(
            Map<String, List<String>> headers,
            String expected) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(expected))
                .map(Map.Entry::getValue)
                .filter(values -> values != null && !values.isEmpty())
                .map(List::getFirst)
                .findFirst()
                .orElse(null);
    }

    /**
     * 中文说明：执行 category 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the category operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.category(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 category 的处理结果；returns the result of the operation.
     */
    private String category(int status) {
        if (status < 400) {
            return "SUCCESS";
        }
        return status < 500 ? "REJECTED" : "ERROR";
    }

    /**
     * 中文说明：执行 classification 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the classification operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.classification(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 classification 的处理结果；returns the result of the operation.
     */
    private ProviderCallClassification classification(int status) {
        if (status < 400) {
            return ProviderCallClassification.SUCCESS;
        }
        return status >= 500
                ? ProviderCallClassification.RETRYABLE_FAILURE
                : ProviderCallClassification.BUSINESS_FAILURE;
    }

    /**
     * 中文说明：执行 健康Outcome 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the health outcome operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.healthOutcome(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param classification 参数 classification；parameter classification。
     * @return 返回 健康Outcome 的处理结果；returns the result of the operation.
     */
    private ProviderCallOutcome healthOutcome(
            ProviderCallClassification classification) {
        return switch (classification) {
            case SUCCESS -> ProviderCallOutcome.SUCCESS;
            case RETRYABLE_FAILURE ->
                    ProviderCallOutcome.RETRYABLE_FAILURE;
            case BUSINESS_FAILURE ->
                    ProviderCallOutcome.BUSINESS_REJECTION;
            case CANCELLED -> ProviderCallOutcome.CANCELLED;
        };
    }

    /**
     * 中文说明：执行 retryable 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retryable operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.retryable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     * @return 返回 retryable 的处理结果；returns the result of the operation.
     */
    private boolean retryable(Throwable failure) {
        return failure instanceof RetryableHttpStatusException
                || failure instanceof java.io.IOException
                || failure instanceof java.util.concurrent.TimeoutException
                || failure instanceof java.net.ConnectException
                || failure.getCause() != null
                && failure.getCause() != failure
                && retryable(failure.getCause());
    }

    /**
     * 中文说明：执行 idempotent 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the idempotent operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.idempotent(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param match 参数 match；parameter match。
     * @param request 参数 请求；parameter request。
     * @return 返回 idempotent 的处理结果；returns the result of the operation.
     */
    private boolean idempotent(
            HttpRouteMatch match,
            NormalizedHttpRequest request) {
        String configured = match.route().metadata().get("idempotent");
        if (configured != null) {
            return Boolean.parseBoolean(configured);
        }
        return Set.of("GET", "HEAD", "OPTIONS", "PUT", "DELETE")
                .contains(request.method());
    }

    /**
     * 中文说明：执行 流量Context 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the traffic context operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.trafficContext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param match 参数 match；parameter match。
     * @param normalized 参数 normalized；parameter normalized。
     * @param request 参数 请求；parameter request。
     * @param security 参数 安全；parameter security。
     * @return 返回 流量Context 的处理结果；returns the result of the operation.
     */
    private GatewayTrafficContext trafficContext(
            HttpRouteMatch match,
            NormalizedHttpRequest normalized,
            GatewayInboundHttpRequest request,
            GatewayHttpSecurityProcessor.Outcome security) {
        return new GatewayTrafficContext(
                match.route().operationId(),
                match.route().routeId(),
                match.route().metadata().getOrDefault(
                        "applicationCode",
                        match.route().gatewayGroupId()
                ),
                security.trustedIdentity().httpHeaders().get(
                        "X-Egon-Gateway-Principal-Id"
                ),
                request.remoteAddress() == null
                        ? null
                        : request.remoteAddress().getAddress()
                        .getHostAddress(),
                match.route().upstream().serviceName(),
                null,
                approvedHeaders(normalized.headers()),
                match.pathVariables(),
                queryParameters(normalized.rawQuery())
        );
    }

    /**
     * 中文说明：执行 approvedHeaders 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the approved headers operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.approvedHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 approvedHeaders 的处理结果；returns the result of the operation.
     */
    private Map<String, String> approvedHeaders(
            Map<String, List<String>> headers) {
        Map<String, String> approved = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (!Set.of(
                    "authorization",
                    "proxy-authorization",
                    "cookie",
                    "set-cookie"
            ).contains(lower)
                    && values != null
                    && !values.isEmpty()) {
                approved.put(lower, values.getFirst());
            }
        });
        return Map.copyOf(approved);
    }

    /**
     * 中文说明：执行 queryParameters 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the query parameters operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.queryParameters(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 queryParameters 的处理结果；returns the result of the operation.
     */
    private Map<String, String> queryParameters(String query) {
        if (query == null || query.isBlank()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String parameter : query.split("&")) {
            int separator = parameter.indexOf('=');
            String name = separator < 0
                    ? parameter
                    : parameter.substring(0, separator);
            String value = separator < 0
                    ? ""
                    : parameter.substring(separator + 1);
            values.putIfAbsent(
                    URLDecoder.decode(name, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8)
            );
        }
        return Map.copyOf(values);
    }

    /**
     * 中文说明：执行 elapsedMillis 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the elapsed millis operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.elapsedMillis(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param startedNanos 参数 startedNanos；parameter started nanos。
     * @return 返回 elapsedMillis 的处理结果；returns the result of the operation.
     */
    private long elapsedMillis(long startedNanos) {
        return Math.max(
                0,
                (System.nanoTime() - startedNanos) / 1_000_000
        );
    }

    /**
     * 中文说明：执行 网关Context 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway context operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.gatewayContext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param match 参数 match；parameter match。
     * @param trace 参数 trace；parameter trace。
     * @return 返回 网关Context 的处理结果；returns the result of the operation.
     */
    private GatewayContext gatewayContext(
            AccessZone accessZone,
            HttpRouteMatch match,
            GatewayTraceContext trace) {
        Instant startedAt = Instant.now();
        return new GatewayContext(
                trace.requestId(),
                trace.traceId(),
                trace.engineTraceparent(),
                trace.tracestate(),
                accessZone,
                match.route().gatewayGroupId(),
                engineNodeId,
                match.route().operationId(),
                match.route().routeId(),
                match.route().metadata().get("releaseId"),
                null,
                null,
                startedAt.plus(upstreamTimeout),
                startedAt,
                GatewayStage.ROUTE_MATCHED,
                List.of(),
                List.of()
        );
    }

    /**
     * 中文说明：{@code HttpStageExchange} 是类型，位于当前 Gateway 模块的相关包中，负责HttpStageExchange相关的职责与边界。
     * English summary: {@code HttpStageExchange} is a type in the current Gateway module; it owns the http stage exchange-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private final class HttpStageExchange
            extends AbstractGatewayHttpStageExchange {

        /**
         * 中文说明：保存 accessZone 对应的状态、依赖或配置值；字段类型为 {@code AccessZone}，由 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by access zone; its type is {@code AccessZone}, and {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AccessZone accessZone;

        /**
         * 中文说明：保存 normalized 对应的状态、依赖或配置值；字段类型为 {@code NormalizedHttpRequest}，由 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by normalized; its type is {@code NormalizedHttpRequest}, and {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final NormalizedHttpRequest normalized;

        /**
         * 中文说明：保存 match 对应的状态、依赖或配置值；字段类型为 {@code HttpRouteMatch}，由 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by match; its type is {@code HttpRouteMatch}, and {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final HttpRouteMatch match;

        /**
         * 中文说明：保存 路由方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by route method; its type is {@code String}, and {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String routeMethod;

        /**
         * 中文说明：保存 trace 对应的状态、依赖或配置值；字段类型为 {@code GatewayTraceContext}，由 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trace; its type is {@code GatewayTraceContext}, and {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayTraceContext trace;

        /**
         * 中文说明：保存 观测 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallObservation}，由 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observation; its type is {@code GatewayCallObservation}, and {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayCallObservation observation;

        /**
         * 中文说明：保存 cors 对应的状态、依赖或配置值；字段类型为 {@code GatewayCorsProcessor.Decision}，由 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by cors; its type is {@code GatewayCorsProcessor.Decision}, and {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private GatewayCorsProcessor.Decision cors;

        /**
         * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpSecurityProcessor.Outcome}，由 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code GatewayHttpSecurityProcessor.Outcome}, and {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private GatewayHttpSecurityProcessor.Outcome security;

        /**
         * 中文说明：保存 permit 对应的状态、依赖或配置值；字段类型为 {@code GatewayTrafficGovernance.RequestPermit}，由 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by permit; its type is {@code GatewayTrafficGovernance.RequestPermit}, and {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private GatewayTrafficGovernance.RequestPermit permit;

        /**
         * 中文说明：保存 failed 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failed; its type is {@code boolean}, and {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean failed;

        /**
         * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param accessZone 参数 accessZone；parameter access zone。
         * @param request 参数 请求；parameter request。
         * @param normalized 参数 normalized；parameter normalized。
         * @param match 参数 match；parameter match。
         * @param routeMethod 参数 路由方法；parameter route method。
         * @param trace 参数 trace；parameter trace。
         * @param observation 参数 观测；parameter observation。
         */
        private HttpStageExchange(
                AccessZone accessZone,
                GatewayInboundHttpRequest request,
                NormalizedHttpRequest normalized,
                HttpRouteMatch match,
                String routeMethod,
                GatewayTraceContext trace,
                GatewayCallObservation observation) {
            super(request, gatewayContext(accessZone, match, trace));
            this.accessZone = accessZone;
            this.normalized = normalized;
            this.match = match;
            this.routeMethod = routeMethod;
            this.trace = trace;
            this.observation = observation;
        }

        /**
         * 中文说明：执行 cors 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the cors operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange.cors(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param chain 参数 chain；parameter chain。
         * @return 返回 cors 的处理结果；returns the result of the operation.
         */
        @Override
        public Publisher<GatewayResponse> cors(
                GatewayFilterChain chain) {
            cors = corsProcessor.evaluate(
                    match.route().policyRefs(),
                    inbound(),
                    routeMethod,
                    trace.traceId()
            );
            return cors.preflightResponse()
                    .<Publisher<GatewayResponse>>map(this::respond)
                    .orElseGet(() -> chain.filter(this));
        }

        /**
         * 中文说明：执行 安全 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the security operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange.security(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param chain 参数 chain；parameter chain。
         * @return 返回 安全 的处理结果；returns the result of the operation.
         */
        @Override
        public Publisher<GatewayResponse> security(
                GatewayFilterChain chain) {
            return securityProcessor.authorize(
                            accessZone,
                            inbound(),
                            normalized,
                            match,
                            trace.traceId()
                    )
                    .flatMap(outcome -> {
                        security = outcome;
                        return Mono.from(chain.filter(this));
                    });
        }

        /**
         * 中文说明：执行 governance 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the governance operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange.governance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param chain 参数 chain；parameter chain。
         * @return 返回 governance 的处理结果；returns the result of the operation.
         */
        @Override
        public Publisher<GatewayResponse> governance(
                GatewayFilterChain chain) {
            GatewayTrafficContext context = trafficContext(
                    match,
                    normalized,
                    inbound(),
                    security
            );
            return trafficGovernance.acquire(
                            match.route().policyRefs(),
                            context,
                            upstreamTimeout
                    )
                    .flatMap(acquired -> {
                        permit = acquired;
                        return Mono.from(chain.filter(this))
                                .doFinally(signal -> acquired.close());
                    });
        }

        /**
         * 中文说明：执行 invoke 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the invoke operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 invoke 的处理结果；returns the result of the operation.
         */
        @Override
        public Publisher<GatewayResponse> invoke() {
            return invokeUpstream(
                    match,
                    normalized,
                    inbound(),
                    security,
                    trace,
                    observation,
                    permit
            )
                    .map(response -> withSecurityHeaders(
                            response,
                            security.responseHeaders()))
                    .map(cors::decorate)
                    .flatMap(response -> Mono.from(respond(response)));
        }

        /**
         * 中文说明：执行 mapFailure 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the map failure operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange.mapFailure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param failure 参数 failure；parameter failure。
         * @return 返回 mapFailure 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayOutboundHttpResponse mapFailure(Throwable failure) {
            failed = true;
            if (failure instanceof GatewaySecurityException rejected) {
                return observed(
                        error(
                                rejected.httpStatus(),
                                rejected.code(),
                                trace.traceId(),
                                rejected.responseHeaders()
                        ),
                        observation,
                        "SECURITY",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof GatewayCorsException rejected) {
                return observed(
                        error(403, rejected.code(), trace.traceId()),
                        observation,
                        "CORS",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof GatewayRequestRejectedException rejected) {
                return observed(
                        error(
                                rejected.status(),
                                rejected.code(),
                                trace.traceId()
                        ),
                        observation,
                        "RESOURCE",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof GatewayRequestBodyTooLargeException
                    rejected) {
                return observed(
                        error(413, rejected.code(), trace.traceId()),
                        observation,
                        "RESOURCE",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof GatewayResponseBodyTooLargeException
                    rejected) {
                return observed(
                        error(502, rejected.code(), trace.traceId()),
                        observation,
                        "UPSTREAM",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof GatewayTrafficRejectedException rejected) {
                observation.governance(
                        "APPLIED",
                        rejected.code(),
                        "REJECT"
                );
                return observed(
                        trafficError(rejected, trace.traceId()),
                        observation,
                        "GOVERNANCE",
                        "REJECTED",
                        rejected.code()
                );
            }
            if (failure instanceof java.util.concurrent.TimeoutException) {
                return observed(
                        error(
                                504,
                                "GATEWAY_UPSTREAM_TIMEOUT",
                                trace.traceId()
                        ),
                        observation,
                        "UPSTREAM",
                        "TIMEOUT",
                        "GATEWAY_UPSTREAM_TIMEOUT"
                );
            }
            return observed(
                    error(
                            502,
                            "GATEWAY_UPSTREAM_CONNECT_FAILED",
                            trace.traceId()
                    ),
                    observation,
                    "UPSTREAM",
                    "ERROR",
                    "GATEWAY_UPSTREAM_CONNECT_FAILED"
            );
        }

        /**
         * 中文说明：执行 failed 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the failed operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.HttpStageExchange.failed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 failed 的处理结果；returns the result of the operation.
         */
        private boolean failed() {
            return failed;
        }
    }

    /**
     * 中文说明：{@code WebSocketStageExchange} 是类型，位于当前 Gateway 模块的相关包中，负责WebSocketStageExchange相关的职责与边界。
     * English summary: {@code WebSocketStageExchange} is a type in the current Gateway module; it owns the web socket stage exchange-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private final class WebSocketStageExchange
            extends AbstractGatewayHttpStageExchange {

        /**
         * 中文说明：保存 accessZone 对应的状态、依赖或配置值；字段类型为 {@code AccessZone}，由 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by access zone; its type is {@code AccessZone}, and {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AccessZone accessZone;

        /**
         * 中文说明：保存 normalized 对应的状态、依赖或配置值；字段类型为 {@code NormalizedHttpRequest}，由 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by normalized; its type is {@code NormalizedHttpRequest}, and {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final NormalizedHttpRequest normalized;

        /**
         * 中文说明：保存 match 对应的状态、依赖或配置值；字段类型为 {@code HttpRouteMatch}，由 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by match; its type is {@code HttpRouteMatch}, and {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final HttpRouteMatch match;

        /**
         * 中文说明：保存 trace 对应的状态、依赖或配置值；字段类型为 {@code GatewayTraceContext}，由 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trace; its type is {@code GatewayTraceContext}, and {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayTraceContext trace;

        /**
         * 中文说明：保存 观测 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallObservation}，由 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observation; its type is {@code GatewayCallObservation}, and {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayCallObservation observation;

        /**
         * 中文说明：保存 handedOff 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by handed off; its type is {@code AtomicBoolean}, and {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean handedOff = new AtomicBoolean();

        /**
         * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code GatewayHttpSecurityProcessor.Outcome}，由 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code GatewayHttpSecurityProcessor.Outcome}, and {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private GatewayHttpSecurityProcessor.Outcome security;

        /**
         * 中文说明：保存 请求Permit 对应的状态、依赖或配置值；字段类型为 {@code GatewayTrafficGovernance.RequestPermit}，由 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by request permit; its type is {@code GatewayTrafficGovernance.RequestPermit}, and {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange}; do not couple callers to its representation when the owning type exposes an API.
         */
        private GatewayTrafficGovernance.RequestPermit requestPermit;

        /**
         * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param accessZone 参数 accessZone；parameter access zone。
         * @param request 参数 请求；parameter request。
         * @param normalized 参数 normalized；parameter normalized。
         * @param match 参数 match；parameter match。
         * @param trace 参数 trace；parameter trace。
         * @param observation 参数 观测；parameter observation。
         */
        private WebSocketStageExchange(
                AccessZone accessZone,
                GatewayInboundHttpRequest request,
                NormalizedHttpRequest normalized,
                HttpRouteMatch match,
                GatewayTraceContext trace,
                GatewayCallObservation observation) {
            super(request, gatewayContext(accessZone, match, trace));
            this.accessZone = accessZone;
            this.normalized = normalized;
            this.match = match;
            this.trace = trace;
            this.observation = observation;
        }

        /**
         * 中文说明：执行 cors 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the cors operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange.cors(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param chain 参数 chain；parameter chain。
         * @return 返回 cors 的处理结果；returns the result of the operation.
         */
        @Override
        public Publisher<GatewayResponse> cors(GatewayFilterChain chain) {
            GatewayCorsProcessor.Decision decision = corsProcessor.evaluate(
                    match.route().policyRefs(),
                    inbound(),
                    normalized.method(),
                    trace.traceId()
            );
            return decision.preflightResponse()
                    .<Publisher<GatewayResponse>>map(this::respond)
                    .orElseGet(() -> chain.filter(this));
        }

        /**
         * 中文说明：执行 安全 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the security operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange.security(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param chain 参数 chain；parameter chain。
         * @return 返回 安全 的处理结果；returns the result of the operation.
         */
        @Override
        public Publisher<GatewayResponse> security(
                GatewayFilterChain chain) {
            return securityProcessor.authorize(
                            accessZone,
                            inbound(),
                            normalized,
                            match,
                            trace.traceId()
                    )
                    .flatMap(outcome -> {
                        security = outcome;
                        return Mono.from(chain.filter(this));
                    });
        }

        /**
         * 中文说明：执行 governance 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the governance operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange.governance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param chain 参数 chain；parameter chain。
         * @return 返回 governance 的处理结果；returns the result of the operation.
         */
        @Override
        public Publisher<GatewayResponse> governance(
                GatewayFilterChain chain) {
            return trafficGovernance.acquire(
                            match.route().policyRefs(),
                            trafficContext(
                                    match,
                                    normalized,
                                    inbound(),
                                    security
                            ),
                            upstreamTimeout
                    )
                    .flatMap(acquired -> {
                        requestPermit = acquired;
                        return Mono.from(chain.filter(this))
                                .doFinally(ignored -> {
                                    if (!handedOff.get()) {
                                        acquired.close();
                                    }
                                });
                    });
        }

        /**
         * 中文说明：执行 invoke 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the invoke operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 invoke 的处理结果；returns the result of the operation.
         */
        @Override
        public Publisher<GatewayResponse> invoke() {
            if (match.route().transportPolicy().transportProtocol()
                    != GatewayTransportProtocol.WEBSOCKET) {
                return respondWebSocket(
                        GatewayWebSocketHandshakeResult.rejected(
                                426,
                                "GATEWAY_WEBSOCKET_ROUTE_REQUIRED",
                                "route is not configured for WebSocket"
                        )
                );
            }
            ProviderSelectionHandle selection = providerSelector.select(
                    match.route().upstream(),
                    match.route().policyRefs(),
                    Set.of()
            );
            ProviderInstance provider = selection.instance();
            GatewayTrafficGovernance.AttemptPermit attemptPermit;
            try {
                attemptPermit = requestPermit.acquireAttempt(provider);
            } catch (RuntimeException failure) {
                selection.close();
                return Mono.error(failure);
            }
            GatewayTelemetry.AttemptTrace attemptTrace =
                    observation.beginAttempt(
                            1,
                            provider.instanceId(),
                            provider.serviceKey().protocolType().name()
                    );
            AttemptLifecycle lifecycle = new AttemptLifecycle(
                    selection,
                    attemptPermit,
                    provider,
                    observation,
                    1,
                    attemptTrace.spanId(),
                    System.currentTimeMillis(),
                    System.nanoTime(),
                    new LinkedHashSet<>()
            );
            Map<String, List<String>> headers = forwardedHeaders(
                    normalized.headers(),
                    trace,
                    attemptTrace,
                    security,
                    match.route().transportPolicy()
                            .authorizationForwardingAllowed(),
                    provider.serviceKey().protocolType()
                            == ProviderProtocolType.HTTP
                            || provider.serviceKey().protocolType()
                            == ProviderProtocolType.RPC,
                    security.routeSecurityType()
                            == top.egon.cola.component.gateway.core.security.GatewayRouteSecurityType.PUBLIC_PROTOCOL
            );
            GatewayWebSocketProxyContext context =
                    new GatewayWebSocketProxyContext(
                            provider,
                            normalized.normalizedPath()
                                    + (normalized.rawQuery().isEmpty()
                                    ? ""
                                    : "?" + normalized.rawQuery()),
                            headers,
                            subprotocols(inbound().headers()),
                            match.route().transportPolicy(),
                            GatewayCommitGuard.websocket(),
                            webSocketObserver(
                                    observation,
                                    match.route().transportPolicy()
                                            .bodyLogEnabled()
                            )
                    );
            return transportDispatcher.prepareWebSocket(context)
                    .flatMap(result -> {
                        if (result instanceof GatewayWebSocketHandshakeResult
                                .Rejected rejected) {
                            lifecycle.complete(rejected.httpStatus());
                            return Mono.from(respondWebSocket(rejected));
                        }
                        GatewayPreparedWebSocketSession prepared =
                                ((GatewayWebSocketHandshakeResult.Accepted)
                                        result).session();
                        GatewayPreparedWebSocketSession managed =
                                prepared.onDispose(() -> {
                                    try {
                                        lifecycle.complete(101);
                                    } finally {
                                        requestPermit.close();
                                        publish(
                                                observation,
                                                "COMPLETE",
                                                "SUCCESS",
                                                null,
                                                101
                                        );
                                    }
                                });
                        handedOff.set(true);
                        return Mono.from(respondWebSocket(
                                new GatewayWebSocketHandshakeResult.Accepted(
                                        managed
                                )
                        ));
                    })
                    .doOnError(lifecycle::fail)
                    .doOnCancel(lifecycle::cancel);
        }

        /**
         * 中文说明：执行 mapFailure 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the map failure operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.WebSocketStageExchange.mapFailure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param failure 参数 failure；parameter failure。
         * @return 返回 mapFailure 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayOutboundHttpResponse mapFailure(Throwable failure) {
            if (failure instanceof GatewaySecurityException rejected) {
                return error(
                        rejected.httpStatus(),
                        rejected.code(),
                        trace.traceId()
                );
            }
            if (failure instanceof GatewayCorsException rejected) {
                return error(403, rejected.code(), trace.traceId());
            }
            if (failure instanceof GatewayTrafficRejectedException rejected) {
                return trafficError(rejected, trace.traceId());
            }
            if (failure instanceof java.util.concurrent.TimeoutException) {
                return error(
                        504,
                        "GATEWAY_UPSTREAM_TIMEOUT",
                        trace.traceId()
                );
            }
            return error(
                    502,
                    "GATEWAY_UPSTREAM_CONNECT_FAILED",
                    trace.traceId()
            );
        }
    }

    /**
     * 中文说明：执行 webSocketObserver 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the web socket observer operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.webSocketObserver(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param observation 参数 观测；parameter observation。
     * @param bodyLogEnabled 参数 bodyLogEnabled；parameter body log enabled。
     * @return 返回 webSocketObserver 的处理结果；returns the result of the operation.
     */
    private GatewayWebSocketObserver webSocketObserver(
            GatewayCallObservation observation,
            boolean bodyLogEnabled) {
        return new GatewayWebSocketObserver() {
            /**
             * 中文说明：执行 observe 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the observe operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.observe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param transportMode 参数 传输Mode；parameter transport mode。
             * @param commitPoint 参数 commitPoint；parameter commit point。
             * @param terminationReason 参数 terminationReason；parameter termination reason。
             */
            @Override
            public void observe(
                    String transportMode,
                    String commitPoint,
                    String terminationReason) {
                try {
                    observation.transport(
                            transportMode,
                            commitPoint,
                            terminationReason
                    );
                } catch (RuntimeException ignored) {
                    // Observation cannot alter WebSocket forwarding.
                }
            }

            /**
             * 中文说明：执行 observeFrame 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the observe frame operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.observeFrame(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @param direction 参数 direction；parameter direction。
             * @param frameType 参数 frameType；parameter frame type。
             * @param payloadBytes 参数 payloadBytes；parameter payload bytes。
             * @param finalFragment 参数 finalFragment；parameter final fragment。
             */
            @Override
            public void observeFrame(
                    String direction,
                    GatewayWebSocketFrameType frameType,
                    long payloadBytes,
                    boolean finalFragment) {
                if (bodyLogEnabled) {
                    accessLogger.onWebSocketFrame(
                            direction,
                            frameType.name(),
                            payloadBytes,
                            finalFragment
                    );
                }
            }
        };
    }

    /**
     * 中文说明：执行 observe传输 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observe transport operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.observeTransport(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param observation 参数 观测；parameter observation。
     * @param match 参数 match；parameter match。
     * @param commitGuard 参数 commitGuard；parameter commit guard。
     * @param terminationReason 参数 terminationReason；parameter termination reason。
     */
    private void observeTransport(
            GatewayCallObservation observation,
            HttpRouteMatch match,
            GatewayCommitGuard commitGuard,
            String terminationReason) {
        try {
            var policy = match.route().transportPolicy();
            observation.transport(
                    policy.transportProtocol().name()
                            + "_" + policy.requestBodyMode().name()
                            + "_" + policy.responseMode().name(),
                    commitGuard.current().name(),
                    terminationReason
            );
        } catch (RuntimeException ignored) {
            // Observation cannot alter HTTP forwarding.
        }
    }

    /**
     * 中文说明：执行 subprotocols 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the subprotocols operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.subprotocols(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 subprotocols 的处理结果；returns the result of the operation.
     */
    private List<String> subprotocols(
            Map<String, List<String>> headers) {
        return headers.entrySet().stream()
                .filter(entry -> "sec-websocket-protocol".equalsIgnoreCase(
                        entry.getKey()
                ))
                .flatMap(entry -> entry.getValue().stream())
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * 中文说明：{@code RetryableHttpStatusException} 是异常类型，位于当前 Gateway 模块的相关包中，负责RetryableHttpStatusException相关的职责与边界。
     * English summary: {@code RetryableHttpStatusException} is a retryable http status exception exception in the current Gateway module; it owns the retryable http status exception-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class RetryableHttpStatusException
            extends RuntimeException {

        /**
         * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler.RetryableHttpStatusException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler.RetryableHttpStatusException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param status 参数 status；parameter status。
         */
        private RetryableHttpStatusException(int status) {
            super("retryable HTTP status " + status);
        }
    }

    /**
     * 中文说明：{@code AttemptLifecycle} 是类型，位于当前 Gateway 模块的相关包中，负责Attempt生命周期相关的职责与边界。
     * English summary: {@code AttemptLifecycle} is a type in the current Gateway module; it owns the attempt lifecycle-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private final class AttemptLifecycle {

        /**
         * 中文说明：保存 completed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by completed; its type is {@code AtomicBoolean}, and {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean completed = new AtomicBoolean();

        /**
         * 中文说明：保存 selection 对应的状态、依赖或配置值；字段类型为 {@code ProviderSelectionHandle}，由 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by selection; its type is {@code ProviderSelectionHandle}, and {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final ProviderSelectionHandle selection;

        /**
         * 中文说明：保存 permit 对应的状态、依赖或配置值；字段类型为 {@code GatewayTrafficGovernance.AttemptPermit}，由 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by permit; its type is {@code GatewayTrafficGovernance.AttemptPermit}, and {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayTrafficGovernance.AttemptPermit permit;

        /**
         * 中文说明：保存 提供方 对应的状态、依赖或配置值；字段类型为 {@code ProviderInstance}，由 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider; its type is {@code ProviderInstance}, and {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final ProviderInstance provider;

        /**
         * 中文说明：保存 观测 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallObservation}，由 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observation; its type is {@code GatewayCallObservation}, and {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayCallObservation observation;

        /**
         * 中文说明：保存 attemptNumber 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt number; its type is {@code int}, and {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final int attemptNumber;

        /**
         * 中文说明：保存 attemptSpanId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt span id; its type is {@code String}, and {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String attemptSpanId;

        /**
         * 中文说明：保存 attemptStartedAt 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt started at; its type is {@code long}, and {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final long attemptStartedAt;

        /**
         * 中文说明：保存 attemptStartedNanos 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt started nanos; its type is {@code long}, and {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final long attemptStartedNanos;

        /**
         * 中文说明：保存 failedProviders 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failed providers; its type is {@code Set<String>}, and {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Set<String> failedProviders;

        /**
         * 中文说明：创建 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param selection 参数 selection；parameter selection。
         * @param permit 参数 permit；parameter permit。
         * @param provider 参数 提供方；parameter provider。
         * @param observation 参数 观测；parameter observation。
         * @param attemptNumber 参数 attemptNumber；parameter attempt number。
         * @param attemptSpanId 参数 attemptSpanId；parameter attempt span id。
         * @param attemptStartedAt 参数 attemptStartedAt；parameter attempt started at。
         * @param attemptStartedNanos 参数 attemptStartedNanos；parameter attempt started nanos。
         * @param failedProviders 参数 failedProviders；parameter failed providers。
         */
        private AttemptLifecycle(
                ProviderSelectionHandle selection,
                GatewayTrafficGovernance.AttemptPermit permit,
                ProviderInstance provider,
                GatewayCallObservation observation,
                int attemptNumber,
                String attemptSpanId,
                long attemptStartedAt,
                long attemptStartedNanos,
                Set<String> failedProviders) {
            this.selection = selection;
            this.permit = permit;
            this.provider = provider;
            this.observation = observation;
            this.attemptNumber = attemptNumber;
            this.attemptSpanId = attemptSpanId;
            this.attemptStartedAt = attemptStartedAt;
            this.attemptStartedNanos = attemptStartedNanos;
            this.failedProviders = failedProviders;
        }

        /**
         * 中文说明：执行 complete 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the complete operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param status 参数 status；parameter status。
         */
        private void complete(int status) {
            ProviderCallClassification classification =
                    classification(status);
            finish(
                    classification,
                    category(status),
                    null,
                    classification
                            == ProviderCallClassification.RETRYABLE_FAILURE
            );
        }

        /**
         * 中文说明：执行 fail 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the fail operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle.fail(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param failure 参数 failure；parameter failure。
         */
        private void fail(Throwable failure) {
            finish(
                    ProviderCallClassification.RETRYABLE_FAILURE,
                    "ERROR",
                    retryable(failure)
                            ? "RETRYABLE_UPSTREAM_FAILURE"
                            : null,
                    true
            );
        }

        /**
         * 中文说明：执行 cancel 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the cancel operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle.cancel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void cancel() {
            finish(
                    ProviderCallClassification.CANCELLED,
                    "CANCELLED",
                    "GATEWAY_CLIENT_CANCELLED",
                    false
            );
        }

        /**
         * 中文说明：执行 finish 操作；该方法是 {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the finish operation; this method is the invocation entry point on {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayHttpDataPlaneHandler.AttemptLifecycle.finish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param classification 参数 classification；parameter classification。
         * @param category 参数 category；parameter category。
         * @param retryReason 参数 重试Reason；parameter retry reason。
         * @param failedProvider 参数 failed提供方；parameter failed provider。
         */
        private void finish(
                ProviderCallClassification classification,
                String category,
                String retryReason,
                boolean failedProvider) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            try {
                permit.complete(classification);
                outcomeRecorder.record(
                        provider.runtimeIdentity(),
                        healthOutcome(classification)
                );
                if (failedProvider) {
                    failedProviders.add(provider.runtimeIdentity());
                }
                observation.attempt(
                        attemptNumber,
                        attemptSpanId,
                        provider.instanceId(),
                        attemptStartedAt,
                        elapsedMillis(attemptStartedNanos),
                        category,
                        retryReason
                );
            } finally {
                selection.close();
            }
        }
    }
}
