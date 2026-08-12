package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.Deadline;
import io.grpc.Metadata;
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
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityChain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 中文说明：{@code RuleBackedRpcGatewaySecurityProcessor} 是类型，位于当前 Gateway 模块的相关包中，负责规则BackedRpc网关安全Processor相关的职责与边界。
 * English summary: {@code RuleBackedRpcGatewaySecurityProcessor} is a type in the current Gateway module; it owns the rule backed rpc gateway security processor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RuleBackedRpcGatewaySecurityProcessor
        implements GatewayRpcSecurityProcessor {

    /**
     * 中文说明：保存 chain 对应的状态、依赖或配置值；字段类型为 {@code GatewaySecurityChain}，由 {@code RuleBackedRpcGatewaySecurityProcessor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by chain; its type is {@code GatewaySecurityChain}, and {@code RuleBackedRpcGatewaySecurityProcessor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RuleBackedRpcGatewaySecurityProcessor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedRpcGatewaySecurityProcessor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewaySecurityChain chain;

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledGatewayRules>}，由 {@code RuleBackedRpcGatewaySecurityProcessor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledGatewayRules>}, and {@code RuleBackedRpcGatewaySecurityProcessor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RuleBackedRpcGatewaySecurityProcessor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedRpcGatewaySecurityProcessor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledGatewayRules> rules;

    /**
     * 中文说明：保存 引擎NodeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuleBackedRpcGatewaySecurityProcessor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by engine node id; its type is {@code String}, and {@code RuleBackedRpcGatewaySecurityProcessor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RuleBackedRpcGatewaySecurityProcessor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedRpcGatewaySecurityProcessor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String engineNodeId;

    /**
     * 中文说明：创建 {@code RuleBackedRpcGatewaySecurityProcessor} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RuleBackedRpcGatewaySecurityProcessor} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param chain 参数 chain；parameter chain。
     * @param rules 参数 rules；parameter rules。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     */
    public RuleBackedRpcGatewaySecurityProcessor(
            GatewaySecurityChain chain,
            Supplier<CompiledGatewayRules> rules,
            String engineNodeId) {
        this.chain = Objects.requireNonNull(chain, "chain");
        this.rules = Objects.requireNonNull(rules, "rules");
        if (engineNodeId == null || engineNodeId.isBlank()) {
            throw new IllegalArgumentException("engineNodeId is required");
        }
        this.engineNodeId = engineNodeId.trim();
    }

    /**
     * 中文说明：执行 authorize 操作；该方法是 {@code RuleBackedRpcGatewaySecurityProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize operation; this method is the invocation entry point on {@code RuleBackedRpcGatewaySecurityProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedRpcGatewaySecurityProcessor.authorize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     * @param inboundMetadata 参数 inbound元数据；parameter inbound metadata。
     * @param traceId 参数 traceId；parameter trace id。
     * @param inboundDeadline 参数 inboundDeadline；parameter inbound deadline。
     * @return 返回 authorize 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Outcome> authorize(
            RuntimeRpcRoute route,
            Metadata inboundMetadata,
            String traceId,
            Deadline inboundDeadline) {
        CompiledGatewayRules current = rules.get();
        List<GatewaySecurityPolicy> policies = route.policyRefs().stream()
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
        Instant startedAt = Instant.now();
        Instant deadline = startedAt.plus(effectiveTimeout(
                policy.providerTimeout(),
                inboundDeadline
        ));
        String requestId = UuidV7.simpleString();
        GatewayContext gatewayContext = new GatewayContext(
                requestId,
                traceId,
                null,
                null,
                AccessZone.INTERNAL,
                current.snapshot().content().gatewayGroupId(),
                engineNodeId,
                route.operationId(),
                route.routeId(),
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
                AccessZone.INTERNAL,
                GatewayProtocol.RPC,
                route.operationId(),
                route.routeId(),
                policy.policyId(),
                route.fullMethodName(),
                null,
                java.util.Set.of(),
                GatewayPrincipal.anonymous(),
                "rpc-internal",
                traceId,
                requestId,
                deadline,
                current.snapshot().releaseId(),
                securityAttributes(route)
        );
        GatewayExchange exchange = new RpcExchange(
                new RpcRequest(
                        requestId,
                        traceId,
                        new ImmutableGatewayHeaders(headers(inboundMetadata))
                ),
                gatewayContext
        );
        return chain.execute(
                        exchange,
                        authContext,
                        policy,
                        GatewayProtocol.RPC
                )
                .map(result -> new Outcome(
                        result.trustedIdentity(),
                        result.fieldsToRemove()
                ));
    }

    /**
     * 中文说明：执行 安全Attributes 操作；该方法是 {@code RuleBackedRpcGatewaySecurityProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the security attributes operation; this method is the invocation entry point on {@code RuleBackedRpcGatewaySecurityProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedRpcGatewaySecurityProcessor.securityAttributes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     * @return 返回 安全Attributes 的处理结果；returns the result of the operation.
     */
    static Map<String, String> securityAttributes(RuntimeRpcRoute route) {
        ProviderServiceKey target = route.targetService();
        return Map.of(
                "idp.biz-code", target.bizCode(),
                "idp.app-code", target.appCode(),
                "idp.env", target.env()
        );
    }

    /**
     * 中文说明：执行 effective超时 操作；该方法是 {@code RuleBackedRpcGatewaySecurityProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the effective timeout operation; this method is the invocation entry point on {@code RuleBackedRpcGatewaySecurityProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedRpcGatewaySecurityProcessor.effectiveTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policyTimeout 参数 策略超时；parameter policy timeout。
     * @param inboundDeadline 参数 inboundDeadline；parameter inbound deadline。
     * @return 返回 effective超时 的处理结果；returns the result of the operation.
     */
    private Duration effectiveTimeout(
            Duration policyTimeout,
            Deadline inboundDeadline) {
        if (inboundDeadline == null) {
            return policyTimeout;
        }
        Duration inbound = Duration.ofNanos(Math.max(
                1,
                inboundDeadline.timeRemaining(TimeUnit.NANOSECONDS)
        ));
        return inbound.compareTo(policyTimeout) < 0
                ? inbound
                : policyTimeout;
    }

    /**
     * 中文说明：执行 headers 操作；该方法是 {@code RuleBackedRpcGatewaySecurityProcessor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the headers operation; this method is the invocation entry point on {@code RuleBackedRpcGatewaySecurityProcessor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedRpcGatewaySecurityProcessor.headers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param metadata 参数 元数据；parameter metadata。
     * @return 返回 headers 的处理结果；returns the result of the operation.
     */
    private Map<String, List<String>> headers(Metadata metadata) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String name : metadata.keys()) {
            if (name.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                continue;
            }
            String value = metadata.get(Metadata.Key.of(
                    name,
                    Metadata.ASCII_STRING_MARSHALLER
            ));
            if (value != null) {
                result.computeIfAbsent(
                        name,
                        ignored -> new java.util.ArrayList<>()
                ).add(value);
            }
        }
        return result;
    }

    /**
     * 中文说明：{@code RpcRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Rpc请求相关的职责与边界。
     * English summary: {@code RpcRequest} is an immutable data carrier in the current Gateway module; it owns the rpc request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param requestId 参数 请求Id；parameter request id。
     * @param traceId 参数 traceId；parameter trace id。
     * @param headers 参数 headers；parameter headers。
     */
    private record RpcRequest(
            /**
             * 中文说明：保存 请求Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request id; its type is {@code String}, and {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String requestId,
            /**
             * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String traceId,
            /**
             * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code GatewayHeaders}，由 {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code GatewayHeaders}, and {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayHeaders headers
    ) implements GatewayRequest {

        /**
         * 中文说明：执行 protocol 操作；该方法是 {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the protocol operation; this method is the invocation entry point on {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest.protocol(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 protocol 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayProtocol protocol() {
            return GatewayProtocol.RPC;
        }

        /**
         * 中文说明：执行 accessZone 操作；该方法是 {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the access zone operation; this method is the invocation entry point on {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest.accessZone(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 accessZone 的处理结果；returns the result of the operation.
         */
        @Override
        public AccessZone accessZone() {
            return AccessZone.INTERNAL;
        }

        /**
         * 中文说明：执行 body 操作；该方法是 {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the body operation; this method is the invocation entry point on {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedRpcGatewaySecurityProcessor.RpcRequest.body(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 body 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayBody body() {
            return EmptyGatewayBody.INSTANCE;
        }
    }

    /**
     * 中文说明：{@code RpcExchange} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责RpcExchange相关的职责与边界。
     * English summary: {@code RpcExchange} is an immutable data carrier in the current Gateway module; it owns the rpc exchange-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     */
    private record RpcExchange(
            /**
             * 中文说明：保存 请求 对应的状态、依赖或配置值；字段类型为 {@code GatewayRequest}，由 {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request; its type is {@code GatewayRequest}, and {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayRequest request,
            /**
             * 中文说明：保存 context 对应的状态、依赖或配置值；字段类型为 {@code GatewayContext}，由 {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by context; its type is {@code GatewayContext}, and {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayContext context
    ) implements GatewayExchange {

        /**
         * 中文说明：执行 响应 操作；该方法是 {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the response operation; this method is the invocation entry point on {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RuleBackedRpcGatewaySecurityProcessor.RpcExchange.response(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 响应 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayResponse response() {
            return DefaultGatewayResponse.success(EmptyGatewayBody.INSTANCE);
        }
    }
}
