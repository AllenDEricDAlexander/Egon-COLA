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

public final class GatewayTrafficGovernance {

    private final Supplier<CompiledGatewayRules> rules;

    private final LocalTokenBucketRateLimiter localRateLimiter;

    private final DistributedTokenBucketRateLimiter distributedRateLimiter;

    private final GatewayBulkheadRegistry bulkheads =
            new GatewayBulkheadRegistry();

    private final GatewayCircuitBreakerRegistry circuits =
            new GatewayCircuitBreakerRegistry();

    private final GatewayPolicyKeyCompiler keyCompiler =
            new GatewayPolicyKeyCompiler();

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

    public static GatewayTrafficGovernance noop() {
        return new GatewayTrafficGovernance(() -> null, null);
    }

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

    private Duration effectiveTimeout(
            List<RuntimeTrafficPolicy> policies,
            Duration fallback) {
        Duration result = null;
        for (RuntimeTrafficPolicy policy : policies) {
            if (policy.type() == TrafficPolicyType.TIMEOUT) {
                Duration configured = duration(
                        policy.parameters(),
                        "timeout",
                        null
                );
                if (configured != null
                        && (result == null
                        || configured.compareTo(result) < 0)) {
                    result = configured;
                }
            }
        }
        return result == null ? fallback : result;
    }

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

    private boolean distributed(RuntimeTrafficPolicy policy) {
        return "DISTRIBUTED".equalsIgnoreCase(string(
                policy.parameters(),
                "mode",
                "LOCAL"
        ));
    }

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

    private String string(
            Map<String, Object> source,
            String key,
            String defaultValue) {
        Object value = source.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private int integer(
            Map<String, Object> source,
            String key,
            int defaultValue) {
        return Math.toIntExact(longValue(source, key, defaultValue));
    }

    private long longValue(
            Map<String, Object> source,
            String key,
            long defaultValue) {
        Object value = source.get(key);
        return value == null ? defaultValue : number(value);
    }

    private long number(Object value) {
        return value instanceof Number number
                ? number.longValue()
                : Long.parseLong(value.toString());
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public final class RequestPermit implements AutoCloseable {

        private final List<RuntimeTrafficPolicy> policies;

        private final GatewayTrafficContext context;

        private final Duration timeout;

        private final List<GatewayBulkheadRegistry.Permit> permits;

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

        public Duration timeout() {
            return timeout;
        }

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

        public long requestSizeLimit(long hardLimit) {
            return bodySizeLimit(TrafficPolicyType.REQUEST_SIZE, hardLimit);
        }

        public long responseSizeLimit(long hardLimit) {
            return bodySizeLimit(TrafficPolicyType.RESPONSE_SIZE, hardLimit);
        }

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

        public GatewayTrafficContext context() {
            return context;
        }

        @Override
        public void close() {
            permits.forEach(GatewayBulkheadRegistry.Permit::close);
        }
    }

    public static final class AttemptPermit implements AutoCloseable {

        private final List<GatewayBulkheadRegistry.Permit> permits;

        private final GatewayCircuitBreakerRegistry.CallPermission circuit;

        private AttemptPermit(
                List<GatewayBulkheadRegistry.Permit> permits,
                GatewayCircuitBreakerRegistry.CallPermission circuit) {
            this.permits = List.copyOf(permits);
            this.circuit = circuit;
        }

        public void complete(ProviderCallClassification classification) {
            if (circuit != null) {
                circuit.complete(classification);
            }
            closePermits();
        }

        @Override
        public void close() {
            if (circuit != null) {
                circuit.close();
            }
            closePermits();
        }

        private void closePermits() {
            permits.forEach(GatewayBulkheadRegistry.Permit::close);
        }
    }

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
