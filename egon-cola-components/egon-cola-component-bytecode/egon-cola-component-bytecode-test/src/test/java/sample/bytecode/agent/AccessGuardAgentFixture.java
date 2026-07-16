package sample.bytecode.agent;

import top.egon.cola.component.accessguard.annotation.AccessGuard;
import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.blacklist.BlacklistStatus;
import top.egon.cola.component.accessguard.config.AccessGuardAnnotationResolver;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.config.DefaultAccessGuardConfigProvider;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.event.AccessGuardEvent;
import top.egon.cola.component.accessguard.execution.AccessGuardExecutionService;
import top.egon.cola.component.accessguard.execution.AccessGuardFailureHandler;
import top.egon.cola.component.accessguard.execution.ConstructorAccessGuardExecutionService;
import top.egon.cola.component.accessguard.execution.ConstructorAccessGuardValidator;
import top.egon.cola.component.accessguard.key.DefaultAccessKeyResolver;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterDecision;
import top.egon.cola.component.accessguard.reject.ReflectionFallbackInvoker;
import top.egon.cola.component.accessguard.whitelist.WhiteListDecision;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AccessGuardAgentFixture {

    private AccessGuardAgentFixture() {
    }

    public static void main(String[] args) {
        List<AccessGuardEvent> guardEvents = new ArrayList<>();
        List<ObservationEvent> observationEvents = new ArrayList<>();
        AccessGuardProperties properties = new AccessGuardProperties();
        AccessGuardRuleResolver rules = new AccessGuardRuleResolver(
                properties,
                new DefaultAccessGuardConfigProvider(),
                new AccessGuardAnnotationResolver()
        );
        DefaultAccessKeyResolver keys = new DefaultAccessKeyResolver();
        AccessGuardFailureHandler failures = new AccessGuardFailureHandler(properties);
        BlacklistService blacklist = new BlacklistService() {
            @Override
            public BlacklistStatus status(AccessGuardRule rule, AccessGuardContext context) {
                return BlacklistStatus.none();
            }

            @Override
            public BlacklistStatus incrementRejectAndMaybeBlacklist(
                    AccessGuardRule rule, AccessGuardContext context) {
                return BlacklistStatus.none();
            }

            @Override
            public void remove(String ruleName, String accessKeyHash) {
            }
        };
        var whiteList = (top.egon.cola.component.accessguard.whitelist.WhiteListService)
                (rule, keyHash) -> rule.name().startsWith("reject")
                        ? WhiteListDecision.reject("policy")
                        : WhiteListDecision.pass(rule.whiteListMode());
        var rateLimiter = (top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor)
                (rule, context) -> RateLimiterDecision.allow(1);
        ReflectionFallbackInvoker rejects = new ReflectionFallbackInvoker();
        AccessGuardExecutionService methodService = new AccessGuardExecutionService(
                properties,
                rules,
                keys,
                whiteList,
                blacklist,
                rateLimiter,
                (joinPoint, rule, context) -> joinPoint.proceed(),
                rejects,
                guardEvents::add,
                failures
        );
        ConstructorAccessGuardExecutionService constructorService =
                new ConstructorAccessGuardExecutionService(
                        properties,
                        rules,
                        new ConstructorAccessGuardValidator(),
                        keys,
                        whiteList,
                        blacklist,
                        rateLimiter,
                        guardEvents::add,
                        failures
                );
        AccessGuardRuntimeAdapter adapter = new AccessGuardRuntimeAdapter(
                () -> methodService,
                () -> constructorService,
                new MethodMetadataResolver(),
                rules,
                failures
        );
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
            require(guardEvents.stream().noneMatch(event ->
                            event.toString().contains("password=secret")),
                    "guard event leaked arguments");
            require(runtimeFailures.failures().isEmpty(),
                    "runtime diagnostics are not empty");
            System.out.println("ACCESS_GUARD_AGENT_OK events=" + guardEvents.size()
                    + " observations=" + observationEvents.size());
        }
    }

    private static ExecutorTaskDecorator taskDecorator(BoundedFailureStore failures) {
        return new ExecutorTaskDecorator(
                new CompositeContextCarrier(List.of()),
                new RuntimeEventFanout(List.of(), failures),
                new RuntimeTaskDetector(),
                new ExecutorNameResolver(List.of(), Map.of())
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class Target {

        private final int value;

        @AccessGuard(
                name = "constructor",
                key = "value",
                failStrategy = FailStrategy.FAIL_CLOSED
        )
        public Target(int value) {
            this.value = value;
        }

        @AccessGuard(
                name = "private-constructor",
                key = "value",
                failStrategy = FailStrategy.FAIL_CLOSED
        )
        private Target(Integer value) {
            this.value = value;
        }

        static Target createPrivate() {
            return new Target(Integer.valueOf(9));
        }

        @AccessGuard(name = "allowed")
        public String allowed(String sensitive) {
            return "allowed";
        }

        @AccessGuard(name = "reject-method", whitelist = true)
        public String rejected() {
            return "body";
        }

        @AccessGuard(name = "private")
        private String privateValue() {
            return "private";
        }

        String callPrivate() {
            return privateValue();
        }

        @AccessGuard(
                name = "reject-static",
                whitelist = true,
                fallbackMethod = "staticFallback"
        )
        public static String staticRejected() {
            return "body";
        }

        private static String staticFallback() {
            return "static-fallback";
        }

        @AccessGuard(name = "synchronized")
        public synchronized String synchronizedValue() {
            return "synchronized";
        }

        @AccessGuard(name = "recursive")
        public int recursive(int remaining) {
            return remaining == 0 ? 0 : 1 + recursive(remaining - 1);
        }
    }
}
