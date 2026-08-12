package top.egon.cola.component.gateway.engine.traffic;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

/**
 * 中文说明：{@code GatewayTrafficGovernance} 是类型，位于当前 Gateway 模块的相关包中，负责网关流量Governance相关的职责与边界。
 * English summary: {@code GatewayTrafficGovernance} is a type in the current Gateway module; it owns the gateway traffic governance-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayTrafficGovernance {

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledGatewayRules>}，由 {@code GatewayTrafficGovernance} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledGatewayRules>}, and {@code GatewayTrafficGovernance} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledGatewayRules> rules;

    /**
     * 中文说明：保存 localRateLimiter 对应的状态、依赖或配置值；字段类型为 {@code LocalTokenBucketRateLimiter}，由 {@code GatewayTrafficGovernance} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by local rate limiter; its type is {@code LocalTokenBucketRateLimiter}, and {@code GatewayTrafficGovernance} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final LocalTokenBucketRateLimiter localRateLimiter;

    /**
     * 中文说明：保存 distributedRateLimiter 对应的状态、依赖或配置值；字段类型为 {@code DistributedTokenBucketRateLimiter}，由 {@code GatewayTrafficGovernance} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by distributed rate limiter; its type is {@code DistributedTokenBucketRateLimiter}, and {@code GatewayTrafficGovernance} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DistributedTokenBucketRateLimiter distributedRateLimiter;

    /**
     * 中文说明：保存 bulkheads 对应的状态、依赖或配置值；字段类型为 {@code GatewayBulkheadRegistry}，由 {@code GatewayTrafficGovernance} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by bulkheads; its type is {@code GatewayBulkheadRegistry}, and {@code GatewayTrafficGovernance} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayBulkheadRegistry bulkheads =
            new GatewayBulkheadRegistry();

    /**
     * 中文说明：保存 circuits 对应的状态、依赖或配置值；字段类型为 {@code GatewayCircuitBreakerRegistry}，由 {@code GatewayTrafficGovernance} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by circuits; its type is {@code GatewayCircuitBreakerRegistry}, and {@code GatewayTrafficGovernance} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCircuitBreakerRegistry circuits =
            new GatewayCircuitBreakerRegistry();

    /**
     * 中文说明：保存 键Compiler 对应的状态、依赖或配置值；字段类型为 {@code GatewayPolicyKeyCompiler}，由 {@code GatewayTrafficGovernance} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by key compiler; its type is {@code GatewayPolicyKeyCompiler}, and {@code GatewayTrafficGovernance} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayPolicyKeyCompiler keyCompiler =
            new GatewayPolicyKeyCompiler();

    /**
     * 中文说明：创建 {@code GatewayTrafficGovernance} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTrafficGovernance} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param redis 参数 redis；parameter redis。
     */
    public GatewayTrafficGovernance(
            Supplier<CompiledGatewayRules> rules,
            RedisTokenBucketExecutor redis) {
        this.rules = Objects.requireNonNull(rules, "rules");
        localRateLimiter = new LocalTokenBucketRateLimiter(
                System::nanoTime,
                System::currentTimeMillis
        );
        RedisTokenBucketExecutor executor = redis == null
                ? (script, keys, arguments) -> {
            throw new IllegalStateException(
                    "gateway rate limit Redis is unavailable"
            );
        }
                : redis;
        distributedRateLimiter = new DistributedTokenBucketRateLimiter(
                executor,
                localRateLimiter
        );
    }

    /**
     * 中文说明：执行 noop 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the noop operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.noop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 noop 的处理结果；returns the result of the operation.
     */
    public static GatewayTrafficGovernance noop() {
        return new GatewayTrafficGovernance(() -> null, null);
    }

    /**
     * 中文说明：执行 acquire 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the acquire operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.acquire(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @param context 参数 context；parameter context。
     * @param defaultTimeout 参数 default超时；parameter default timeout。
     * @return 返回 acquire 的处理结果；returns the result of the operation.
     */
    public Mono<RequestPermit> acquire(
            Set<String> policyRefs,
            GatewayTrafficContext context,
            Duration defaultTimeout) {
        List<RuntimeTrafficPolicy> policies = policies(policyRefs);
        if (policies.isEmpty()) {
            return Mono.just(new RequestPermit(
                    policies,
                    context,
                    defaultTimeout,
                    List.of()
            ));
        }
        boolean distributed = policies.stream()
                .filter(policy -> policy.type()
                        == TrafficPolicyType.RATE_LIMIT)
                .anyMatch(this::distributed);
        Mono<RequestPermit> acquisition = Mono.fromCallable(() -> {
            List<GatewayBulkheadRegistry.Permit> acquired =
                    new ArrayList<>();
            try {
                applyRateLimits(policies, context);
                for (RuntimeTrafficPolicy policy : policies) {
                    if (policy.type() == TrafficPolicyType.BULKHEAD
                            && policy.scope()
                            != TrafficPolicyScope.PROVIDER_INSTANCE) {
                        GatewayBulkheadRegistry.Permit permit =
                                bulkheads.tryAcquire(
                                        policy.policyId(),
                                        policy.stateEpoch(),
                                        policy.policyVersion(),
                                        dimension(policy.scope(), context),
                                        integer(
                                                policy.parameters(),
                                                "maxConcurrent",
                                                100
                                        )
                                );
                        if (!permit.acquired()) {
                            throw rejected(
                                    "GATEWAY_CONCURRENCY_REJECTED",
                                    503,
                                    "RESOURCE_EXHAUSTED",
                                    0
                            );
                        }
                        acquired.add(permit);
                    }
                }
                return new RequestPermit(
                        policies,
                        context,
                        effectiveTimeout(policies, defaultTimeout),
                        acquired
                );
            } catch (RuntimeException failure) {
                acquired.forEach(GatewayBulkheadRegistry.Permit::close);
                throw failure;
            }
        });
        return distributed
                ? acquisition.subscribeOn(Schedulers.boundedElastic())
                : acquisition;
    }

    /**
     * 中文说明：执行 applyRateLimits 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the apply rate limits operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.applyRateLimits(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policies 参数 policies；parameter policies。
     * @param context 参数 context；parameter context。
     */
    private void applyRateLimits(
            List<RuntimeTrafficPolicy> policies,
            GatewayTrafficContext context) {
        for (RuntimeTrafficPolicy policy : policies) {
            if (policy.type() != TrafficPolicyType.RATE_LIMIT) {
                continue;
            }
            LocalTokenBucketPolicy bucket = bucket(policy);
            String keyHash = keyCompiler.compile(
                    policy.keyExpression()
            ).hash(context);
            RateLimitDecision decision = distributed(policy)
                    ? distributedRateLimiter.acquire(
                    string(
                            policy.parameters(),
                            "env",
                            "default"
                    ),
                    string(
                            policy.parameters(),
                            "namespace",
                            "default"
                    ),
                    bucket,
                    keyHash,
                    longValue(policy.parameters(), "permits", 1),
                    policy.failureMode()
            )
                    : localRateLimiter.acquire(
                    bucket,
                    keyHash,
                    longValue(policy.parameters(), "permits", 1)
            );
            if (!decision.allowed()) {
                String code = decision.backendUnavailable()
                        && policy.failureMode() == RateLimitFailureMode.DENY
                        ? "GATEWAY_RATE_LIMIT_BACKEND_UNAVAILABLE"
                        : "GATEWAY_RATE_LIMITED";
                throw rejected(
                        code,
                        429,
                        "RESOURCE_EXHAUSTED",
                        decision.retryAfterMillis()
                );
            }
        }
    }

    /**
     * 中文说明：执行 bucket 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bucket operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.bucket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @return 返回 bucket 的处理结果；returns the result of the operation.
     */
    private LocalTokenBucketPolicy bucket(RuntimeTrafficPolicy policy) {
        Map<String, Object> values = policy.parameters();
        long capacity = longValue(values, "capacity", 100);
        return new LocalTokenBucketPolicy(
                policy.policyId(),
                policy.stateEpoch(),
                capacity,
                longValue(values, "refillTokens", capacity),
                duration(values, "refillPeriod", Duration.ofSeconds(1)),
                longValue(values, "initialTokens", capacity),
                integer(values, "maximumKeys", 10_000),
                duration(values, "idleTtl", Duration.ofMinutes(10))
        );
    }

    /**
     * 中文说明：执行 policies 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the policies operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.policies(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @return 返回 policies 的处理结果；returns the result of the operation.
     */
    private List<RuntimeTrafficPolicy> policies(Set<String> policyRefs) {
        CompiledGatewayRules active = rules.get();
        if (active == null || policyRefs == null || policyRefs.isEmpty()) {
            return List.of();
        }
        return policyRefs.stream()
                .map(active.trafficPolicies()::get)
                .filter(Objects::nonNull)
                .filter(RuntimeTrafficPolicy::enabled)
                .sorted(Comparator
                        .comparingInt(RuntimeTrafficPolicy::priority)
                        .thenComparing(RuntimeTrafficPolicy::policyId))
                .toList();
    }

    /**
     * 中文说明：执行 effective超时 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the effective timeout operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.effectiveTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policies 参数 policies；parameter policies。
     * @param fallback 参数 fallback；parameter fallback。
     * @return 返回 effective超时 的处理结果；returns the result of the operation.
     */
    private Duration effectiveTimeout(
            List<RuntimeTrafficPolicy> policies,
            Duration fallback) {
        Duration result = fallback;
        for (RuntimeTrafficPolicy policy : policies) {
            if (policy.type() == TrafficPolicyType.TIMEOUT) {
                Duration configured = duration(
                        policy.parameters(),
                        "timeout",
                        fallback
                );
                if (configured.compareTo(result) < 0) {
                    result = configured;
                }
            }
        }
        return result;
    }

    /**
     * 中文说明：执行 dimension 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dimension operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.dimension(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param context 参数 context；parameter context。
     * @return 返回 dimension 的处理结果；returns the result of the operation.
     */
    private String dimension(
            TrafficPolicyScope scope,
            GatewayTrafficContext context) {
        return switch (scope) {
            case GLOBAL -> "global";
            case GATEWAY_GROUP -> safe(context.applicationCode(), "group");
            case APPLICATION -> safe(
                    context.applicationCode(),
                    "application"
            );
            case OPERATION -> safe(context.operationId(), "operation");
            case ROUTE -> safe(context.routeId(), "route");
            case CALLER -> safe(context.callerId(), "anonymous");
            case PROVIDER_SERVICE -> safe(
                    context.providerService(),
                    "provider"
            );
            case PROVIDER_INSTANCE -> safe(
                    context.providerInstance(),
                    "provider"
            );
            case BUSINESS_DOMAIN, ENTITY_DOMAIN, INTERFACE_GROUP ->
                    safe(context.operationId(), scope.name());
        };
    }

    /**
     * 中文说明：执行 distributed 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the distributed operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.distributed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @return 返回 distributed 的处理结果；returns the result of the operation.
     */
    private boolean distributed(RuntimeTrafficPolicy policy) {
        return "DISTRIBUTED".equalsIgnoreCase(string(
                policy.parameters(),
                "mode",
                "LOCAL"
        ));
    }

    /**
     * 中文说明：执行 rejected 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rejected operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param code 参数 code；parameter code。
     * @param httpStatus 参数 httpStatus；parameter http status。
     * @param rpcStatus 参数 rpcStatus；parameter rpc status。
     * @param retryAfterMillis 参数 重试AfterMillis；parameter retry after millis。
     * @return 返回 rejected 的处理结果；returns the result of the operation.
     */
    private GatewayTrafficRejectedException rejected(
            String code,
            int httpStatus,
            String rpcStatus,
            long retryAfterMillis) {
        return new GatewayTrafficRejectedException(
                code,
                httpStatus,
                rpcStatus,
                retryAfterMillis
        );
    }

    /**
     * 中文说明：执行 duration 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the duration operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.duration(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 duration 的处理结果；returns the result of the operation.
     */
    private Duration duration(
            Map<String, Object> source,
            String key,
            Duration defaultValue) {
        Object value = source.get(key);
        if (value == null) {
            Object millis = source.get(key + "Millis");
            return millis == null
                    ? defaultValue
                    : Duration.ofMillis(number(millis));
        }
        if (value instanceof Duration duration) {
            return duration;
        }
        if (value instanceof Number number) {
            return Duration.ofMillis(number.longValue());
        }
        return Duration.parse(value.toString());
    }

    /**
     * 中文说明：执行 string 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.string(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 string 的处理结果；returns the result of the operation.
     */
    private String string(
            Map<String, Object> source,
            String key,
            String defaultValue) {
        Object value = source.get(key);
        return value == null ? defaultValue : value.toString();
    }

    /**
     * 中文说明：执行 integer 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the integer operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.integer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 integer 的处理结果；returns the result of the operation.
     */
    private int integer(
            Map<String, Object> source,
            String key,
            int defaultValue) {
        return Math.toIntExact(longValue(source, key, defaultValue));
    }

    /**
     * 中文说明：执行 long值 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the long value operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.longValue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 long值 的处理结果；returns the result of the operation.
     */
    private long longValue(
            Map<String, Object> source,
            String key,
            long defaultValue) {
        Object value = source.get(key);
        return value == null ? defaultValue : number(value);
    }

    /**
     * 中文说明：执行 number 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the number operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.number(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 number 的处理结果；returns the result of the operation.
     */
    private long number(Object value) {
        return value instanceof Number number
                ? number.longValue()
                : Long.parseLong(value.toString());
    }

    /**
     * 中文说明：执行 safe 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.safe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param fallback 参数 fallback；parameter fallback。
     * @return 返回 safe 的处理结果；returns the result of the operation.
     */
    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 中文说明：{@code RequestPermit} 是类型，位于当前 Gateway 模块的相关包中，负责请求Permit相关的职责与边界。
     * English summary: {@code RequestPermit} is a type in the current Gateway module; it owns the request permit-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public final class RequestPermit implements AutoCloseable {

        /**
         * 中文说明：保存 policies 对应的状态、依赖或配置值；字段类型为 {@code List<RuntimeTrafficPolicy>}，由 {@code GatewayTrafficGovernance.RequestPermit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policies; its type is {@code List<RuntimeTrafficPolicy>}, and {@code GatewayTrafficGovernance.RequestPermit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance.RequestPermit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance.RequestPermit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final List<RuntimeTrafficPolicy> policies;

        /**
         * 中文说明：保存 context 对应的状态、依赖或配置值；字段类型为 {@code GatewayTrafficContext}，由 {@code GatewayTrafficGovernance.RequestPermit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by context; its type is {@code GatewayTrafficContext}, and {@code GatewayTrafficGovernance.RequestPermit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance.RequestPermit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance.RequestPermit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayTrafficContext context;

        /**
         * 中文说明：保存 超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayTrafficGovernance.RequestPermit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by timeout; its type is {@code Duration}, and {@code GatewayTrafficGovernance.RequestPermit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance.RequestPermit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance.RequestPermit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Duration timeout;

        /**
         * 中文说明：保存 permits 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayBulkheadRegistry.Permit>}，由 {@code GatewayTrafficGovernance.RequestPermit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by permits; its type is {@code List<GatewayBulkheadRegistry.Permit>}, and {@code GatewayTrafficGovernance.RequestPermit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance.RequestPermit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance.RequestPermit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final List<GatewayBulkheadRegistry.Permit> permits;

        /**
         * 中文说明：创建 {@code GatewayTrafficGovernance.RequestPermit} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayTrafficGovernance.RequestPermit} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param policies 参数 policies；parameter policies。
         * @param context 参数 context；parameter context。
         * @param timeout 参数 超时；parameter timeout。
         * @param permits 参数 permits；parameter permits。
         */
        private RequestPermit(
                List<RuntimeTrafficPolicy> policies,
                GatewayTrafficContext context,
                Duration timeout,
                List<GatewayBulkheadRegistry.Permit> permits) {
            this.policies = List.copyOf(policies);
            this.context = context;
            this.timeout = timeout;
            this.permits = List.copyOf(permits);
        }

        /**
         * 中文说明：执行 超时 操作；该方法是 {@code GatewayTrafficGovernance.RequestPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the timeout operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.RequestPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.RequestPermit.timeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 超时 的处理结果；returns the result of the operation.
         */
        public Duration timeout() {
            return timeout;
        }

        /**
         * 中文说明：执行 重试策略 操作；该方法是 {@code GatewayTrafficGovernance.RequestPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the retry policy operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.RequestPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.RequestPermit.retryPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 重试策略 的处理结果；returns the result of the operation.
         */
        public GatewayRetryPolicy retryPolicy() {
            RuntimeTrafficPolicy policy = policies.stream()
                    .filter(value -> value.type()
                            == TrafficPolicyType.RETRY)
                    .findFirst()
                    .orElse(null);
            if (policy == null) {
                return GatewayRetryPolicy.disabled();
            }
            Map<String, Object> values = policy.parameters();
            boolean enabled = Boolean.parseBoolean(
                    string(values, "enabled", "true")
            );
            return new GatewayRetryPolicy(
                    enabled,
                    enabled
                            ? integer(values, "maxAttempts", 2)
                            : 1,
                    duration(
                            values,
                            "initialBackoff",
                            Duration.ofMillis(10)
                    ),
                    duration(
                            values,
                            "maximumBackoff",
                            Duration.ofMillis(100)
                    ),
                    Double.parseDouble(string(
                            values,
                            "multiplier",
                            "2"
                    )),
                    duration(
                            values,
                            "minimumAttemptBudget",
                            Duration.ofMillis(20)
                    ),
                    integerSet(
                            values,
                            "retryableHttpStatuses",
                            Set.of(502, 503, 504)
                    ),
                    stringSet(
                            values,
                            "retryableRpcStatuses",
                            Set.of(
                                    "UNAVAILABLE",
                                    "RESOURCE_EXHAUSTED",
                                    "ABORTED"
                            )
                    )
            );
        }

        /**
         * 中文说明：执行 请求SizeLimit 操作；该方法是 {@code GatewayTrafficGovernance.RequestPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the request size limit operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.RequestPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.RequestPermit.requestSizeLimit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param hardLimit 参数 hardLimit；parameter hard limit。
         * @return 返回 请求SizeLimit 的处理结果；returns the result of the operation.
         */
        public long requestSizeLimit(long hardLimit) {
            return bodySizeLimit(TrafficPolicyType.REQUEST_SIZE, hardLimit);
        }

        /**
         * 中文说明：执行 响应SizeLimit 操作；该方法是 {@code GatewayTrafficGovernance.RequestPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the response size limit operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.RequestPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.RequestPermit.responseSizeLimit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param hardLimit 参数 hardLimit；parameter hard limit。
         * @return 返回 响应SizeLimit 的处理结果；returns the result of the operation.
         */
        public long responseSizeLimit(long hardLimit) {
            return bodySizeLimit(TrafficPolicyType.RESPONSE_SIZE, hardLimit);
        }

        /**
         * 中文说明：执行 bodySizeLimit 操作；该方法是 {@code GatewayTrafficGovernance.RequestPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the body size limit operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.RequestPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.RequestPermit.bodySizeLimit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param type 参数 type；parameter type。
         * @param hardLimit 参数 hardLimit；parameter hard limit。
         * @return 返回 bodySizeLimit 的处理结果；returns the result of the operation.
         */
        private long bodySizeLimit(
                TrafficPolicyType type,
                long hardLimit) {
            long result = hardLimit;
            for (RuntimeTrafficPolicy policy : policies) {
                if (policy.type() == type) {
                    result = Math.min(
                            result,
                            longValue(
                                    policy.parameters(),
                                    "maxBytes",
                                    hardLimit
                            )
                    );
                }
            }
            return result;
        }

        /**
         * 中文说明：执行 acquireAttempt 操作；该方法是 {@code GatewayTrafficGovernance.RequestPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the acquire attempt operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.RequestPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.RequestPermit.acquireAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param provider 参数 提供方；parameter provider。
         * @return 返回 acquireAttempt 的处理结果；returns the result of the operation.
         */
        public AttemptPermit acquireAttempt(ProviderInstance provider) {
            List<GatewayBulkheadRegistry.Permit> instancePermits =
                    new ArrayList<>();
            GatewayCircuitBreakerRegistry.CallPermission circuit = null;
            try {
                for (RuntimeTrafficPolicy policy : policies) {
                    if (policy.type() == TrafficPolicyType.BULKHEAD
                            && policy.scope()
                            == TrafficPolicyScope.PROVIDER_INSTANCE) {
                        GatewayBulkheadRegistry.Permit permit =
                                bulkheads.tryAcquire(
                                        policy.policyId(),
                                        policy.stateEpoch(),
                                        policy.policyVersion(),
                                        provider.runtimeIdentity(),
                                        integer(
                                                policy.parameters(),
                                                "maxConcurrent",
                                                100
                                        )
                                );
                        if (!permit.acquired()) {
                            throw rejected(
                                    "GATEWAY_CONCURRENCY_REJECTED",
                                    503,
                                    "RESOURCE_EXHAUSTED",
                                    0
                            );
                        }
                        instancePermits.add(permit);
                    }
                    if (policy.type()
                            == TrafficPolicyType.CIRCUIT_BREAKER) {
                        circuit = circuits.tryAcquire(
                                policy.policyId(),
                                policy.stateEpoch(),
                                policy.policyVersion(),
                                provider.runtimeIdentity(),
                                circuitPolicy(policy.parameters())
                        );
                        if (!circuit.acquired()) {
                            throw rejected(
                                    "GATEWAY_CIRCUIT_OPEN",
                                    503,
                                    "UNAVAILABLE",
                                    0
                            );
                        }
                    }
                }
                return new AttemptPermit(instancePermits, circuit);
            } catch (RuntimeException failure) {
                instancePermits.forEach(
                        GatewayBulkheadRegistry.Permit::close
                );
                if (circuit != null) {
                    circuit.close();
                }
                throw failure;
            }
        }

        /**
         * 中文说明：执行 context 操作；该方法是 {@code GatewayTrafficGovernance.RequestPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the context operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.RequestPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.RequestPermit.context(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 context 的处理结果；returns the result of the operation.
         */
        public GatewayTrafficContext context() {
            return context;
        }

        /**
         * 中文说明：执行 close 操作；该方法是 {@code GatewayTrafficGovernance.RequestPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.RequestPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.RequestPermit.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        @Override
        public void close() {
            permits.forEach(GatewayBulkheadRegistry.Permit::close);
        }
    }

    /**
     * 中文说明：{@code AttemptPermit} 是类型，位于当前 Gateway 模块的相关包中，负责AttemptPermit相关的职责与边界。
     * English summary: {@code AttemptPermit} is a type in the current Gateway module; it owns the attempt permit-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static final class AttemptPermit implements AutoCloseable {

        /**
         * 中文说明：保存 permits 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayBulkheadRegistry.Permit>}，由 {@code GatewayTrafficGovernance.AttemptPermit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by permits; its type is {@code List<GatewayBulkheadRegistry.Permit>}, and {@code GatewayTrafficGovernance.AttemptPermit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance.AttemptPermit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance.AttemptPermit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final List<GatewayBulkheadRegistry.Permit> permits;

        /**
         * 中文说明：保存 circuit 对应的状态、依赖或配置值；字段类型为 {@code GatewayCircuitBreakerRegistry.CallPermission}，由 {@code GatewayTrafficGovernance.AttemptPermit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by circuit; its type is {@code GatewayCircuitBreakerRegistry.CallPermission}, and {@code GatewayTrafficGovernance.AttemptPermit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficGovernance.AttemptPermit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficGovernance.AttemptPermit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayCircuitBreakerRegistry.CallPermission circuit;

        /**
         * 中文说明：创建 {@code GatewayTrafficGovernance.AttemptPermit} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayTrafficGovernance.AttemptPermit} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param permits 参数 permits；parameter permits。
         * @param circuit 参数 circuit；parameter circuit。
         */
        private AttemptPermit(
                List<GatewayBulkheadRegistry.Permit> permits,
                GatewayCircuitBreakerRegistry.CallPermission circuit) {
            this.permits = List.copyOf(permits);
            this.circuit = circuit;
        }

        /**
         * 中文说明：执行 complete 操作；该方法是 {@code GatewayTrafficGovernance.AttemptPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the complete operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.AttemptPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.AttemptPermit.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param classification 参数 classification；parameter classification。
         */
        public void complete(ProviderCallClassification classification) {
            if (circuit != null) {
                circuit.complete(classification);
            }
            closePermits();
        }

        /**
         * 中文说明：执行 close 操作；该方法是 {@code GatewayTrafficGovernance.AttemptPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.AttemptPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.AttemptPermit.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        @Override
        public void close() {
            if (circuit != null) {
                circuit.close();
            }
            closePermits();
        }

        /**
         * 中文说明：执行 closePermits 操作；该方法是 {@code GatewayTrafficGovernance.AttemptPermit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the close permits operation; this method is the invocation entry point on {@code GatewayTrafficGovernance.AttemptPermit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.AttemptPermit.closePermits(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void closePermits() {
            permits.forEach(GatewayBulkheadRegistry.Permit::close);
        }
    }

    /**
     * 中文说明：执行 circuit策略 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the circuit policy operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.circuitPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @return 返回 circuit策略 的处理结果；returns the result of the operation.
     */
    private GatewayCircuitBreakerRegistry.CircuitPolicy circuitPolicy(
            Map<String, Object> values) {
        return new GatewayCircuitBreakerRegistry.CircuitPolicy(
                Float.parseFloat(string(
                        values,
                        "failureRateThreshold",
                        "50"
                )),
                integer(values, "slidingWindowSize", 20),
                integer(values, "minimumNumberOfCalls", 10),
                duration(
                        values,
                        "openDuration",
                        Duration.ofSeconds(30)
                ),
                integer(values, "halfOpenPermits", 3)
        );
    }

    /**
     * 中文说明：执行 integerSet 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the integer set operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.integerSet(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param key 参数 键；parameter key。
     * @param defaults 参数 defaults；parameter defaults。
     * @return 返回 integerSet 的处理结果；returns the result of the operation.
     */
    private Set<Integer> integerSet(
            Map<String, Object> values,
            String key,
            Set<Integer> defaults) {
        return stringSet(
                values,
                key,
                defaults.stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.toSet())
        ).stream().map(Integer::valueOf).collect(
                java.util.stream.Collectors.toUnmodifiableSet()
        );
    }

    /**
     * 中文说明：执行 stringSet 操作；该方法是 {@code GatewayTrafficGovernance} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string set operation; this method is the invocation entry point on {@code GatewayTrafficGovernance} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficGovernance.stringSet(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param key 参数 键；parameter key。
     * @param defaults 参数 defaults；parameter defaults。
     * @return 返回 stringSet 的处理结果；returns the result of the operation.
     */
    private Set<String> stringSet(
            Map<String, Object> values,
            String key,
            Set<String> defaults) {
        Object raw = values.get(key);
        if (raw == null) {
            return defaults;
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (raw instanceof Iterable<?> iterable) {
            iterable.forEach(value -> result.add(value.toString().trim()));
        } else {
            for (String value : raw.toString().split(",")) {
                result.add(value.trim());
            }
        }
        if (result.contains("")) {
            throw new IllegalArgumentException(
                    key + " must not contain blank values"
            );
        }
        return Set.copyOf(result);
    }
}
