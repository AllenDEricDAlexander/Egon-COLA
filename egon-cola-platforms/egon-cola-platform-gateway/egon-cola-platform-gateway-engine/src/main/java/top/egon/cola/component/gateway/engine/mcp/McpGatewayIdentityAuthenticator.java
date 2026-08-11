package top.egon.cola.component.gateway.engine.mcp;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayContext;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.context.GatewayStage;
import top.egon.cola.component.gateway.core.exchange.DefaultGatewayResponse;
import top.egon.cola.component.gateway.core.exchange.EmptyGatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayRequest;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.exchange.ImmutableGatewayHeaders;
import top.egon.cola.component.gateway.core.security.AuthenticationMode;
import top.egon.cola.component.gateway.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.gateway.core.security.CredentialForwardingMode;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityFailureMode;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityChain;
import top.egon.cola.component.gateway.mcp.transport.McpHttpRequest;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Adapts the fixed IdP identity-only Gateway security chain to MCP ingress.
 */
public final class McpGatewayIdentityAuthenticator
        implements McpEngineHttpHandler.IdentityAuthenticator {

    private static final GatewaySecurityPolicy IDENTITY_ONLY_POLICY =
            new GatewaySecurityPolicy(
                    "gateway-mcp-idp",
                    AuthenticationMode.REQUIRED,
                    List.of("idp-bearer"),
                    List.of("idp-jwt"),
                    List.of(),
                    AuthorizationDecisionMode.ALL_ALLOW,
                    null,
                    Duration.ofSeconds(3),
                    SecurityFailureMode.FAIL_CLOSED,
                    CredentialForwardingMode.ORIGINAL_BEARER
            );

    private final GatewaySecurityChain security;

    private final String issuer;

    private final String engineNodeId;

    private final Clock clock;

    public McpGatewayIdentityAuthenticator(
            GatewaySecurityChain security,
            String issuer,
            String engineNodeId,
            Clock clock) {
        this.security = Objects.requireNonNull(security, "security");
        this.issuer = required(issuer, "issuer");
        this.engineNodeId = required(engineNodeId, "engineNodeId");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Mono<Map<String, Object>> authenticate(
            McpHttpRequest request,
            McpRuntimeServer server) {
        Instant startedAt = clock.instant();
        Instant deadline = startedAt.plus(
                IDENTITY_ONLY_POLICY.providerTimeout()
        );
        String requestId = UUID.randomUUID().toString();
        String traceId = traceId(request, requestId);
        AccessZone accessZone = accessZone(request);
        GatewayPrincipal anonymous = GatewayPrincipal.anonymous();
        GatewayAuthContext auth = new GatewayAuthContext(
                accessZone,
                GatewayProtocol.HTTP,
                "gateway.mcp." + server.serverCode(),
                null,
                IDENTITY_ONLY_POLICY.policyId(),
                request.path(),
                request.method(),
                Set.of(),
                anonymous,
                remoteAddress(request),
                traceId,
                requestId,
                deadline,
                "mcp-runtime",
                securityAttributes(server)
        );
        GatewayContext context = new GatewayContext(
                requestId,
                traceId,
                request.header("traceparent"),
                request.header("tracestate"),
                accessZone,
                "gateway",
                engineNodeId,
                "gateway.mcp." + server.serverCode(),
                null,
                "mcp-runtime",
                anonymous,
                null,
                deadline,
                startedAt,
                GatewayStage.ROUTE_MATCHED,
                List.of(),
                List.of()
        );
        GatewayExchange exchange = new McpGatewayExchange(
                new McpGatewayRequest(
                        requestId,
                        traceId,
                        accessZone,
                        headers(request),
                        request.body().getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        ).length
                ),
                context
        );
        return security.execute(
                        exchange,
                        auth,
                        IDENTITY_ONLY_POLICY,
                        GatewayProtocol.HTTP
                )
                .map(result -> identity(
                        result.context().principal(),
                        result.forwardingCredential() == null
                                ? null
                                : result.forwardingCredential()
                                .tokenReference(),
                        request
                ))
                .onErrorResume(ignored -> Mono.empty());
    }

    private Map<String, Object> identity(
            GatewayPrincipal principal,
            String bearer,
            McpHttpRequest request) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("identity.issuer", issuer);
        result.put("identity.subject", principal.principalId());
        result.put("callerId", principal.principalId());
        result.put("identity.tenant-id", principal.tenantId());
        result.put("tenantId", principal.tenantId());
        principal.attributes().forEach(result::put);
        if (bearer != null) {
            result.put("originalBearerToken", "Bearer " + bearer);
        }
        copyHeader(request, result, "traceparent");
        copyHeader(request, result, "tracestate");
        copyHeader(request, result, "x-egon-request-id");
        result.put("clientIp", remoteAddress(request));
        return Map.copyOf(result);
    }

    static Map<String, String> securityAttributes(McpRuntimeServer server) {
        return Map.of("idp.resource-uri", server.resourceUri());
    }

    private void copyHeader(
            McpHttpRequest request,
            Map<String, Object> target,
            String name) {
        String value = request.header(name);
        if (value != null && !value.isBlank()) {
            target.put(name, value.trim());
        }
    }

    private Map<String, List<String>> headers(McpHttpRequest request) {
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        request.headers().forEach((name, value) -> result.put(
                name,
                List.of(value)
        ));
        return Map.copyOf(result);
    }

    private AccessZone accessZone(McpHttpRequest request) {
        Object value = request.attributes().get("accessZone");
        try {
            return value == null
                    ? AccessZone.PUBLIC
                    : AccessZone.valueOf(String.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            return AccessZone.PUBLIC;
        }
    }

    private String remoteAddress(McpHttpRequest request) {
        Object value = request.attributes().get("remoteAddress");
        return value == null || String.valueOf(value).isBlank()
                ? "unknown"
                : String.valueOf(value).trim();
    }

    private String traceId(McpHttpRequest request, String fallback) {
        String traceparent = request.header("traceparent");
        if (traceparent == null) {
            return fallback;
        }
        String[] parts = traceparent.split("-");
        return parts.length >= 2 && !parts[1].isBlank()
                ? parts[1]
                : fallback;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private record McpGatewayRequest(
            String requestId,
            String traceId,
            AccessZone accessZone,
            Map<String, List<String>> rawHeaders,
            long contentLength
    ) implements GatewayRequest {

        @Override
        public GatewayProtocol protocol() {
            return GatewayProtocol.HTTP;
        }

        @Override
        public ImmutableGatewayHeaders headers() {
            return new ImmutableGatewayHeaders(rawHeaders);
        }

        @Override
        public GatewayBody body() {
            return new GatewayBody() {
                @Override
                public long contentLength() {
                    return contentLength;
                }

                @Override
                public boolean replayable() {
                    return true;
                }
            };
        }
    }

    private record McpGatewayExchange(
            GatewayRequest request,
            GatewayContext context
    ) implements GatewayExchange {

        @Override
        public GatewayResponse response() {
            return DefaultGatewayResponse.success(EmptyGatewayBody.INSTANCE);
        }
    }
}
