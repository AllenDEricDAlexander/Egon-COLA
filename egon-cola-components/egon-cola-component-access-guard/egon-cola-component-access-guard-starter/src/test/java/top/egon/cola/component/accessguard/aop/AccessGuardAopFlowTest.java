package top.egon.cola.component.accessguard.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.blacklist.BlacklistStatus;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;
import top.egon.cola.component.accessguard.key.AccessKeyResolution;
import top.egon.cola.component.accessguard.key.AccessKeyResolver;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterDecision;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor;
import top.egon.cola.component.accessguard.reject.RejectResponseInvoker;
import top.egon.cola.component.accessguard.whitelist.WhiteListDecision;
import top.egon.cola.component.accessguard.whitelist.WhiteListService;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.accessguard.annotation.FailStrategy.FAIL_OPEN;
import static top.egon.cola.component.accessguard.annotation.TimeoutExecutorType.THREAD_POOL;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.BYPASS_GUARD;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.GATEKEEPER;

class AccessGuardAopFlowTest {

    @Test
    void shouldRejectNonWhiteListUserWithoutCallingBusiness() throws Throwable {
        Harness harness = new Harness(rule(true, GATEKEEPER, true, true, false));
        harness.whiteListDecision = WhiteListDecision.reject("not white");

        Object result = harness.aop().around(harness.joinPoint());

        assertThat(result).isEqualTo("reject");
        assertThat(harness.businessCalls).hasValue(0);
        assertThat(harness.rateLimiterCalls).hasValue(0);
    }

    @Test
    void shouldContinueIntoRateLimiterWhenWhiteListGatekeeperHits() throws Throwable {
        Harness harness = new Harness(rule(true, GATEKEEPER, true, true, false));
        harness.whiteListDecision = WhiteListDecision.pass(GATEKEEPER);
        harness.rateLimiterDecision = RateLimiterDecision.reject("limited");

        Object result = harness.aop().around(harness.joinPoint());

        assertThat(result).isEqualTo("reject");
        assertThat(harness.rateLimiterCalls).hasValue(1);
    }

    @Test
    void shouldRejectBlacklistHitBeforeRateLimiter() throws Throwable {
        Harness harness = new Harness(rule(false, GATEKEEPER, true, true, false));
        harness.blacklistStatus = BlacklistStatus.hit(3L, System.currentTimeMillis() + 1000);

        Object result = harness.aop().around(harness.joinPoint());

        assertThat(result).isEqualTo("reject");
        assertThat(harness.rateLimiterCalls).hasValue(0);
    }

    @Test
    void shouldIncrementBlacklistWhenRateLimiterRejects() throws Throwable {
        Harness harness = new Harness(rule(false, GATEKEEPER, true, true, false));
        harness.rateLimiterDecision = RateLimiterDecision.reject("limited");

        Object result = harness.aop().around(harness.joinPoint());

        assertThat(result).isEqualTo("reject");
        assertThat(harness.blacklistIncrementCalls).hasValue(1);
    }

    @Test
    void shouldExecuteTimeoutWrapperWhenRateLimiterPasses() throws Throwable {
        Harness harness = new Harness(rule(false, GATEKEEPER, true, false, true));
        harness.rateLimiterDecision = RateLimiterDecision.allow(0L);
        harness.timeoutResult = "timeout-result";

        Object result = harness.aop().around(harness.joinPoint());

        assertThat(result).isEqualTo("timeout-result");
        assertThat(harness.timeoutCalls).hasValue(1);
    }

    @Test
    void shouldProceedDirectlyWhenGlobalSwitchDisabled() throws Throwable {
        Harness harness = new Harness(rule(true, BYPASS_GUARD, true, true, true));
        harness.properties.setEnabled(false);

        Object result = harness.aop().around(harness.joinPoint());

        assertThat(result).isEqualTo("ok");
        assertThat(harness.businessCalls).hasValue(1);
        assertThat(harness.rateLimiterCalls).hasValue(0);
        assertThat(harness.timeoutCalls).hasValue(0);
    }

    private AccessGuardRule rule(
            boolean whiteListEnabled,
            top.egon.cola.component.accessguard.annotation.WhiteListMode whiteListMode,
            boolean rateLimiterEnabled,
            boolean blacklistEnabled,
            boolean timeoutEnabled
    ) {
        return new AccessGuardRule(
                "draw-api",
                "userId",
                "",
                whiteListEnabled,
                List.of("hash001"),
                whiteListMode,
                rateLimiterEnabled,
                1L,
                1L,
                TimeUnit.SECONDS,
                blacklistEnabled,
                3L,
                Duration.ofHours(24),
                false,
                timeoutEnabled,
                Duration.ofMillis(350),
                THREAD_POOL,
                false,
                true,
                "fallback",
                "",
                FAIL_OPEN
        );
    }

    static class Harness {

        private final AccessGuardRule rule;

        private final AccessGuardProperties properties = new AccessGuardProperties();

        private final AtomicInteger businessCalls = new AtomicInteger();

        private final AtomicInteger rateLimiterCalls = new AtomicInteger();

        private final AtomicInteger timeoutCalls = new AtomicInteger();

        private final AtomicInteger blacklistIncrementCalls = new AtomicInteger();

        private WhiteListDecision whiteListDecision = WhiteListDecision.pass(GATEKEEPER);

        private BlacklistStatus blacklistStatus = BlacklistStatus.none();

        private RateLimiterDecision rateLimiterDecision = RateLimiterDecision.allow(0L);

        private Object timeoutResult = "timeout";

        Harness(AccessGuardRule rule) {
            this.rule = rule;
        }

        AccessGuardAop aop() {
            AccessGuardRuleResolver ruleResolver = new AccessGuardRuleResolver() {
                @Override
                public AccessGuardRule resolve(Method method) {
                    return rule;
                }
            };
            AccessKeyResolver keyResolver = (joinPoint, accessGuardRule) -> new AccessKeyResolution("u-001", "u-001", "hash001");
            WhiteListService whiteListService = (accessGuardRule, accessKeyHash) -> whiteListDecision;
            BlacklistService blacklistService = new BlacklistService() {
                @Override
                public BlacklistStatus status(AccessGuardRule accessGuardRule, AccessGuardContext context) {
                    return blacklistStatus;
                }

                @Override
                public BlacklistStatus incrementRejectAndMaybeBlacklist(AccessGuardRule accessGuardRule, AccessGuardContext context) {
                    blacklistIncrementCalls.incrementAndGet();
                    return BlacklistStatus.rejected(1L);
                }

                @Override
                public void remove(String ruleName, String accessKeyHash) {
                }
            };
            RateLimiterExecutor rateLimiterExecutor = (accessGuardRule, context) -> {
                rateLimiterCalls.incrementAndGet();
                return rateLimiterDecision;
            };
            RejectResponseInvoker rejectResponseInvoker = (joinPoint, accessGuardRule, context, args) -> "reject";
            AccessGuardEventPublisher eventPublisher = event -> {
            };
            return new AccessGuardAop(
                    properties,
                    ruleResolver,
                    keyResolver,
                    whiteListService,
                    blacklistService,
                    rateLimiterExecutor,
                    (joinPoint, accessGuardRule, context) -> {
                        timeoutCalls.incrementAndGet();
                        return timeoutResult;
                    },
                    rejectResponseInvoker,
                    eventPublisher
            );
        }

        ProceedingJoinPoint joinPoint() throws NoSuchMethodException {
            Method method = SampleService.class.getDeclaredMethod("guarded", String.class);
            return new TestProceedingJoinPoint(new SampleService(), method, new Object[]{"u-001"}, () -> {
                businessCalls.incrementAndGet();
                return "ok";
            });
        }
    }

    static class SampleService {

        String guarded(String userId) {
            return userId;
        }
    }

    interface ThrowingSupplier {

        Object get() throws Throwable;
    }

    static class TestProceedingJoinPoint implements ProceedingJoinPoint {

        private final Object target;

        private final Method method;

        private final Object[] args;

        private final ThrowingSupplier supplier;

        TestProceedingJoinPoint(Object target, Method method, Object[] args, ThrowingSupplier supplier) {
            this.target = target;
            this.method = method;
            this.args = args;
            this.supplier = supplier;
        }

        @Override
        public Object proceed() throws Throwable {
            return supplier.get();
        }

        @Override
        public Object proceed(Object[] args) throws Throwable {
            return supplier.get();
        }

        @Override
        public void set$AroundClosure(AroundClosure arc) {
        }

        @Override
        public String toShortString() {
            return method.getName();
        }

        @Override
        public String toLongString() {
            return method.toGenericString();
        }

        @Override
        public Object getThis() {
            return target;
        }

        @Override
        public Object getTarget() {
            return target;
        }

        @Override
        public Object[] getArgs() {
            return args;
        }

        @Override
        public Signature getSignature() {
            return new TestMethodSignature(method);
        }

        @Override
        public SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public String getKind() {
            return "method-execution";
        }

        @Override
        public StaticPart getStaticPart() {
            return null;
        }
    }

    record TestMethodSignature(Method method) implements MethodSignature {

        @Override
        public Class<?> getReturnType() {
            return method.getReturnType();
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public String[] getParameterNames() {
            return new String[]{"userId"};
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        @Override
        public String toShortString() {
            return method.getName();
        }

        @Override
        public String toLongString() {
            return method.toGenericString();
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public Class<?> getDeclaringType() {
            return method.getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return method.getDeclaringClass().getName();
        }
    }
}
