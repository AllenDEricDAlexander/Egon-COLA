package top.egon.cola.component.gateway.engine.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.core.operation.GatewayInvocationResult;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.http.ProviderSelector;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.traffic.GatewayAttemptExecutor;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficContext;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.traffic.ProviderCallClassification;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves an active operation and executes it without a gateway loopback.
 * 补充说明 / Supplementary summary: {@code EngineGatewayOperationInvoker} 是类型，位于当前 Gateway 模块的相关包中，负责引擎网关操作Invoker相关的职责与边界。
 * English supplement: {@code EngineGatewayOperationInvoker} is a type in the current Gateway module; it owns the engine gateway operation invoker-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class EngineGatewayOperationInvoker
        implements GatewayOperationInvoker {

    /**
     * 中文说明：表示 PATHVARIABLE 这一固定值；它属于 {@code EngineGatewayOperationInvoker} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value path variable; it is a state, type, or protocol value of {@code EngineGatewayOperationInvoker} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Pattern PATH_VARIABLE = Pattern.compile(
            "\\{([^}/]+)}"
    );

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledGatewayRules>}，由 {@code EngineGatewayOperationInvoker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledGatewayRules>}, and {@code EngineGatewayOperationInvoker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledGatewayRules> rules;

    /**
     * 中文说明：保存 提供方Selector 对应的状态、依赖或配置值；字段类型为 {@code ProviderSelector}，由 {@code EngineGatewayOperationInvoker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by provider selector; its type is {@code ProviderSelector}, and {@code EngineGatewayOperationInvoker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderSelector providerSelector;

    /**
     * 中文说明：保存 流量Governance 对应的状态、依赖或配置值；字段类型为 {@code GatewayTrafficGovernance}，由 {@code EngineGatewayOperationInvoker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by traffic governance; its type is {@code GatewayTrafficGovernance}, and {@code EngineGatewayOperationInvoker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTrafficGovernance trafficGovernance;

    /**
     * 中文说明：保存 传输 对应的状态、依赖或配置值；字段类型为 {@code OperationTransport}，由 {@code EngineGatewayOperationInvoker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transport; its type is {@code OperationTransport}, and {@code EngineGatewayOperationInvoker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final OperationTransport transport;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code EngineGatewayOperationInvoker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code EngineGatewayOperationInvoker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 default超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code EngineGatewayOperationInvoker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by default timeout; its type is {@code Duration}, and {@code EngineGatewayOperationInvoker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration defaultTimeout;

    /**
     * 中文说明：保存 maximum请求Bytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code EngineGatewayOperationInvoker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum request bytes; its type is {@code long}, and {@code EngineGatewayOperationInvoker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final long maximumRequestBytes;

    /**
     * 中文说明：保存 maximum响应Bytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code EngineGatewayOperationInvoker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum response bytes; its type is {@code long}, and {@code EngineGatewayOperationInvoker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final long maximumResponseBytes;

    /**
     * 中文说明：保存 attemptExecutor 对应的状态、依赖或配置值；字段类型为 {@code GatewayAttemptExecutor}，由 {@code EngineGatewayOperationInvoker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by attempt executor; its type is {@code GatewayAttemptExecutor}, and {@code EngineGatewayOperationInvoker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAttemptExecutor attemptExecutor =
            new GatewayAttemptExecutor();

    /**
     * 中文说明：创建 {@code EngineGatewayOperationInvoker} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code EngineGatewayOperationInvoker} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param transport 参数 传输；parameter transport。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param defaultTimeout 参数 default超时；parameter default timeout。
     * @param maximumRequestBytes 参数 maximum请求Bytes；parameter maximum request bytes。
     * @param maximumResponseBytes 参数 maximum响应Bytes；parameter maximum response bytes。
     */
    public EngineGatewayOperationInvoker(
            Supplier<CompiledGatewayRules> rules,
            ProviderSelector providerSelector,
            GatewayTrafficGovernance trafficGovernance,
            OperationTransport transport,
            ObjectMapper objectMapper,
            Duration defaultTimeout,
            long maximumRequestBytes,
            long maximumResponseBytes) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.providerSelector = Objects.requireNonNull(
                providerSelector,
                "providerSelector"
        );
        this.trafficGovernance = Objects.requireNonNull(
                trafficGovernance,
                "trafficGovernance"
        );
        this.transport = Objects.requireNonNull(transport, "transport");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.defaultTimeout = positive(defaultTimeout, "defaultTimeout");
        this.maximumRequestBytes = positive(
                maximumRequestBytes,
                "maximumRequestBytes"
        );
        this.maximumResponseBytes = positive(
                maximumResponseBytes,
                "maximumResponseBytes"
        );
    }

    /**
     * 中文说明：执行 invoke 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param invocation 参数 invocation；parameter invocation。
     * @return 返回 invoke 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<GatewayInvocationResult> invoke(
            GatewayOperationInvocation invocation) {
        return Mono.defer(() -> invokeActive(invocation));
    }

    /**
     * 中文说明：执行 invokeActive 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke active operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.invokeActive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param invocation 参数 invocation；parameter invocation。
     * @return 返回 invokeActive 的处理结果；returns the result of the operation.
     */
    private Mono<GatewayInvocationResult> invokeActive(
            GatewayOperationInvocation invocation) {
        Objects.requireNonNull(invocation, "invocation");
        CompiledGatewayRules active = rules.get();
        if (active == null) {
            return Mono.error(new IllegalStateException(
                    "GATEWAY_RULES_NOT_READY"
            ));
        }
        GatewayRuntimeOperation operation = active.snapshot()
                .content()
                .operations()
                .stream()
                .filter(candidate -> candidate.operationId().equals(
                        invocation.operationId()
                ))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "GATEWAY_OPERATION_NOT_FOUND"
                ));
        if (operation.deprecated()) {
            return Mono.error(new IllegalArgumentException(
                    "GATEWAY_OPERATION_DEPRECATED"
            ));
        }
        PreparedRequest prepared = prepare(operation, invocation);
        GatewayTrafficContext traffic = trafficContext(
                operation,
                invocation,
                prepared
        );
        return trafficGovernance.acquire(
                operation.policyRefs(),
                traffic,
                defaultTimeout
        ).flatMap(permit -> {
            long requestLimit = permit.requestSizeLimit(
                    maximumRequestBytes
            );
            if (prepared.body().length > requestLimit) {
                permit.close();
                return Mono.error(new IllegalArgumentException(
                        "GATEWAY_REQUEST_BODY_TOO_LARGE"
                ));
            }
            long responseLimit = permit.responseSizeLimit(
                    maximumResponseBytes
            );
            Set<String> failedProviders = new LinkedHashSet<>();
            AtomicInteger attempts = new AtomicInteger();
            return attemptExecutor.execute(
                    permit.retryPolicy(),
                    idempotent(operation),
                    true,
                    permit.timeout(),
                    () -> attempt(
                            operation,
                            prepared,
                            permit,
                            failedProviders,
                            responseLimit,
                            attempts.incrementAndGet()
                    ),
                    RetryableInvocationException.class::isInstance
            ).doFinally(ignored -> permit.close());
        });
    }

    /**
     * 中文说明：执行 attempt 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attempt operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.attempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @param prepared 参数 prepared；parameter prepared。
     * @param requestPermit 参数 请求Permit；parameter request permit。
     * @param failedProviders 参数 failedProviders；parameter failed providers。
     * @param responseLimit 参数 响应Limit；parameter response limit。
     * @param attemptNumber 参数 attemptNumber；parameter attempt number。
     * @return 返回 attempt 的处理结果；returns the result of the operation.
     */
    private Mono<GatewayInvocationResult> attempt(
            GatewayRuntimeOperation operation,
            PreparedRequest prepared,
            GatewayTrafficGovernance.RequestPermit requestPermit,
            Set<String> failedProviders,
            long responseLimit,
            int attemptNumber) {
        ProviderSelectionHandle selection = providerSelector.select(
                serviceKey(operation.providerService()),
                operation.policyRefs(),
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
        AtomicBoolean completed = new AtomicBoolean();
        return transport.invoke(provider, prepared, requestPermit.timeout())
                .flatMap(result -> {
                    if (result.body().length > responseLimit) {
                        return Mono.error(new IllegalArgumentException(
                                "GATEWAY_RESPONSE_BODY_TOO_LARGE"
                        ));
                    }
                    if (requestPermit.retryPolicy().enabled()
                            && idempotent(operation)
                            && attemptNumber < requestPermit.retryPolicy()
                            .maxAttempts()
                            && requestPermit.retryPolicy()
                            .retryableHttpStatus(result.statusCode())) {
                        failedProviders.add(provider.runtimeIdentity());
                        return Mono.error(new RetryableInvocationException(
                                "retryable upstream status "
                                        + result.statusCode()
                        ));
                    }
                    complete(
                            attemptPermit,
                            completed,
                            classification(result.statusCode())
                    );
                    return Mono.just(result);
                })
                .onErrorMap(failure -> failure instanceof
                        RetryableInvocationException
                        || failure instanceof IllegalArgumentException
                        ? failure
                        : new RetryableInvocationException(failure))
                .doOnError(failure -> complete(
                        attemptPermit,
                        completed,
                        failure instanceof RetryableInvocationException
                                ? ProviderCallClassification.RETRYABLE_FAILURE
                                : ProviderCallClassification.BUSINESS_FAILURE
                ))
                .doFinally(ignored -> {
                    if (completed.compareAndSet(false, true)) {
                        attemptPermit.close();
                    }
                    selection.close();
                });
    }

    /**
     * 中文说明：执行 complete 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the complete operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param permit 参数 permit；parameter permit。
     * @param completed 参数 completed；parameter completed。
     * @param classification 参数 classification；parameter classification。
     */
    private void complete(
            GatewayTrafficGovernance.AttemptPermit permit,
            AtomicBoolean completed,
            ProviderCallClassification classification) {
        if (completed.compareAndSet(false, true)) {
            permit.complete(classification);
        }
    }

    /**
     * 中文说明：执行 classification 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the classification operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.classification(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 classification 的处理结果；returns the result of the operation.
     */
    private ProviderCallClassification classification(int status) {
        if (status >= 500) {
            return ProviderCallClassification.RETRYABLE_FAILURE;
        }
        if (status >= 400) {
            return ProviderCallClassification.BUSINESS_FAILURE;
        }
        return ProviderCallClassification.SUCCESS;
    }

    /**
     * 中文说明：执行 prepare 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prepare operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.prepare(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @param invocation 参数 invocation；parameter invocation。
     * @return 返回 prepare 的处理结果；returns the result of the operation.
     */
    private PreparedRequest prepare(
            GatewayRuntimeOperation operation,
            GatewayOperationInvocation invocation) {
        String[] identity = operation.methodIdentity().split("\\s+", 2);
        String method = operation.protocol() == GatewayProtocol.HTTP
                && identity.length == 2
                ? identity[0].toUpperCase(Locale.ROOT)
                : "POST";
        String path = operation.protocol() == GatewayProtocol.HTTP
                && identity.length == 2
                ? identity[1]
                : "/rpc/" + encodePath(operation.methodIdentity());
        LinkedHashMap<String, Object> remaining = new LinkedHashMap<>(
                invocation.call().pathArguments()
        );
        LinkedHashMap<String, String> pathVariables = new LinkedHashMap<>();
        Matcher matcher = PATH_VARIABLE.matcher(path);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!remaining.containsKey(name)) {
                throw new IllegalArgumentException(
                        "GATEWAY_OPERATION_ARGUMENT_MISSING: " + name
                );
            }
            String value = Objects.toString(remaining.remove(name), "");
            pathVariables.put(name, value);
            matcher.appendReplacement(
                    resolved,
                    Matcher.quoteReplacement(encodePath(value))
            );
        }
        matcher.appendTail(resolved);
        if (!remaining.isEmpty()) {
            throw new IllegalArgumentException(
                    "GATEWAY_OPERATION_PATH_ARGUMENT_UNKNOWN: "
                            + remaining.keySet().iterator().next()
            );
        }
        LinkedHashMap<String, String> query = new LinkedHashMap<>();
        invocation.call().queryArguments().forEach((key, value) -> query.put(
                key,
                Objects.toString(value, "")
        ));
        byte[] body = invocation.call().body() == null
                ? new byte[0]
                : json(invocation.call().body());
        String pathAndQuery = resolved + query(query);
        LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("accept", List.of("application/json"));
        if (body.length > 0) {
            headers.put("content-type", List.of("application/json"));
        }
        if (invocation.originalBearerToken() != null) {
            headers.put("authorization", List.of(
                    invocation.originalBearerToken()
            ));
        }
        invocation.traceHeaders().forEach((name, value) ->
                headers.put(name, List.of(value))
        );
        return new PreparedRequest(
                operation,
                method,
                pathAndQuery,
                Map.copyOf(headers),
                body,
                Map.copyOf(pathVariables),
                Map.copyOf(query)
        );
    }

    /**
     * 中文说明：执行 流量Context 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the traffic context operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.trafficContext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @param invocation 参数 invocation；parameter invocation。
     * @param prepared 参数 prepared；parameter prepared。
     * @return 返回 流量Context 的处理结果；returns the result of the operation.
     */
    private GatewayTrafficContext trafficContext(
            GatewayRuntimeOperation operation,
            GatewayOperationInvocation invocation,
            PreparedRequest prepared) {
        return new GatewayTrafficContext(
                operation.operationId(),
                "mcp:" + operation.operationId(),
                operation.providerService().appCode(),
                invocation.callerId(),
                invocation.clientIp(),
                operation.providerService().serviceName(),
                null,
                invocation.traceHeaders(),
                prepared.pathVariables(),
                prepared.queryParameters()
        );
    }

    /**
     * 中文说明：执行 服务键 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the service key operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.serviceKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param service 参数 服务；parameter service。
     * @return 返回 服务键 的处理结果；returns the result of the operation.
     */
    private ProviderServiceKey serviceKey(GatewayProviderServiceRef service) {
        return new ProviderServiceKey(
                service.bizCode(),
                service.appCode(),
                service.env(),
                service.namespace(),
                service.protocol() == GatewayProtocol.HTTP
                        ? ProviderProtocolType.HTTP
                        : ProviderProtocolType.RPC,
                service.serviceName(),
                service.group(),
                service.version(),
                service.transport()
        );
    }

    /**
     * 中文说明：执行 idempotent 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the idempotent operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.idempotent(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @return 返回 idempotent 的处理结果；returns the result of the operation.
     */
    private boolean idempotent(GatewayRuntimeOperation operation) {
        return Boolean.parseBoolean(operation.attributes().getOrDefault(
                "idempotent",
                "false"
        ));
    }

    /**
     * 中文说明：执行 json 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.json(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param body 参数 body；parameter body。
     * @return 返回 json 的处理结果；returns the result of the operation.
     */
    private byte[] json(Object body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception failure) {
            throw new IllegalArgumentException(
                    "GATEWAY_OPERATION_ARGUMENTS_INVALID",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 query 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the query operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.query(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @return 返回 query 的处理结果；returns the result of the operation.
     */
    private String query(Map<String, String> values) {
        if (values.isEmpty()) {
            return "";
        }
        List<String> parameters = new ArrayList<>();
        values.forEach((name, value) -> parameters.add(
                encodeQuery(name) + "=" + encodeQuery(value)
        ));
        return "?" + String.join("&", parameters);
    }

    /**
     * 中文说明：执行 encodePath 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the encode path operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.encodePath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 encodePath 的处理结果；returns the result of the operation.
     */
    private String encodePath(String value) {
        return encodeQuery(value).replace("+", "%20");
    }

    /**
     * 中文说明：执行 encodeQuery 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the encode query operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.encodeQuery(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 encodeQuery 的处理结果；returns the result of the operation.
     */
    private String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code EngineGatewayOperationInvoker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private long positive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /**
     * 中文说明：{@code OperationTransport} 是接口契约，位于当前 Gateway 模块的相关包中，负责操作传输相关的职责与边界。
     * English summary: {@code OperationTransport} is an interface contract in the current Gateway module; it owns the operation transport-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    public interface OperationTransport {

        /**
         * 中文说明：执行 invoke 操作；该方法是 {@code EngineGatewayOperationInvoker.OperationTransport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the invoke operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker.OperationTransport} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.OperationTransport.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param provider 参数 提供方；parameter provider。
         * @param request 参数 请求；parameter request。
         * @param timeout 参数 超时；parameter timeout。
         * @return 返回 invoke 的处理结果；returns the result of the operation.
         */
        Mono<GatewayInvocationResult> invoke(
                ProviderInstance provider,
                PreparedRequest request,
                Duration timeout);
    }

    /**
     * 中文说明：{@code PreparedRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Prepared请求相关的职责与边界。
     * English summary: {@code PreparedRequest} is an immutable data carrier in the current Gateway module; it owns the prepared request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param operation 参数 操作；parameter operation。
     * @param method 参数 方法；parameter method。
     * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param pathVariables 参数 pathVariables；parameter path variables。
     * @param queryParameters 参数 queryParameters；parameter query parameters。
     */
    public record PreparedRequest(
            /**
             * 中文说明：保存 操作 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuntimeOperation}，由 {@code EngineGatewayOperationInvoker.PreparedRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation; its type is {@code GatewayRuntimeOperation}, and {@code EngineGatewayOperationInvoker.PreparedRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker.PreparedRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker.PreparedRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayRuntimeOperation operation,
            /**
             * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code EngineGatewayOperationInvoker.PreparedRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code EngineGatewayOperationInvoker.PreparedRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker.PreparedRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker.PreparedRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String method,
            /**
             * 中文说明：保存 pathAndQuery 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code EngineGatewayOperationInvoker.PreparedRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by path and query; its type is {@code String}, and {@code EngineGatewayOperationInvoker.PreparedRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker.PreparedRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker.PreparedRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String pathAndQuery,
            /**
             * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, List<String>>}，由 {@code EngineGatewayOperationInvoker.PreparedRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, List<String>>}, and {@code EngineGatewayOperationInvoker.PreparedRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker.PreparedRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker.PreparedRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, List<String>> headers,
            /**
             * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code EngineGatewayOperationInvoker.PreparedRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code byte[]}, and {@code EngineGatewayOperationInvoker.PreparedRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker.PreparedRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker.PreparedRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            byte[] body,
            /**
             * 中文说明：保存 pathVariables 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code EngineGatewayOperationInvoker.PreparedRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by path variables; its type is {@code Map<String, String>}, and {@code EngineGatewayOperationInvoker.PreparedRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker.PreparedRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker.PreparedRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> pathVariables,
            /**
             * 中文说明：保存 queryParameters 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code EngineGatewayOperationInvoker.PreparedRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by query parameters; its type is {@code Map<String, String>}, and {@code EngineGatewayOperationInvoker.PreparedRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code EngineGatewayOperationInvoker.PreparedRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code EngineGatewayOperationInvoker.PreparedRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> queryParameters
    ) {

        /**
         * 中文说明：创建 {@code EngineGatewayOperationInvoker.PreparedRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code EngineGatewayOperationInvoker.PreparedRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param operation 参数 操作；parameter operation。
         * @param method 参数 方法；parameter method。
         * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
         * @param headers 参数 headers；parameter headers。
         * @param body 参数 body；parameter body。
         * @param pathVariables 参数 pathVariables；parameter path variables。
         * @param queryParameters 参数 queryParameters；parameter query parameters。
         */
        public PreparedRequest {
            operation = Objects.requireNonNull(operation, "operation");
            method = Objects.requireNonNull(method, "method");
            pathAndQuery = Objects.requireNonNull(
                    pathAndQuery,
                    "pathAndQuery"
            );
            headers = Map.copyOf(headers);
            body = body.clone();
            pathVariables = Map.copyOf(pathVariables);
            queryParameters = Map.copyOf(queryParameters);
        }

        /**
         * 中文说明：执行 body 操作；该方法是 {@code EngineGatewayOperationInvoker.PreparedRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the body operation; this method is the invocation entry point on {@code EngineGatewayOperationInvoker.PreparedRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code EngineGatewayOperationInvoker.PreparedRequest.body(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 body 的处理结果；returns the result of the operation.
         */
        @Override
        public byte[] body() {
            return body.clone();
        }
    }

    /**
     * 中文说明：{@code RetryableInvocationException} 是异常类型，位于当前 Gateway 模块的相关包中，负责RetryableInvocationException相关的职责与边界。
     * English summary: {@code RetryableInvocationException} is a retryable invocation exception exception in the current Gateway module; it owns the retryable invocation exception-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class RetryableInvocationException
            extends RuntimeException {

        /**
         * 中文说明：创建 {@code EngineGatewayOperationInvoker.RetryableInvocationException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code EngineGatewayOperationInvoker.RetryableInvocationException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param message 参数 消息；parameter message。
         */
        private RetryableInvocationException(String message) {
            super(message);
        }

        /**
         * 中文说明：创建 {@code EngineGatewayOperationInvoker.RetryableInvocationException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code EngineGatewayOperationInvoker.RetryableInvocationException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param cause 参数 cause；parameter cause。
         */
        private RetryableInvocationException(Throwable cause) {
            super(cause);
        }
    }
}
