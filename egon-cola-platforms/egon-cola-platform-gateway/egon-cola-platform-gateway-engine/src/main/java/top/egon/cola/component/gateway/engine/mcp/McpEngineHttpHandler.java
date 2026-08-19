package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.mcp.common.protocol.HttpMcpRequest;
import top.egon.cola.component.gateway.mcp.common.protocol.LegacySseMcpAdapter;
import top.egon.cola.component.gateway.mcp.common.protocol.McpDialectAdapter;
import top.egon.cola.component.gateway.mcp.common.protocol.McpJsonRpcCodec;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.common.protocol.RcMcpDialectAdapter;
import top.egon.cola.component.gateway.mcp.common.protocol.StableMcpDialectAdapter;
import top.egon.cola.component.gateway.mcp.rule.domain.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.server.service.McpMethodDispatcher;
import top.egon.cola.component.gateway.mcp.server.domain.McpRequestContext;
import top.egon.cola.component.gateway.mcp.common.transport.McpHttpRequest;
import top.egon.cola.component.gateway.mcp.common.transport.McpHttpResponse;
import top.egon.cola.component.gateway.mcp.common.transport.McpSessionStore;
import top.egon.cola.component.gateway.mcp.common.transport.McpSubscriptionEventStore;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * MCP HTTP front controller for Stable, RC and legacy SSE transports.
 * 补充说明 / Supplementary summary: {@code McpEngineHttpHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCP引擎Http处理器相关的职责与边界。
 * English supplement: {@code McpEngineHttpHandler} is a mcp engine http handler handler in the current Gateway module; it owns the mcp engine http handler-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpEngineHttpHandler {

    /**
     * 中文说明：表示 MCPPATHPREFIX 这一固定值；它属于 {@code McpEngineHttpHandler} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value mcp path prefix; it is a state, type, or protocol value of {@code McpEngineHttpHandler} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String MCP_PATH_PREFIX = "/mcp/";

    /**
     * 中文说明：表示 LEGACYPATHPREFIX 这一固定值；它属于 {@code McpEngineHttpHandler} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value legacy path prefix; it is a state, type, or protocol value of {@code McpEngineHttpHandler} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String LEGACY_PATH_PREFIX = "/legacy/mcp/";

    /**
     * 中文说明：表示 元数据PATHPREFIX 这一固定值；它属于 {@code McpEngineHttpHandler} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value metadata path prefix; it is a state, type, or protocol value of {@code McpEngineHttpHandler} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String METADATA_PATH_PREFIX =
            "/.well-known/oauth-protected-resource/mcp/";

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 分发器 对应的状态、依赖或配置值；字段类型为 {@code McpMethodDispatcher}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by dispatcher; its type is {@code McpMethodDispatcher}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpMethodDispatcher dispatcher;

    /**
     * 中文说明：保存 sessions 对应的状态、依赖或配置值；字段类型为 {@code McpSessionStore}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by sessions; its type is {@code McpSessionStore}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSessionStore sessions;

    /**
     * 中文说明：保存 events 对应的状态、依赖或配置值；字段类型为 {@code McpSubscriptionEventStore}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by events; its type is {@code McpSubscriptionEventStore}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSubscriptionEventStore events;

    /**
     * 中文说明：保存 authenticator 对应的状态、依赖或配置值；字段类型为 {@code IdentityAuthenticator}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by authenticator; its type is {@code IdentityAuthenticator}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final IdentityAuthenticator authenticator;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 会话Ttl 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by session ttl; its type is {@code Duration}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration sessionTtl;

    /**
     * 中文说明：保存 streamWait 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by stream wait; its type is {@code Duration}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration streamWait;

    /**
     * 中文说明：保存 maximumBodyBytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum body bytes; its type is {@code int}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int maximumBodyBytes;

    /**
     * 中文说明：保存 adapters 对应的状态、依赖或配置值；字段类型为 {@code Map<McpProtocolDialect, McpDialectAdapter>}，由 {@code McpEngineHttpHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by adapters; its type is {@code Map<McpProtocolDialect, McpDialectAdapter>}, and {@code McpEngineHttpHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<McpProtocolDialect, McpDialectAdapter> adapters;

    /**
     * 中文说明：创建 {@code McpEngineHttpHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpEngineHttpHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param dispatcher 参数 分发器；parameter dispatcher。
     * @param sessions 参数 sessions；parameter sessions。
     * @param events 参数 events；parameter events。
     * @param authenticator 参数 authenticator；parameter authenticator。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param clock 参数 clock；parameter clock。
     * @param sessionTtl 参数 会话Ttl；parameter session ttl。
     * @param streamWait 参数 streamWait；parameter stream wait。
     * @param maximumBodyBytes 参数 maximumBodyBytes；parameter maximum body bytes。
     */
    public McpEngineHttpHandler(
            Supplier<CompiledMcpRules> rules,
            McpMethodDispatcher dispatcher,
            McpSessionStore sessions,
            McpSubscriptionEventStore events,
            IdentityAuthenticator authenticator,
            ObjectMapper objectMapper,
            Clock clock,
            Duration sessionTtl,
            Duration streamWait,
            int maximumBodyBytes) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.events = Objects.requireNonNull(events, "events");
        this.authenticator = Objects.requireNonNull(
                authenticator,
                "authenticator"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sessionTtl = positive(sessionTtl, "sessionTtl");
        this.streamWait = positive(streamWait, "streamWait");
        if (maximumBodyBytes < 1) {
            throw new IllegalArgumentException(
                    "maximumBodyBytes must be positive"
            );
        }
        this.maximumBodyBytes = maximumBodyBytes;
        McpJsonRpcCodec codec = new McpJsonRpcCodec(objectMapper);
        this.adapters = Map.of(
                McpProtocolDialect.STABLE_2025_11_25,
                new StableMcpDialectAdapter(codec),
                McpProtocolDialect.RC_2026_07_28,
                new RcMcpDialectAdapter(codec),
                McpProtocolDialect.LEGACY_2024_SSE,
                new LegacySseMcpAdapter(codec)
        );
    }

    /**
     * 中文说明：执行 supports 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the supports operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.supports(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param uri 参数 uri；parameter uri。
     * @return 返回 supports 的处理结果；returns the result of the operation.
     */
    public boolean supports(String uri) {
        String path = path(uri);
        return path.startsWith(MCP_PATH_PREFIX)
                || path.startsWith(LEGACY_PATH_PREFIX)
                || path.startsWith(METADATA_PATH_PREFIX);
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    public Mono<McpHttpResponse> handle(McpHttpRequest request) {
        Objects.requireNonNull(request, "request");
        if (!supports(request.path())) {
            return Mono.just(error(404, "MCP_ENDPOINT_NOT_FOUND", null));
        }
        if (request.body().getBytes(StandardCharsets.UTF_8).length
                > maximumBodyBytes) {
            return Mono.just(error(413, "MCP_BODY_TOO_LARGE", null));
        }
        return Mono.defer(() -> route(request))
                .onErrorResume(
                        McpTransportException.class,
                        failure -> Mono.just(error(
                                failure.status,
                                failure.code,
                                failure.getMessage()
                        ))
                )
                .onErrorResume(
                        McpProtocolException.class,
                        failure -> Mono.just(protocolError(failure))
                )
                .onErrorResume(
                        failure -> Mono.just(error(
                                500,
                                "MCP_INTERNAL_ERROR",
                                "MCP request processing failed"
                        ))
                );
    }

    /**
     * 中文说明：执行 路由 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the route operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.route(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 路由 的处理结果；returns the result of the operation.
     */
    private Mono<McpHttpResponse> route(McpHttpRequest request) {
        String path = path(request.path());
        if (path.startsWith(METADATA_PATH_PREFIX)) {
            return metadata(request, serverCode(path, METADATA_PATH_PREFIX));
        }
        if (path.startsWith(LEGACY_PATH_PREFIX)) {
            return legacy(request, serverCode(path, LEGACY_PATH_PREFIX));
        }
        return current(request, serverCode(path, MCP_PATH_PREFIX));
    }

    /**
     * 中文说明：执行 元数据 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the metadata operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.metadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 元数据 的处理结果；returns the result of the operation.
     */
    private Mono<McpHttpResponse> metadata(
            McpHttpRequest request,
            String serverCode) {
        if (!"GET".equals(request.method())) {
            throw transport(405, "MCP_METHOD_NOT_ALLOWED", null);
        }
        McpRuntimeServer server = server(serverCode);
        return Mono.just(McpHttpResponse.json(
                200,
                json(Map.of(
                        "resource", MCP_PATH_PREFIX + server.serverCode(),
                        "resourceUri", server.resourceUri(),
                        "bearer_methods_supported", List.of("header")
                )),
                Map.of("cache-control", List.of("public, max-age=60"))
        ));
    }

    /**
     * 中文说明：执行 current 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.current(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 current 的处理结果；returns the result of the operation.
     */
    private Mono<McpHttpResponse> current(
            McpHttpRequest request,
            String serverCode) {
        McpRuntimeServer server = server(serverCode);
        if (isRc(request)) {
            return authenticate(request, server)
                    .flatMap(identity -> dispatch(
                            request,
                            server,
                            McpProtocolDialect.RC_2026_07_28,
                            null,
                            identity
                    ).map(response -> direct(response, Map.of()))
                            .defaultIfEmpty(McpHttpResponse.empty(202)));
        }
        return switch (request.method()) {
            case "POST" -> stablePost(request, server);
            case "GET" -> sessionStream(request, server, false);
            case "DELETE" -> deleteSession(request, server);
            default -> Mono.error(transport(
                    405,
                    "MCP_METHOD_NOT_ALLOWED",
                    null
            ));
        };
    }

    /**
     * 中文说明：执行 stablePost 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the stable post operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.stablePost(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param server 参数 服务器；parameter server。
     * @return 返回 stablePost 的处理结果；returns the result of the operation.
     */
    private Mono<McpHttpResponse> stablePost(
            McpHttpRequest request,
            McpRuntimeServer server) {
        return authenticate(request, server).flatMap(identity -> {
            McpJsonRpcRequest decoded = decode(
                    request,
                    McpProtocolDialect.STABLE_2025_11_25
            );
            if ("initialize".equals(decoded.method())) {
                if (decoded.notification()) {
                    throw new McpProtocolException(
                            McpErrorCode.MCP_INVALID_REQUEST,
                            "MCP initialize requires a request id"
                    );
                }
                if (request.header("Mcp-Session-Id") != null) {
                    throw transport(
                            400,
                            "MCP_SESSION_UNEXPECTED",
                            "initialize must not reuse an MCP session"
                    );
                }
                return createSession(server, identity).flatMap(session ->
                        dispatchDecoded(
                                decoded,
                                server,
                                McpProtocolDialect.STABLE_2025_11_25,
                                session.sessionId(),
                                identity
                        ).map(response -> direct(
                                response,
                                Map.of("mcp-session-id", List.of(
                                        session.sessionId()
                                ))
                        ))
                );
            }
            if (!McpProtocolDialect.STABLE_2025_11_25.protocolVersion()
                    .equals(request.header("Mcp-Protocol-Version"))) {
                throw new McpProtocolException(
                        McpErrorCode.MCP_PROTOCOL_UNSUPPORTED,
                        "MCP protocol version is not supported"
                );
            }
            return boundSession(request, server, identity)
                    .flatMap(session -> dispatchDecoded(
                            decoded,
                            server,
                            McpProtocolDialect.STABLE_2025_11_25,
                            session.sessionId(),
                            identity
                    ).flatMap(response -> appendResponse(
                            session.sessionId(),
                            response
                    ).thenReturn(direct(response, Map.of())))
                            .defaultIfEmpty(McpHttpResponse.empty(202)));
        });
    }

    /**
     * 中文说明：执行 legacy 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the legacy operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.legacy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 legacy 的处理结果；returns the result of the operation.
     */
    private Mono<McpHttpResponse> legacy(
            McpHttpRequest request,
            String serverCode) {
        McpRuntimeServer server = server(serverCode);
        requireDialect(server, McpProtocolDialect.LEGACY_2024_SSE);
        return switch (request.method()) {
            case "GET" -> authenticate(request, server)
                    .flatMap(identity -> createSession(server, identity)
                            .map(session -> legacyStream(
                                    request,
                                    server,
                                    session.sessionId()
                            )));
            case "POST" -> authenticate(request, server)
                    .flatMap(identity -> boundSession(
                                    request,
                                    server,
                                    identity
                            ).flatMap(session -> dispatch(
                                    request,
                                    server,
                                    McpProtocolDialect.LEGACY_2024_SSE,
                                    session.sessionId(),
                                    identity
                            ).flatMap(response -> appendResponse(
                                    session.sessionId(),
                                    response
                            )).thenReturn(McpHttpResponse.empty(202))));
            case "DELETE" -> deleteSession(request, server);
            default -> Mono.error(transport(
                    405,
                    "MCP_METHOD_NOT_ALLOWED",
                    null
            ));
        };
    }

    /**
     * 中文说明：执行 会话Stream 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the session stream operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.sessionStream(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param server 参数 服务器；parameter server。
     * @param legacy 参数 legacy；parameter legacy。
     * @return 返回 会话Stream 的处理结果；returns the result of the operation.
     */
    private Mono<McpHttpResponse> sessionStream(
            McpHttpRequest request,
            McpRuntimeServer server,
            boolean legacy) {
        return authenticate(request, server)
                .flatMap(identity -> boundSession(request, server, identity))
                .map(session -> stream(
                        session.sessionId(),
                        request.header("Last-Event-ID"),
                        legacy ? legacyEndpoint(server, session.sessionId())
                                : null
                ));
    }

    /**
     * 中文说明：执行 legacyStream 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the legacy stream operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.legacyStream(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param server 参数 服务器；parameter server。
     * @param sessionId 参数 会话Id；parameter session id。
     * @return 返回 legacyStream 的处理结果；returns the result of the operation.
     */
    private McpHttpResponse legacyStream(
            McpHttpRequest request,
            McpRuntimeServer server,
            String sessionId) {
        return stream(
                sessionId,
                request.header("Last-Event-ID"),
                legacyEndpoint(server, sessionId)
        );
    }

    /**
     * 中文说明：执行 stream 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the stream operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.stream(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionId 参数 会话Id；parameter session id。
     * @param afterEventId 参数 after事件Id；parameter after event id。
     * @param initialEvent 参数 initial事件；parameter initial event。
     * @return 返回 stream 的处理结果；returns the result of the operation.
     */
    private McpHttpResponse stream(
            String sessionId,
            String afterEventId,
            String initialEvent) {
        Flux<byte[]> initial = initialEvent == null
                ? Flux.empty()
                : Flux.just(initialEvent.getBytes(StandardCharsets.UTF_8));
        Flux<byte[]> eventStream = Flux.from(events.listen(
                        sessionId,
                        afterEventId,
                        streamWait
                ))
                .map(this::sse)
                .map(value -> value.getBytes(StandardCharsets.UTF_8));
        return new McpHttpResponse(
                200,
                Map.of(
                        "content-type", List.of("text/event-stream"),
                        "cache-control", List.of("no-cache, no-transform"),
                        "connection", List.of("keep-alive"),
                        "mcp-session-id", List.of(sessionId)
                ),
                initial.concatWith(eventStream),
                true
        );
    }

    /**
     * 中文说明：执行 delete会话 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete session operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.deleteSession(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param server 参数 服务器；parameter server。
     * @return 返回 delete会话 的处理结果；returns the result of the operation.
     */
    private Mono<McpHttpResponse> deleteSession(
            McpHttpRequest request,
            McpRuntimeServer server) {
        return authenticate(request, server)
                .flatMap(identity -> boundSession(request, server, identity))
                .flatMap(session -> Mono.from(sessions.delete(
                        session.sessionId()
                )))
                .thenReturn(McpHttpResponse.empty(204));
    }

    /**
     * 中文说明：执行 authenticate 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authenticate operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.authenticate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param server 参数 服务器；parameter server。
     * @return 返回 authenticate 的处理结果；returns the result of the operation.
     */
    private Mono<Map<String, Object>> authenticate(
            McpHttpRequest request,
            McpRuntimeServer server) {
        return Mono.from(authenticator.authenticate(request, server))
                .switchIfEmpty(Mono.error(transport(
                        401,
                        "MCP_UNAUTHENTICATED",
                        "MCP authentication is required"
                )));
    }

    /**
     * 中文说明：执行 create会话 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create session operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.createSession(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param server 参数 服务器；parameter server。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 create会话 的处理结果；returns the result of the operation.
     */
    private Mono<McpSessionStore.Session> createSession(
            McpRuntimeServer server,
            Map<String, Object> identity) {
        McpSessionStore.Session session = new McpSessionStore.Session(
                UUID.randomUUID().toString(),
                server.serverCode(),
                identity(identity, "callerId", "identity.subject"),
                identity(identity, "tenantId", "identity.tenant-id"),
                identity(
                        identity,
                        "idp.client-id",
                        "idp.audience",
                        "identity.client-id"
                ),
                clock.instant()
        );
        return Mono.from(sessions.create(session, sessionTtl))
                .thenReturn(session);
    }

    /**
     * 中文说明：执行 bound会话 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bound session operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.boundSession(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param server 参数 服务器；parameter server。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 bound会话 的处理结果；returns the result of the operation.
     */
    private Mono<McpSessionStore.Session> boundSession(
            McpHttpRequest request,
            McpRuntimeServer server,
            Map<String, Object> identity) {
        String sessionId = sessionId(request);
        if (sessionId == null) {
            return Mono.error(transport(
                    400,
                    "MCP_SESSION_REQUIRED",
                    "MCP session is required"
            ));
        }
        return Mono.from(sessions.find(sessionId))
                .switchIfEmpty(Mono.error(transport(
                        404,
                        "MCP_SESSION_NOT_FOUND",
                        "MCP session was not found"
                )))
                .flatMap(session -> {
                    boolean matches = session.serverCode().equals(
                            server.serverCode()
                    ) && session.subjectId().equals(identity(
                            identity,
                            "callerId",
                            "identity.subject"
                    )) && session.tenantId().equals(identity(
                            identity,
                            "tenantId",
                            "identity.tenant-id"
                    )) && session.clientId().equals(identity(
                            identity,
                            "idp.client-id",
                            "idp.audience",
                            "identity.client-id"
                    ));
                    if (!matches) {
                        return Mono.error(transport(
                                403,
                                "MCP_SESSION_BINDING_MISMATCH",
                                "MCP session is bound to another identity"
                        ));
                    }
                    return Mono.from(sessions.touch(sessionId, sessionTtl))
                            .thenReturn(session);
                });
    }

    /**
     * 中文说明：执行 dispatch 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dispatch operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.dispatch(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param server 参数 服务器；parameter server。
     * @param dialect 参数 dialect；parameter dialect。
     * @param sessionId 参数 会话Id；parameter session id。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 dispatch 的处理结果；returns the result of the operation.
     */
    private Mono<McpJsonRpcResponse> dispatch(
            McpHttpRequest request,
            McpRuntimeServer server,
            McpProtocolDialect dialect,
            String sessionId,
            Map<String, Object> identity) {
        return dispatchDecoded(
                decode(request, dialect),
                server,
                dialect,
                sessionId,
                identity
        );
    }

    /**
     * 中文说明：执行 dispatchDecoded 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dispatch decoded operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.dispatchDecoded(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param decoded 参数 decoded；parameter decoded。
     * @param server 参数 服务器；parameter server。
     * @param dialect 参数 dialect；parameter dialect。
     * @param sessionId 参数 会话Id；parameter session id。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 dispatchDecoded 的处理结果；returns the result of the operation.
     */
    private Mono<McpJsonRpcResponse> dispatchDecoded(
            McpJsonRpcRequest decoded,
            McpRuntimeServer server,
            McpProtocolDialect dialect,
            String sessionId,
            Map<String, Object> identity) {
        requireDialect(server, dialect);
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>(
                identity
        );
        return Flux.from(dispatcher.dispatch(
                        decoded,
                        new McpRequestContext(
                                server,
                                dialect,
                                sessionId,
                                Map.copyOf(attributes)
                        )
                ))
                .next();
    }

    /**
     * 中文说明：执行 decode 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.decode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param dialect 参数 dialect；parameter dialect。
     * @return 返回 decode 的处理结果；returns the result of the operation.
     */
    private McpJsonRpcRequest decode(
            McpHttpRequest request,
            McpProtocolDialect dialect) {
        return adapters.get(dialect).decode(new HttpMcpRequest(
                request.path(),
                request.method(),
                contentType(request),
                request.headers(),
                request.body()
        ));
    }

    /**
     * 中文说明：执行 append响应 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the append response operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.appendResponse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionId 参数 会话Id；parameter session id。
     * @param response 参数 响应；parameter response。
     * @return 返回 append响应 的处理结果；returns the result of the operation.
     */
    private Mono<McpSubscriptionEventStore.Event> appendResponse(
            String sessionId,
            McpJsonRpcResponse response) {
        return Mono.from(events.append(
                sessionId,
                "message",
                jsonRpc(response),
                sessionTtl
        ));
    }

    /**
     * 中文说明：执行 direct 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the direct operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.direct(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @param headers 参数 headers；parameter headers。
     * @return 返回 direct 的处理结果；returns the result of the operation.
     */
    private McpHttpResponse direct(
            McpJsonRpcResponse response,
            Map<String, List<String>> headers) {
        return McpHttpResponse.json(200, jsonRpc(response), headers);
    }

    /**
     * 中文说明：执行 protocolError 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the protocol error operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.protocolError(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     * @return 返回 protocolError 的处理结果；returns the result of the operation.
     */
    private McpHttpResponse protocolError(McpProtocolException failure) {
        int status = switch (failure.code()) {
            case MCP_UNAUTHENTICATED -> 401;
            case MCP_FORBIDDEN, MCP_APPROVAL_REQUIRED,
                 MCP_APPROVAL_MISMATCH, MCP_APPROVAL_CONSUMED -> 403;
            default -> 400;
        };
        return McpHttpResponse.json(
                status,
                jsonRpc(failure.toResponse(null)),
                Map.of()
        );
    }

    /**
     * 中文说明：执行 error 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the error operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.error(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @param code 参数 code；parameter code。
     * @param message 参数 消息；parameter message。
     * @return 返回 error 的处理结果；returns the result of the operation.
     */
    private McpHttpResponse error(
            int status,
            String code,
            String message) {
        LinkedHashMap<String, Object> content = new LinkedHashMap<>();
        content.put("error", code);
        if (message != null) {
            content.put("message", message);
        }
        return McpHttpResponse.json(status, json(content), Map.of());
    }

    /**
     * 中文说明：执行 服务器 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the server operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.server(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 服务器 的处理结果；returns the result of the operation.
     */
    private McpRuntimeServer server(String serverCode) {
        CompiledMcpRules active = rules.get();
        if (active == null) {
            throw transport(503, "MCP_RULES_UNAVAILABLE", null);
        }
        return active.server(serverCode)
                .filter(McpRuntimeServer::enabled)
                .orElseThrow(() -> transport(
                        404,
                        "MCP_SERVER_NOT_FOUND",
                        "MCP server was not found"
                ));
    }

    /**
     * 中文说明：执行 requireDialect 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require dialect operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.requireDialect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param server 参数 服务器；parameter server。
     * @param dialect 参数 dialect；parameter dialect。
     */
    private void requireDialect(
            McpRuntimeServer server,
            McpProtocolDialect dialect) {
        if (!server.dialects().contains(dialect)) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_PROTOCOL_UNSUPPORTED,
                    "MCP protocol version is not supported"
            );
        }
    }

    /**
     * 中文说明：执行 isRc 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is rc operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.isRc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 isRc 的处理结果；returns the result of the operation.
     */
    private boolean isRc(McpHttpRequest request) {
        return McpProtocolDialect.RC_2026_07_28.protocolVersion().equals(
                request.header("Mcp-Protocol-Version")
        );
    }

    /**
     * 中文说明：执行 会话Id 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the session id operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.sessionId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 会话Id 的处理结果；returns the result of the operation.
     */
    private String sessionId(McpHttpRequest request) {
        String header = request.header("Mcp-Session-Id");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String query = query(request.path());
        if (query == null) {
            return null;
        }
        for (String parameter : query.split("&")) {
            int equals = parameter.indexOf('=');
            if (equals > 0 && "sessionId".equals(parameter.substring(0, equals))) {
                String value = parameter.substring(equals + 1);
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }

    /**
     * 中文说明：执行 contentType 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the content type operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.contentType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 contentType 的处理结果；returns the result of the operation.
     */
    private String contentType(McpHttpRequest request) {
        String contentType = request.header("content-type");
        return contentType == null ? "application/json" : contentType;
    }

    /**
     * 中文说明：执行 sse 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sse operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.sse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     * @return 返回 sse 的处理结果；returns the result of the operation.
     */
    private String sse(McpSubscriptionEventStore.Event event) {
        return "id:" + event.eventId() + "\n"
                + "event:" + event.type() + "\n"
                + "data:" + event.data() + "\n\n";
    }

    /**
     * 中文说明：执行 legacyEndpoint 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the legacy endpoint operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.legacyEndpoint(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param server 参数 服务器；parameter server。
     * @param sessionId 参数 会话Id；parameter session id。
     * @return 返回 legacyEndpoint 的处理结果；returns the result of the operation.
     */
    private String legacyEndpoint(
            McpRuntimeServer server,
            String sessionId) {
        return "event:endpoint\n"
                + "data:" + LEGACY_PATH_PREFIX + server.serverCode()
                + "?sessionId=" + sessionId + "\n\n";
    }

    /**
     * 中文说明：执行 身份 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param identity 参数 身份；parameter identity。
     * @param names 参数 names；parameter names。
     * @return 返回 身份 的处理结果；returns the result of the operation.
     */
    private String identity(
            Map<String, Object> identity,
            String... names) {
        for (String name : names) {
            Object value = identity.get(name);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        throw transport(
                401,
                "MCP_IDENTITY_INCOMPLETE",
                "MCP identity context is incomplete"
        );
    }

    /**
     * 中文说明：执行 json 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.json(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 json 的处理结果；returns the result of the operation.
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("MCP JSON serialization failed");
        }
    }

    /**
     * 中文说明：执行 jsonRpc 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json rpc operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.jsonRpc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @return 返回 jsonRpc 的处理结果；returns the result of the operation.
     */
    private String jsonRpc(McpJsonRpcResponse response) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("jsonrpc", response.jsonrpc());
        value.put("id", response.id());
        if (response.error() == null) {
            value.put("result", response.result());
        } else {
            value.put("error", response.error());
        }
        return json(value);
    }

    /**
     * 中文说明：执行 服务器Code 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the server code operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.serverCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param path 参数 path；parameter path。
     * @param prefix 参数 prefix；parameter prefix。
     * @return 返回 服务器Code 的处理结果；returns the result of the operation.
     */
    private String serverCode(String path, String prefix) {
        String value = path.substring(prefix.length());
        if (value.isBlank() || value.contains("/")) {
            throw transport(404, "MCP_ENDPOINT_NOT_FOUND", null);
        }
        return value;
    }

    /**
     * 中文说明：执行 path 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the path operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.path(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param uri 参数 uri；parameter uri。
     * @return 返回 path 的处理结果；returns the result of the operation.
     */
    private static String path(String uri) {
        int query = uri.indexOf('?');
        return query < 0 ? uri : uri.substring(0, query);
    }

    /**
     * 中文说明：执行 query 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the query operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.query(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param uri 参数 uri；parameter uri。
     * @return 返回 query 的处理结果；returns the result of the operation.
     */
    private static String query(String uri) {
        int query = uri.indexOf('?');
        return query < 0 || query == uri.length() - 1
                ? null
                : uri.substring(query + 1);
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /**
     * 中文说明：执行 传输 操作；该方法是 {@code McpEngineHttpHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transport operation; this method is the invocation entry point on {@code McpEngineHttpHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.transport(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @param code 参数 code；parameter code。
     * @param message 参数 消息；parameter message。
     * @return 返回 传输 的处理结果；returns the result of the operation.
     */
    private static McpTransportException transport(
            int status,
            String code,
            String message) {
        return new McpTransportException(status, code, message);
    }

    /**
     * 中文说明：{@code IdentityAuthenticator} 是接口契约，位于当前 Gateway 模块的相关包中，负责身份Authenticator相关的职责与边界。
     * English summary: {@code IdentityAuthenticator} is an interface contract in the current Gateway module; it owns the identity authenticator-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    public interface IdentityAuthenticator {

        /**
         * 中文说明：执行 authenticate 操作；该方法是 {@code McpEngineHttpHandler.IdentityAuthenticator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the authenticate operation; this method is the invocation entry point on {@code McpEngineHttpHandler.IdentityAuthenticator} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpEngineHttpHandler.IdentityAuthenticator.authenticate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param request 参数 请求；parameter request。
         * @param server 参数 服务器；parameter server。
         * @return 返回 authenticate 的处理结果；returns the result of the operation.
         */
        Publisher<Map<String, Object>> authenticate(
                McpHttpRequest request,
                McpRuntimeServer server
        );
    }

    /**
     * 中文说明：{@code McpTransportException} 是异常类型，位于当前 Gateway 模块的相关包中，负责MCP传输Exception相关的职责与边界。
     * English summary: {@code McpTransportException} is a mcp transport exception exception in the current Gateway module; it owns the mcp transport exception-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class McpTransportException
            extends RuntimeException {

        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpEngineHttpHandler.McpTransportException} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code int}, and {@code McpEngineHttpHandler.McpTransportException} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler.McpTransportException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler.McpTransportException}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final int status;

        /**
         * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpEngineHttpHandler.McpTransportException} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code McpEngineHttpHandler.McpTransportException} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpEngineHttpHandler.McpTransportException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpEngineHttpHandler.McpTransportException}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String code;

        /**
         * 中文说明：创建 {@code McpEngineHttpHandler.McpTransportException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpEngineHttpHandler.McpTransportException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param status 参数 status；parameter status。
         * @param code 参数 code；parameter code。
         * @param message 参数 消息；parameter message。
         */
        private McpTransportException(
                int status,
                String code,
                String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }
}
