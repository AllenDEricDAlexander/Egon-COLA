package sample.bytecode.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.accessguard.adapter.aop.GuardBindingResolver;
import top.egon.cola.component.accessguard.api.AccessGuard;
import top.egon.cola.component.accessguard.core.DefaultGuardEngine;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.failure.DefaultFailurePolicyResolver;
import top.egon.cola.component.accessguard.core.plan.DefaultGuardPlanResolver;
import top.egon.cola.component.accessguard.core.plan.GuardPlanProperties;
import top.egon.cola.component.accessguard.core.plan.GuardPlanValidator;
import top.egon.cola.component.accessguard.core.plan.PropertiesGuardPlanSource;
import top.egon.cola.component.accessguard.execution.DefaultRejectionHandler;
import top.egon.cola.component.accessguard.execution.FallbackMethodCache;
import top.egon.cola.component.accessguard.execution.JsonRejectValueParser;
import top.egon.cola.component.accessguard.execution.MethodHandleFallbackHandler;
import top.egon.cola.component.accessguard.execution.RejectionMode;
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
        GuardPlanProperties properties = properties();
        DefaultGuardPlanResolver plans = new DefaultGuardPlanResolver(
                List.of(new PropertiesGuardPlanSource(properties)),
                new GuardPlanValidator());
        DenyListPolicy deny = new DenyListPolicy((ruleId, version, hash) -> false);
        AllowListPolicy allow = new AllowListPolicy(
                (ruleId, version, hash) -> !ruleId.startsWith("reject"));
        PenaltyBoxPolicy penalty = new PenaltyBoxPolicy(key -> Optional.empty());
        RateLimitPolicy rate = new RateLimitPolicy(
                request -> new RateLimitDecision(true, 1, Duration.ZERO));
        FallbackMethodCache fallbackCache = new FallbackMethodCache();
        fallbackCache.validateAndCache(
                Target.class.getDeclaredMethod("staticRejected"), "staticFallback");
        return new DefaultGuardEngine(
                plans,
                (invocation, config) -> new GuardKeyResolution(
                        GuardKeyScope.GLOBAL, List.of(), KEY_HASH),
                AdmissionPolicies.builtIns(deny, allow, penalty, rate),
                Map.of("penalty-box", penalty, "rate-limit", rate),
                new DefaultFailurePolicyResolver(),
                (context, config) -> new PenaltyState(0, false, null, null),
                (invocation, config) -> invocation.continuation().execute(),
                new DefaultRejectionHandler(
                        new MethodHandleFallbackHandler(fallbackCache),
                        new JsonRejectValueParser(new ObjectMapper())),
                System::nanoTime,
                "LOCAL",
                "AGENT",
                new CompositeGuardEventPublisher(List.of(events::add)));
    }

    private static GuardPlanProperties properties() {
        GuardPlanProperties properties = new GuardPlanProperties();
        properties.getKey().setHmacSecret("integration-secret");
        Map<String, GuardPlanProperties.Rule> rules = new LinkedHashMap<>();
        for (String ruleId : List.of(
                "constructor", "private-constructor", "allowed", "private",
                "synchronized", "recursive")) {
            rules.put(ruleId, new GuardPlanProperties.Rule());
        }
        GuardPlanProperties.Rule rejected = new GuardPlanProperties.Rule();
        rejected.getAllowList().setEnabled(true);
        rejected.getAllowList().setMode(AllowListMode.GATE);
        rejected.getRejection().setMode(RejectionMode.RETURN_JSON);
        rejected.getRejection().setReturnJson("\"access rejected\"");
        rules.put("reject-method", rejected);
        GuardPlanProperties.Rule fallback = new GuardPlanProperties.Rule();
        fallback.getAllowList().setEnabled(true);
        fallback.getAllowList().setMode(AllowListMode.GATE);
        fallback.getRejection().setMode(RejectionMode.FALLBACK);
        fallback.getRejection().setFallbackMethod("staticFallback");
        rules.put("reject-static", fallback);
        properties.setRules(rules);
        return properties;
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

    static final class Target {

        private final int value;

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
    }
}
