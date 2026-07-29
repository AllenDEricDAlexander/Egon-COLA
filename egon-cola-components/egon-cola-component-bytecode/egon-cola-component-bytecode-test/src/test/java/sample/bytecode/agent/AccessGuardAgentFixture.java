package sample.bytecode.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.accessguard.adapter.aop.GuardBindingResolver;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.core.DefaultGuardEngine;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.GuardResolution;
import top.egon.cola.component.accessguard.core.failure.DefaultFailurePolicyResolver;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;
import top.egon.cola.component.accessguard.core.plan.DefaultGuardPlanResolver;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.core.plan.GuardPlanValidator;
import top.egon.cola.component.accessguard.core.plan.PropertiesGuardPlanSource;
import top.egon.cola.component.accessguard.execution.DefaultRejectionHandler;
import top.egon.cola.component.accessguard.execution.FallbackMethodCache;
import top.egon.cola.component.accessguard.execution.JsonRejectValueParser;
import top.egon.cola.component.accessguard.execution.MethodHandleFallbackHandler;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitExceededException;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.key.GuardKeyResolution;
import top.egon.cola.component.accessguard.key.GuardKeyScope;
import top.egon.cola.component.accessguard.observability.CompositeGuardEventPublisher;
import top.egon.cola.component.accessguard.observability.GuardEvent;
import top.egon.cola.component.accessguard.policy.AdmissionPolicies;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;
import top.egon.cola.component.accessguard.policy.allow.AllowListPolicy;
import top.egon.cola.component.accessguard.policy.deny.DenyListPolicy;
import top.egon.cola.component.accessguard.policy.penalty.PenaltyBoxPolicy;
import top.egon.cola.component.accessguard.policy.ratelimit.RateLimitPolicy;
import top.egon.cola.component.accessguard.store.PenaltyState;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.StoreOperationException;
import top.egon.cola.component.bytecode.api.observation.ObservationEvent;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.runtime.DefaultBytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.runtime.context.CompositeContextCarrier;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;
import top.egon.cola.component.bytecode.runtime.event.RuntimeEventFanout;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorNameResolver;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorTaskDecorator;
import top.egon.cola.component.bytecode.runtime.executor.RuntimeTaskDetector;
import top.egon.cola.component.bytecode.runtime.observation.ObservationRuntime;
import top.egon.cola.component.bytecode.starter.accessguard.AccessGuardRuntimeAdapter;
import top.egon.cola.component.bytecode.starter.accessguard.CombinedPolicyDispatcher;
import top.egon.cola.component.bytecode.starter.methodextension.MethodMetadataResolver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AccessGuardAgentFixture {

    private static final String KEY_HASH =
            "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";

    private static final Map<String, ScenarioExpectation> SCENARIOS = Map.ofEntries(
            Map.entry("scenario-allow", new ScenarioExpectation(
                    "business", GuardDecision.PASS, GuardResolution.NONE, 1)),
            Map.entry("scenario-deny", new ScenarioExpectation(
                    "deny", GuardDecision.DENY_LIST_HIT, GuardResolution.RETURN_JSON, 0)),
            Map.entry("scenario-penalty", new ScenarioExpectation(
                    "penalty", GuardDecision.PENALTY_ACTIVE, GuardResolution.RETURN_JSON, 0)),
            Map.entry("scenario-rate-limit", new ScenarioExpectation(
                    "rate-limit", GuardDecision.RATE_LIMITED, GuardResolution.RETURN_JSON, 0)),
            Map.entry("scenario-fail-open", new ScenarioExpectation(
                    "business", GuardDecision.STORE_FAILED, GuardResolution.FAIL_OPEN, 1)),
            Map.entry("scenario-local-fallback", new ScenarioExpectation(
                    "business", GuardDecision.STORE_FAILED, GuardResolution.LOCAL_FALLBACK, 1)),
            Map.entry("scenario-timeout", new ScenarioExpectation(
                    "timeout", GuardDecision.TIME_LIMIT_EXCEEDED, GuardResolution.RETURN_JSON, 0)),
            Map.entry("scenario-fallback", new ScenarioExpectation(
                    "fallback", GuardDecision.DENY_LIST_HIT, GuardResolution.FALLBACK, 0)));

    private AccessGuardAgentFixture() {
    }

    public static void main(String[] args) throws Exception {
        List<GuardEvent> guardEvents = new ArrayList<>();
        List<ObservationEvent> observationEvents = new ArrayList<>();
        DefaultGuardEngine engine = engine(guardEvents);
        AccessGuardRuntimeAdapter adapter = new AccessGuardRuntimeAdapter(
                engine,
                new MethodMetadataResolver(),
                new GuardBindingResolver());
        adapter.markReady();
        BoundedFailureStore runtimeFailures = new BoundedFailureStore(8);
        ObservationRuntime observation = new ObservationRuntime(
                true, 1.0, -1L, observationEvents::add, runtimeFailures);
        DefaultBytecodeRuntimeDispatcher base = new DefaultBytecodeRuntimeDispatcher(
                taskDecorator(runtimeFailures), false, observation, null);
        CombinedPolicyDispatcher dispatcher = new CombinedPolicyDispatcher(base, adapter);
        ClassLoader loader = AccessGuardAgentFixture.class.getClassLoader();

        try (var registration = DispatcherRegistry.register(loader, "integration", dispatcher)) {
            Target target = new Target(7);
            assertScenarios(target, guardEvents);
            require(target.allowed("password=secret").equals("allowed"),
                    "allowed method changed");
            require(target.rejected().equals("access rejected"),
                    "rejected method changed");
            require(target.callPrivate().equals("private"),
                    "private method changed");
            require(Target.staticRejected().equals("static-fallback"),
                    "static fallback changed");
            require(target.synchronizedValue().equals("synchronized"),
                    "synchronized method changed");
            require(target.recursive(2) == 2, "recursive method changed");
            require(Target.createPrivate().value == 9, "private constructor changed");

            require(observationEvents.stream().anyMatch(event ->
                            event.methodName().equals("allowed")),
                    "allowed body observation missing");
            require(observationEvents.stream().noneMatch(event ->
                            event.methodName().equals("rejected")),
                    "rejected method reached observation");
            require(guardEvents.size() >= 10, "guard events missing: " + guardEvents);
            require(guardEvents.stream().map(GuardEvent::outcome).map(GuardOutcome::decision)
                            .anyMatch(decision -> decision.name().equals("ALLOW_LIST_MISS")),
                    "rejection outcome missing");
            require(guardEvents.stream().noneMatch(event ->
                            event.toString().contains("password=secret")),
                    "guard event leaked arguments");
            require(runtimeFailures.failures().isEmpty(),
                    "runtime diagnostics are not empty");
            System.out.println("ACCESS_GUARD_AGENT_OK events=" + guardEvents.size()
                    + " observations=" + observationEvents.size());
        }
    }

    private static DefaultGuardEngine engine(List<GuardEvent> events) throws Exception {
        AccessGuardProperties properties = properties();
        DefaultGuardPlanResolver plans = new DefaultGuardPlanResolver(
                List.of(new PropertiesGuardPlanSource(properties)),
                new GuardPlanValidator());
        DenyListPolicy deny = new DenyListPolicy((ruleId, version, hash) -> {
            if (ruleId.equals("scenario-fail-open") || ruleId.equals("scenario-local-fallback")) {
                throw new StoreOperationException("primary unavailable");
            }
            return ruleId.equals("scenario-deny") || ruleId.equals("scenario-fallback");
        });
        AllowListPolicy allow = new AllowListPolicy(
                (ruleId, version, hash) -> !ruleId.startsWith("reject"));
        PenaltyBoxPolicy penalty = new PenaltyBoxPolicy(key ->
                key.ruleId().equals("scenario-penalty")
                        ? Optional.of(new PenaltyState(
                        5, true, java.time.Instant.now().plusSeconds(60),
                        java.time.Instant.now().plusSeconds(600)))
                        : Optional.empty());
        RateLimitPolicy rate = new RateLimitPolicy(request ->
                request.ruleId().equals("scenario-rate-limit")
                        ? new RateLimitDecision(false, 0, Duration.ofSeconds(1))
                        : new RateLimitDecision(true, 1, Duration.ZERO));
        DenyListPolicy localDeny = new DenyListPolicy((ruleId, version, hash) -> false);
        FallbackMethodCache fallbackCache = new FallbackMethodCache();
        fallbackCache.validateAndCache(
                Target.class.getDeclaredMethod("staticRejected"), "staticFallback");
        fallbackCache.validateAndCache(
                Target.class.getDeclaredMethod("scenarioFallback"), "scenarioFallbackValue");
        return new DefaultGuardEngine(
                plans,
                (invocation, config) -> new GuardKeyResolution(
                        GuardKeyScope.GLOBAL, List.of(), KEY_HASH),
                AdmissionPolicies.builtIns(deny, allow, penalty, rate),
                Map.of("deny-list", localDeny, "penalty-box", penalty, "rate-limit", rate),
                new DefaultFailurePolicyResolver(),
                (context, config) -> new PenaltyState(0, false, null, null),
                (invocation, config) -> {
                    if (invocation.ruleId().equals("scenario-timeout")) {
                        throw new TimeLimitExceededException(config.timeout());
                    }
                    return invocation.continuation().execute();
                },
                new DefaultRejectionHandler(
                        new MethodHandleFallbackHandler(fallbackCache),
                        new JsonRejectValueParser(new ObjectMapper())),
                System::nanoTime,
                "LOCAL",
                "AGENT",
                new CompositeGuardEventPublisher(List.of(events::add)));
    }

    private static AccessGuardProperties properties() {
        AccessGuardProperties properties = new AccessGuardProperties();
        properties.getKey().setHmacSecret("integration-secret");
        Map<String, AccessGuardProperties.Rule> rules = new LinkedHashMap<>();
        for (String ruleId : List.of(
                "constructor", "private-constructor", "allowed", "private",
                "synchronized", "recursive")) {
            rules.put(ruleId, new AccessGuardProperties.Rule());
        }
        AccessGuardProperties.Rule rejected = new AccessGuardProperties.Rule();
        rejected.getAllowList().setEnabled(true);
        rejected.getAllowList().setMode(AllowListMode.GATE);
        rejected.getRejection().setMode(RejectionMode.RETURN_JSON);
        rejected.getRejection().setReturnJson("\"access rejected\"");
        rules.put("reject-method", rejected);
        AccessGuardProperties.Rule fallback = new AccessGuardProperties.Rule();
        fallback.getAllowList().setEnabled(true);
        fallback.getAllowList().setMode(AllowListMode.GATE);
        fallback.getRejection().setMode(RejectionMode.FALLBACK);
        fallback.getRejection().setFallbackMethod("staticFallback");
        rules.put("reject-static", fallback);
        rules.put("scenario-allow", new AccessGuardProperties.Rule());

        AccessGuardProperties.Rule denied = rejected("\"deny\"");
        denied.getDenyList().setEnabled(true);
        rules.put("scenario-deny", denied);

        AccessGuardProperties.Rule penalized = rejected("\"penalty\"");
        penalized.getPenaltyBox().setEnabled(true);
        rules.put("scenario-penalty", penalized);

        AccessGuardProperties.Rule rateLimited = rejected("\"rate-limit\"");
        rateLimited.getRateLimit().setEnabled(true);
        rules.put("scenario-rate-limit", rateLimited);

        AccessGuardProperties.Rule failOpen = new AccessGuardProperties.Rule();
        failOpen.getDenyList().setEnabled(true);
        failOpen.getFailurePolicies().setDenyListStore(FailurePolicy.FAIL_OPEN);
        rules.put("scenario-fail-open", failOpen);

        AccessGuardProperties.Rule localFallback = new AccessGuardProperties.Rule();
        localFallback.getDenyList().setEnabled(true);
        localFallback.getFailurePolicies().setDenyListStore(FailurePolicy.LOCAL_FALLBACK);
        rules.put("scenario-local-fallback", localFallback);

        AccessGuardProperties.Rule timeout = rejected("\"timeout\"");
        timeout.getTimeLimit().setEnabled(true);
        timeout.getTimeLimit().setMode(TimeLimitMode.ENFORCE);
        timeout.getTimeLimit().setExecutor(TimeLimiterType.VIRTUAL_THREAD);
        timeout.getTimeLimit().setTimeout(Duration.ofMillis(50));
        rules.put("scenario-timeout", timeout);

        AccessGuardProperties.Rule scenarioFallback = new AccessGuardProperties.Rule();
        scenarioFallback.getDenyList().setEnabled(true);
        scenarioFallback.getRejection().setMode(RejectionMode.FALLBACK);
        scenarioFallback.getRejection().setFallbackMethod("scenarioFallbackValue");
        rules.put("scenario-fallback", scenarioFallback);
        properties.setRules(rules);
        return properties;
    }

    private static AccessGuardProperties.Rule rejected(String returnJson) {
        AccessGuardProperties.Rule rule = new AccessGuardProperties.Rule();
        rule.getRejection().setMode(RejectionMode.RETURN_JSON);
        rule.getRejection().setReturnJson(returnJson);
        return rule;
    }

    private static void assertScenarios(Target target, List<GuardEvent> events) {
        int firstEvent = events.size();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("scenario-allow", target.scenarioAllow());
        values.put("scenario-deny", target.scenarioDeny());
        values.put("scenario-penalty", target.scenarioPenalty());
        values.put("scenario-rate-limit", target.scenarioRateLimit());
        values.put("scenario-fail-open", target.scenarioFailOpen());
        values.put("scenario-local-fallback", target.scenarioLocalFallback());
        values.put("scenario-timeout", target.scenarioTimeout());
        values.put("scenario-fallback", target.scenarioFallback());

        List<GuardEvent> scenarioEvents = events.subList(firstEvent, events.size());
        require(scenarioEvents.size() == SCENARIOS.size(),
                "scenario event count changed: " + scenarioEvents);
        Map<String, GuardOutcome> outcomes = new LinkedHashMap<>();
        for (GuardEvent event : scenarioEvents) {
            outcomes.put(event.outcome().ruleId(), event.outcome());
        }
        for (Map.Entry<String, ScenarioExpectation> entry : SCENARIOS.entrySet()) {
            String ruleId = entry.getKey();
            ScenarioExpectation expected = entry.getValue();
            GuardOutcome outcome = outcomes.get(ruleId);
            require(outcome != null, "scenario outcome missing: " + ruleId);
            require(values.get(ruleId).equals(expected.value()),
                    "scenario value changed: " + ruleId + " -> " + values.get(ruleId));
            require(outcome.decision() == expected.decision(),
                    "scenario decision changed: " + ruleId + " -> " + outcome);
            require(outcome.resolution() == expected.resolution(),
                    "scenario resolution changed: " + ruleId + " -> " + outcome);
            require(target.businessCalls(ruleId) == expected.businessCalls(),
                    "scenario business calls changed: " + ruleId);
        }
    }

    private static ExecutorTaskDecorator taskDecorator(BoundedFailureStore failures) {
        return new ExecutorTaskDecorator(
                new CompositeContextCarrier(List.of()),
                new RuntimeEventFanout(List.of(), failures),
                new RuntimeTaskDetector(),
                new ExecutorNameResolver(List.of(), Map.of()));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record ScenarioExpectation(
            String value,
            GuardDecision decision,
            GuardResolution resolution,
            int businessCalls
    ) {
    }

    static final class Target {

        private final int value;
        private final Map<String, Integer> scenarioCalls = new LinkedHashMap<>();

        @AccessGuard("constructor")
        public Target(int value) {
            this.value = value;
        }

        @AccessGuard("private-constructor")
        private Target(Integer value) {
            this.value = value;
        }

        static Target createPrivate() {
            return new Target(Integer.valueOf(9));
        }

        @AccessGuard("allowed")
        public String allowed(String sensitive) {
            return "allowed";
        }

        @AccessGuard("reject-method")
        public String rejected() {
            return "body";
        }

        @AccessGuard("private")
        private String privateValue() {
            return "private";
        }

        String callPrivate() {
            return privateValue();
        }

        @AccessGuard("reject-static")
        public static String staticRejected() {
            return "body";
        }

        private static String staticFallback() {
            return "static-fallback";
        }

        @AccessGuard("synchronized")
        public synchronized String synchronizedValue() {
            return "synchronized";
        }

        @AccessGuard("recursive")
        public int recursive(int remaining) {
            return remaining == 0 ? 0 : 1 + recursive(remaining - 1);
        }

        @AccessGuard("scenario-allow")
        public String scenarioAllow() {
            return scenarioBusiness("scenario-allow");
        }

        @AccessGuard("scenario-deny")
        public String scenarioDeny() {
            return scenarioBusiness("scenario-deny");
        }

        @AccessGuard("scenario-penalty")
        public String scenarioPenalty() {
            return scenarioBusiness("scenario-penalty");
        }

        @AccessGuard("scenario-rate-limit")
        public String scenarioRateLimit() {
            return scenarioBusiness("scenario-rate-limit");
        }

        @AccessGuard("scenario-fail-open")
        public String scenarioFailOpen() {
            return scenarioBusiness("scenario-fail-open");
        }

        @AccessGuard("scenario-local-fallback")
        public String scenarioLocalFallback() {
            return scenarioBusiness("scenario-local-fallback");
        }

        @AccessGuard("scenario-timeout")
        public String scenarioTimeout() {
            return scenarioBusiness("scenario-timeout");
        }

        @AccessGuard("scenario-fallback")
        public String scenarioFallback() {
            return scenarioBusiness("scenario-fallback");
        }

        private String scenarioFallbackValue() {
            return "fallback";
        }

        private String scenarioBusiness(String ruleId) {
            scenarioCalls.merge(ruleId, 1, Integer::sum);
            return "business";
        }

        private int businessCalls(String ruleId) {
            return scenarioCalls.getOrDefault(ruleId, 0);
        }
    }
}
