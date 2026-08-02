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
import top.egon.cola.component.gateway.mcp.protocol.HttpMcpRequest;
import top.egon.cola.component.gateway.mcp.protocol.LegacySseMcpAdapter;
import top.egon.cola.component.gateway.mcp.protocol.McpDialectAdapter;
import top.egon.cola.component.gateway.mcp.protocol.McpJsonRpcCodec;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.protocol.RcMcpDialectAdapter;
import top.egon.cola.component.gateway.mcp.protocol.StableMcpDialectAdapter;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.server.McpMethodDispatcher;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;
import top.egon.cola.component.gateway.mcp.transport.McpHttpRequest;
import top.egon.cola.component.gateway.mcp.transport.McpHttpResponse;
import top.egon.cola.component.gateway.mcp.transport.McpSessionStore;
import top.egon.cola.component.gateway.mcp.transport.McpSubscriptionEventStore;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * MCP HTTP front controller for Stable, RC and legacy SSE transports.
 */
public final class McpEngineHttpHandler {

    public static final String MCP_PATH_PREFIX = "/mcp/";

    public static final String LEGACY_PATH_PREFIX = "/legacy/mcp/";

    public static final String METADATA_PATH_PREFIX =
            "/.well-known/oauth-protected-resource/mcp/";

    private final Supplier<CompiledMcpRules> rules;

    private final McpMethodDispatcher dispatcher;

    private final McpSessionStore sessions;

    private final McpSubscriptionEventStore events;

    private final IdentityAuthenticator authenticator;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final Duration sessionTtl;

    private final Duration streamWait;

    private final int maximumBodyBytes;

    private final Map<McpProtocolDialect, McpDialectAdapter> adapters;

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

    public boolean supports(String uri) {
        String path = path(uri);
        return path.startsWith(MCP_PATH_PREFIX)
                || path.startsWith(LEGACY_PATH_PREFIX)
                || path.startsWith(METADATA_PATH_PREFIX);
    }

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
                        "audience", server.oauthAudience(),
                        "bearer_methods_supported", List.of("header")
                )),
                Map.of("cache-control", List.of("public, max-age=60"))
        ));
    }

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

    private Mono<McpSessionStore.Session> createSession(
            McpRuntimeServer server,
            Map<String, Object> identity) {
        McpSessionStore.Session session = new McpSessionStore.Session(
                UUID.randomUUID().toString(),
                server.serverCode(),
                identity(identity, "callerId", "identity.subject"),
                identity(identity, "tenantId", "identity.tenant-id"),
                identity(identity, "idp.client-id", "identity.client-id"),
                clock.instant()
        );
        return Mono.from(sessions.create(session, sessionTtl))
                .thenReturn(session);
    }

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

    private McpHttpResponse direct(
            McpJsonRpcResponse response,
            Map<String, List<String>> headers) {
        return McpHttpResponse.json(200, jsonRpc(response), headers);
    }

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

    private boolean isRc(McpHttpRequest request) {
        return McpProtocolDialect.RC_2026_07_28.protocolVersion().equals(
                request.header("Mcp-Protocol-Version")
        );
    }

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

    private String contentType(McpHttpRequest request) {
        String contentType = request.header("content-type");
        return contentType == null ? "application/json" : contentType;
    }

    private String sse(McpSubscriptionEventStore.Event event) {
        return "id:" + event.eventId() + "\n"
                + "event:" + event.type() + "\n"
                + "data:" + event.data() + "\n\n";
    }

    private String legacyEndpoint(
            McpRuntimeServer server,
            String sessionId) {
        return "event:endpoint\n"
                + "data:" + LEGACY_PATH_PREFIX + server.serverCode()
                + "?sessionId=" + sessionId + "\n\n";
    }

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

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("MCP JSON serialization failed");
        }
    }

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

    private String serverCode(String path, String prefix) {
        String value = path.substring(prefix.length());
        if (value.isBlank() || value.contains("/")) {
            throw transport(404, "MCP_ENDPOINT_NOT_FOUND", null);
        }
        return value;
    }

    private static String path(String uri) {
        int query = uri.indexOf('?');
        return query < 0 ? uri : uri.substring(0, query);
    }

    private static String query(String uri) {
        int query = uri.indexOf('?');
        return query < 0 || query == uri.length() - 1
                ? null
                : uri.substring(query + 1);
    }

    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static McpTransportException transport(
            int status,
            String code,
            String message) {
        return new McpTransportException(status, code, message);
    }

    @FunctionalInterface
    public interface IdentityAuthenticator {

        Publisher<Map<String, Object>> authenticate(
                McpHttpRequest request,
                McpRuntimeServer server
        );
    }

    private static final class McpTransportException
            extends RuntimeException {

        private final int status;

        private final String code;

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
