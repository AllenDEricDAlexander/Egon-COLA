package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Mono;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayContext;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.context.GatewayStage;
import top.egon.cola.component.gateway.core.exchange.DefaultGatewayResponse;
import top.egon.cola.component.gateway.core.exchange.EmptyGatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayHeaders;
import top.egon.cola.component.gateway.core.exchange.GatewayRequest;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.exchange.ImmutableGatewayHeaders;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityChain;
import top.egon.cola.component.gateway.engine.security.TrustedClientAddressResolver;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 中文说明：{@code RuleBackedHttpGatewaySecurityProcessor} 是类型，位于当前 Gateway 模块的相关包中，负责规则BackedHttp网关安全Processor相关的职责与边界。
 * English summary: {@code RuleBackedHttpGatewaySecurityProcessor} is a type in the current Gateway module; it owns the rule backed http gateway security processor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RuleBackedHttpGatewaySecurityProcessor
        implements GatewayHttpSecurityProcessor {

    /**
     * 中文说明：保存 chain 对应的状态、依赖或配置值；字段类型为 {@code GatewaySecurityChain}，由 {@code RuleBackedHttpGatewaySecurityProcessor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by chain; its type is {@code GatewaySecurityChain}, and {@code RuleBackedHttpGatewaySecurityProcessor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewaySecurityChain chain;

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledGatewayRules>}，由 {@code RuleBackedHttpGatewaySecurityProcessor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledGatewayRules>}, and {@code RuleBackedHttpGatewaySecurityProcessor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledGatewayRules> rules;

    /**
     * 中文说明：保存 客户端AddressResolver 对应的状态、依赖或配置值；字段类型为 {@code TrustedClientAddressResolver}，由 {@code RuleBackedHttpGatewaySecurityProcessor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client address resolver; its type is {@code TrustedClientAddressResolver}, and {@code RuleBackedHttpGatewaySecurityProcessor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final TrustedClientAddressResolver clientAddressResolver;

    /**
     * 中文说明：保存 引擎NodeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuleBackedHttpGatewaySecurityProcessor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by engine node id; its type is {@code String}, and {@code RuleBackedHttpGatewaySecurityProcessor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String engineNodeId;

    /**
     * 中文说明：创建 {@code RuleBackedHttpGatewaySecurityProcessor} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RuleBackedHttpGatewaySecurityProcessor} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param chain 参数 chain；parameter chain。
     * @param rules 参数 rules；parameter rules。
     * @param clientAddressResolver 参数 客户端AddressResolver；parameter client address resolver。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     */
    public RuleBackedHttpGatewaySecurityProcessor(
            GatewaySecurityChain chain,
            Supplier<CompiledGatewayRules> rules,
            TrustedClientAddressResolver clientAddressResolver,
            String engineNodeId) {
        this.chain = Objects.requireNonNull(chain, "chain");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.clientAddressResolver = Objects.requireNonNull(
                clientAddressResolver,
                "clientAddressResolver"
        );
        if (engineNodeId == null || engineNodeId.isBlank()) {
            throw new IllegalArgumentException("engineNodeId is required");
        }
        this.engineNodeId = engineNodeId.trim();
    }

    /**
     * 中文说明：执行 authorize 操作；该方法是 {@code RuleBackedHttpGatewaySecurityProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize operation; this method is the invocation entry point on {@code RuleBackedHttpGatewaySecurityProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedHttpGatewaySecurityProcessor.authorize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param request 参数 请求；parameter request。
     * @param normalized 参数 normalized；parameter normalized。
     * @param route 参数 路由；parameter route。
     * @param traceId 参数 traceId；parameter trace id。
     * @return 返回 authorize 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Outcome> authorize(
            AccessZone accessZone,
            GatewayInboundHttpRequest request,
            NormalizedHttpRequest normalized,
            HttpRouteMatch route,
            String traceId) {
        CompiledGatewayRules current = rules.get();
        List<GatewaySecurityPolicy> policies = route.route().policyRefs()
                .stream()
                .map(current.securityPolicies()::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        GatewaySecurityPolicy::policyId
                ))
                .toList();
        if (policies.isEmpty()) {
            return Mono.just(Outcome.anonymous());
        }
        if (policies.size() != 1) {
            return Mono.error(new IllegalArgumentException(
                    "an operation must reference exactly one security policy"
            ));
        }
        GatewaySecurityPolicy policy = policies.getFirst();
        String requestId = UuidV7.simpleString();
        Instant startedAt = Instant.now();
        Instant deadline = startedAt.plus(policy.providerTimeout());
        String remoteAddress = clientAddressResolver.resolve(
                request.remoteAddress(),
                request.headers()
        ).getHostAddress();
        GatewayContext gatewayContext = new GatewayContext(
                requestId,
                traceId,
                null,
                null,
                accessZone,
                route.route().gatewayGroupId(),
                engineNodeId,
                route.route().operationId(),
                route.route().routeId(),
                current.snapshot().releaseId(),
                GatewayPrincipal.anonymous(),
                null,
                deadline,
                startedAt,
                GatewayStage.ROUTE_MATCHED,
                List.of(),
                List.of()
        );
        GatewayAuthContext authContext = new GatewayAuthContext(
                accessZone,
                GatewayProtocol.HTTP,
                route.route().operationId(),
                route.route().routeId(),
                policy.policyId(),
                normalized.normalizedPath(),
                normalized.method(),
                java.util.Set.of(),
                GatewayPrincipal.anonymous(),
                remoteAddress,
                traceId,
                requestId,
                deadline,
                current.snapshot().releaseId(),
                securityAttributes(route)
        );
        GatewayExchange exchange = new HttpExchange(
                new HttpRequest(
                        requestId,
                        traceId,
                        accessZone,
                        new ImmutableGatewayHeaders(normalized.headers()),
                        contentLength(normalized.headers())
                ),
                gatewayContext
        );
        return chain.execute(
                        exchange,
                        authContext,
                        policy,
                        GatewayProtocol.HTTP
                )
                .map(result -> new Outcome(
                        result.trustedIdentity(),
                        result.fieldsToRemove(),
                        result.forwardingCredential()
                ));
    }

    /**
     * 中文说明：执行 安全Attributes 操作；该方法是 {@code RuleBackedHttpGatewaySecurityProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the security attributes operation; this method is the invocation entry point on {@code RuleBackedHttpGatewaySecurityProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedHttpGatewaySecurityProcessor.securityAttributes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     * @return 返回 安全Attributes 的处理结果；returns the result of the operation.
     */
    static java.util.Map<String, String> securityAttributes(HttpRouteMatch route) {
        java.util.Map<String, String> metadata = route.route().metadata();
        java.util.Map<String, String> attributes = new java.util.LinkedHashMap<>();
        ProviderServiceKey upstream = route.route().upstream();
        attributes.put("idp.biz-code", upstream.bizCode());
        attributes.put("idp.app-code", upstream.appCode());
        attributes.put("idp.env", upstream.env());
        copy(metadata, attributes, "applicationCode", "rbac3.application-code");
        copy(metadata, attributes, "definitionSetId", "rbac3.definition-set-id");
        if (!attributes.containsKey("rbac3.definition-set-id")) {
            copy(metadata, attributes, "gateway.definition-set-id",
                    "rbac3.definition-set-id");
        }
        copy(metadata, attributes, "mappingVersion", "rbac3.mapping-version");
        if (!attributes.containsKey("rbac3.mapping-version")) {
            copy(metadata, attributes, "publishedVersion", "rbac3.mapping-version");
        }
        if (!attributes.containsKey("rbac3.mapping-version")) {
            copy(metadata, attributes, "definitionVersion", "rbac3.mapping-version");
        }
        return java.util.Map.copyOf(attributes);
    }

    /**
     * 中文说明：执行 copy 操作；该方法是 {@code RuleBackedHttpGatewaySecurityProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy operation; this method is the invocation entry point on {@code RuleBackedHttpGatewaySecurityProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedHttpGatewaySecurityProcessor.copy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param target 参数 target；parameter target。
     * @param sourceName 参数 sourceName；parameter source name。
     * @param targetName 参数 targetName；parameter target name。
     */
    private static void copy(
            java.util.Map<String, String> source,
            java.util.Map<String, String> target,
            String sourceName,
            String targetName
    ) {
        String value = source.get(sourceName);
        if (value != null && !value.isBlank()) {
            target.put(targetName, value.trim());
        }
    }

    /**
     * 中文说明：执行 contentLength 操作；该方法是 {@code RuleBackedHttpGatewaySecurityProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the content length operation; this method is the invocation entry point on {@code RuleBackedHttpGatewaySecurityProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedHttpGatewaySecurityProcessor.contentLength(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 contentLength 的处理结果；returns the result of the operation.
     */
    private long contentLength(
            java.util.Map<String, List<String>> headers) {
        List<String> values = headers.getOrDefault(
                "content-length",
                List.of()
        );
        if (values.isEmpty()) {
            return -1;
        }
        try {
            return Long.parseLong(values.getFirst());
        } catch (NumberFormatException invalid) {
            return -1;
        }
    }

    /**
     * 中文说明：{@code HttpRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Http请求相关的职责与边界。
     * English summary: {@code HttpRequest} is an immutable data carrier in the current Gateway module; it owns the http request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param requestId 参数 请求Id；parameter request id。
     * @param traceId 参数 traceId；parameter trace id。
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param headers 参数 headers；parameter headers。
     * @param contentLength 参数 contentLength；parameter content length。
     */
    private record HttpRequest(
            /**
             * 中文说明：保存 请求Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request id; its type is {@code String}, and {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String requestId,
            /**
             * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String traceId,
            /**
             * 中文说明：保存 accessZone 对应的状态、依赖或配置值；字段类型为 {@code AccessZone}，由 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by access zone; its type is {@code AccessZone}, and {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            AccessZone accessZone,
            /**
             * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code GatewayHeaders}，由 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code GatewayHeaders}, and {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayHeaders headers,
            /**
             * 中文说明：保存 contentLength 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content length; its type is {@code long}, and {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            long contentLength
    ) implements GatewayRequest {

        /**
         * 中文说明：执行 protocol 操作；该方法是 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the protocol operation; this method is the invocation entry point on {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest.protocol(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 protocol 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayProtocol protocol() {
            return GatewayProtocol.HTTP;
        }

        /**
         * 中文说明：执行 body 操作；该方法是 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the body operation; this method is the invocation entry point on {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest.body(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 body 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayBody body() {
            return new GatewayBody() {
                /**
                 * 中文说明：执行 contentLength 操作；该方法是 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the content length operation; this method is the invocation entry point on {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest.contentLength(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @return 返回 contentLength 的处理结果；returns the result of the operation.
                 */
                @Override
                public long contentLength() {
                    return contentLength;
                }

                /**
                 * 中文说明：执行 replayable 操作；该方法是 {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the replayable operation; this method is the invocation entry point on {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedHttpGatewaySecurityProcessor.HttpRequest.replayable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @return 返回 replayable 的处理结果；returns the result of the operation.
                 */
                @Override
                public boolean replayable() {
                    return false;
                }
            };
        }
    }

    /**
     * 中文说明：{@code HttpExchange} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责HttpExchange相关的职责与边界。
     * English summary: {@code HttpExchange} is an immutable data carrier in the current Gateway module; it owns the http exchange-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     */
    private record HttpExchange(
            /**
             * 中文说明：保存 请求 对应的状态、依赖或配置值；字段类型为 {@code GatewayRequest}，由 {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request; its type is {@code GatewayRequest}, and {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayRequest request,
            /**
             * 中文说明：保存 context 对应的状态、依赖或配置值；字段类型为 {@code GatewayContext}，由 {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by context; its type is {@code GatewayContext}, and {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayContext context
    ) implements GatewayExchange {

        /**
         * 中文说明：执行 响应 操作；该方法是 {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the response operation; this method is the invocation entry point on {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedHttpGatewaySecurityProcessor.HttpExchange.response(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 响应 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayResponse response() {
            return DefaultGatewayResponse.success(EmptyGatewayBody.INSTANCE);
        }
    }
}
