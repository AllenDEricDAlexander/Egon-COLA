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
 * 补充说明 / Supplementary summary: {@code McpGatewayIdentityAuthenticator} 是类型，位于当前 Gateway 模块的相关包中，负责MCP网关身份Authenticator相关的职责与边界。
 * English supplement: {@code McpGatewayIdentityAuthenticator} is a type in the current Gateway module; it owns the mcp gateway identity authenticator-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpGatewayIdentityAuthenticator
        implements McpEngineHttpHandler.IdentityAuthenticator {

    /**
     * 中文说明：表示 身份ONLY策略 这一固定值；它属于 {@code McpGatewayIdentityAuthenticator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value identity only policy; it is a state, type, or protocol value of {@code McpGatewayIdentityAuthenticator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator}; do not couple callers to its representation when the owning type exposes an API.
     */
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

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code GatewaySecurityChain}，由 {@code McpGatewayIdentityAuthenticator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code GatewaySecurityChain}, and {@code McpGatewayIdentityAuthenticator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewaySecurityChain security;

    /**
     * 中文说明：保存 issuer 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpGatewayIdentityAuthenticator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by issuer; its type is {@code String}, and {@code McpGatewayIdentityAuthenticator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String issuer;

    /**
     * 中文说明：保存 引擎NodeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpGatewayIdentityAuthenticator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by engine node id; its type is {@code String}, and {@code McpGatewayIdentityAuthenticator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String engineNodeId;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code McpGatewayIdentityAuthenticator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code McpGatewayIdentityAuthenticator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code McpGatewayIdentityAuthenticator} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpGatewayIdentityAuthenticator} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param security 参数 安全；parameter security。
     * @param issuer 参数 issuer；parameter issuer。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param clock 参数 clock；parameter clock。
     */
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

    /**
     * 中文说明：执行 authenticate 操作；该方法是 {@code McpGatewayIdentityAuthenticator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authenticate operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.authenticate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param server 参数 服务器；parameter server。
     * @return 返回 authenticate 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 身份 操作；该方法是 {@code McpGatewayIdentityAuthenticator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param principal 参数 principal；parameter principal。
     * @param bearer 参数 bearer；parameter bearer。
     * @param request 参数 请求；parameter request。
     * @return 返回 身份 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 安全Attributes 操作；该方法是 {@code McpGatewayIdentityAuthenticator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the security attributes operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.securityAttributes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param server 参数 服务器；parameter server。
     * @return 返回 安全Attributes 的处理结果；returns the result of the operation.
     */
    static Map<String, String> securityAttributes(McpRuntimeServer server) {
        return Map.of("idp.resource-uri", server.resourceUri());
    }

    /**
     * 中文说明：执行 copyHeader 操作；该方法是 {@code McpGatewayIdentityAuthenticator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy header operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.copyHeader(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param target 参数 target；parameter target。
     * @param name 参数 name；parameter name。
     */
    private void copyHeader(
            McpHttpRequest request,
            Map<String, Object> target,
            String name) {
        String value = request.header(name);
        if (value != null && !value.isBlank()) {
            target.put(name, value.trim());
        }
    }

    /**
     * 中文说明：执行 headers 操作；该方法是 {@code McpGatewayIdentityAuthenticator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the headers operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.headers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 headers 的处理结果；returns the result of the operation.
     */
    private Map<String, List<String>> headers(McpHttpRequest request) {
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        request.headers().forEach((name, value) -> result.put(
                name,
                List.of(value)
        ));
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 accessZone 操作；该方法是 {@code McpGatewayIdentityAuthenticator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the access zone operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.accessZone(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 accessZone 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 远程Address 操作；该方法是 {@code McpGatewayIdentityAuthenticator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote address operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.remoteAddress(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 远程Address 的处理结果；returns the result of the operation.
     */
    private String remoteAddress(McpHttpRequest request) {
        Object value = request.attributes().get("remoteAddress");
        return value == null || String.valueOf(value).isBlank()
                ? "unknown"
                : String.valueOf(value).trim();
    }

    /**
     * 中文说明：执行 traceId 操作；该方法是 {@code McpGatewayIdentityAuthenticator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace id operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.traceId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param fallback 参数 fallback；parameter fallback。
     * @return 返回 traceId 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpGatewayIdentityAuthenticator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 中文说明：{@code McpGatewayRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MCP网关请求相关的职责与边界。
     * English summary: {@code McpGatewayRequest} is an immutable data carrier in the current Gateway module; it owns the mcp gateway request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param requestId 参数 请求Id；parameter request id。
     * @param traceId 参数 traceId；parameter trace id。
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param rawHeaders 参数 rawHeaders；parameter raw headers。
     * @param contentLength 参数 contentLength；parameter content length。
     */
    private record McpGatewayRequest(
            /**
             * 中文说明：保存 请求Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request id; its type is {@code String}, and {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator.McpGatewayRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String requestId,
            /**
             * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator.McpGatewayRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String traceId,
            /**
             * 中文说明：保存 accessZone 对应的状态、依赖或配置值；字段类型为 {@code AccessZone}，由 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by access zone; its type is {@code AccessZone}, and {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator.McpGatewayRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            AccessZone accessZone,
            /**
             * 中文说明：保存 rawHeaders 对应的状态、依赖或配置值；字段类型为 {@code Map<String, List<String>>}，由 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by raw headers; its type is {@code Map<String, List<String>>}, and {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator.McpGatewayRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, List<String>> rawHeaders,
            /**
             * 中文说明：保存 contentLength 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content length; its type is {@code long}, and {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator.McpGatewayRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            long contentLength
    ) implements GatewayRequest {

        /**
         * 中文说明：执行 protocol 操作；该方法是 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the protocol operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.McpGatewayRequest.protocol(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 protocol 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayProtocol protocol() {
            return GatewayProtocol.HTTP;
        }

        /**
         * 中文说明：执行 headers 操作；该方法是 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the headers operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.McpGatewayRequest.headers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 headers 的处理结果；returns the result of the operation.
         */
        @Override
        public ImmutableGatewayHeaders headers() {
            return new ImmutableGatewayHeaders(rawHeaders);
        }

        /**
         * 中文说明：执行 body 操作；该方法是 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the body operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.McpGatewayRequest.body(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 body 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayBody body() {
            return new GatewayBody() {
                /**
                 * 中文说明：执行 contentLength 操作；该方法是 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the content length operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.McpGatewayRequest.contentLength(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @return 返回 contentLength 的处理结果；returns the result of the operation.
                 */
                @Override
                public long contentLength() {
                    return contentLength;
                }

                /**
                 * 中文说明：执行 replayable 操作；该方法是 {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the replayable operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator.McpGatewayRequest} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.McpGatewayRequest.replayable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @return 返回 replayable 的处理结果；returns the result of the operation.
                 */
                @Override
                public boolean replayable() {
                    return true;
                }
            };
        }
    }

    /**
     * 中文说明：{@code McpGatewayExchange} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MCP网关Exchange相关的职责与边界。
     * English summary: {@code McpGatewayExchange} is an immutable data carrier in the current Gateway module; it owns the mcp gateway exchange-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     */
    private record McpGatewayExchange(
            /**
             * 中文说明：保存 请求 对应的状态、依赖或配置值；字段类型为 {@code GatewayRequest}，由 {@code McpGatewayIdentityAuthenticator.McpGatewayExchange} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request; its type is {@code GatewayRequest}, and {@code McpGatewayIdentityAuthenticator.McpGatewayExchange} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator.McpGatewayExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator.McpGatewayExchange}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayRequest request,
            /**
             * 中文说明：保存 context 对应的状态、依赖或配置值；字段类型为 {@code GatewayContext}，由 {@code McpGatewayIdentityAuthenticator.McpGatewayExchange} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by context; its type is {@code GatewayContext}, and {@code McpGatewayIdentityAuthenticator.McpGatewayExchange} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpGatewayIdentityAuthenticator.McpGatewayExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpGatewayIdentityAuthenticator.McpGatewayExchange}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayContext context
    ) implements GatewayExchange {

        /**
         * 中文说明：执行 响应 操作；该方法是 {@code McpGatewayIdentityAuthenticator.McpGatewayExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the response operation; this method is the invocation entry point on {@code McpGatewayIdentityAuthenticator.McpGatewayExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpGatewayIdentityAuthenticator.McpGatewayExchange.response(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 响应 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayResponse response() {
            return DefaultGatewayResponse.success(EmptyGatewayBody.INSTANCE);
        }
    }
}
