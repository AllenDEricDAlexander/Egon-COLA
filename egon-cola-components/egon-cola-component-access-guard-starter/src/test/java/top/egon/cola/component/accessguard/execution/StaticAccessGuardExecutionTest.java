package top.egon.cola.component.accessguard.execution;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.agent.AgentProceedingJoinPoint;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.blacklist.BlacklistStatus;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.key.AccessKeyResolution;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterDecision;
import top.egon.cola.component.accessguard.reject.ReflectionFallbackInvoker;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static top.egon.cola.component.accessguard.annotation.FailStrategy.FAIL_OPEN;
import static top.egon.cola.component.accessguard.annotation.TimeoutExecutorType.THREAD_POOL;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.GATEKEEPER;

class StaticAccessGuardExecutionTest {

    @Test
    void rejectsThroughStaticFallbackWithoutInvokingBusiness() throws Throwable {
        Target.calls = 0;
        AccessGuardExecutionService service = service(RateLimiterDecision.reject("limited"));

        Object result = service.execute(joinPoint("value"));

        assertThat(result).isEqualTo("fallback:value");
        assertThat(Target.calls).isZero();
    }

    @Test
    void preservesStaticBusinessThrowableIdentity() throws Throwable {
        AccessGuardExecutionService service = service(RateLimiterDecision.allow(0L));

        assertThatThrownBy(() -> service.execute(joinPoint("fail"))).isSameAs(Target.FAILURE);
    }

    private AccessGuardExecutionService service(RateLimiterDecision rateDecision) {
        AccessGuardProperties properties = new AccessGuardProperties();
        AccessGuardRule rule = rule();
        AccessGuardRuleResolver ruleResolver = new AccessGuardRuleResolver() {
            @Override
            public AccessGuardRule resolve(Method method) {
                return rule;
            }
        };
        BlacklistService blacklistService = new BlacklistService() {
            @Override
            public BlacklistStatus status(AccessGuardRule rule, AccessGuardContext context) {
                return BlacklistStatus.none();
            }

            @Override
            public BlacklistStatus incrementRejectAndMaybeBlacklist(
                    AccessGuardRule rule,
                    AccessGuardContext context
            ) {
                return BlacklistStatus.none();
            }

            @Override
            public void remove(String ruleName, String accessKeyHash) {
            }
        };
        return new AccessGuardExecutionService(
                properties,
                ruleResolver,
                (joinPoint, ignored) -> new AccessKeyResolution("all", "all", "hash"),
                (ignoredRule, ignoredHash) ->
                        top.egon.cola.component.accessguard.whitelist.WhiteListDecision.pass(GATEKEEPER),
                blacklistService,
                (ignoredRule, ignoredContext) -> rateDecision,
                (joinPoint, ignoredRule, ignoredContext) -> joinPoint.proceed(),
                new ReflectionFallbackInvoker(),
                ignored -> {
                },
                new AccessGuardFailureHandler(properties)
        );
    }

    private AgentProceedingJoinPoint joinPoint(String value) throws Exception {
        Method method = Target.class.getDeclaredMethod("guarded", String.class);
        var handle = MethodHandles.lookup().findStatic(
                Target.class, "body", MethodType.methodType(String.class, String.class));
        return new AgentProceedingJoinPoint(null, method, handle, new Object[]{value});
    }

    private AccessGuardRule rule() {
        return new AccessGuardRule(
                "static-rule", "all", "", false, List.of(), GATEKEEPER,
                true, 1L, 1L, TimeUnit.SECONDS, false, 0L, Duration.ofHours(1), false,
                false, Duration.ofMillis(100), THREAD_POOL, false, true,
                "fallback", "", FAIL_OPEN
        );
    }

    static class Target {

        static final RuntimeException FAILURE = new RuntimeException("static-sentinel");
        static int calls;

        static String guarded(String value) {
            return value;
        }

        static String body(String value) {
            calls++;
            if (value.equals("fail")) {
                throw FAILURE;
            }
            return "body:" + value;
        }

        static String fallback(String value) {
            return "fallback:" + value;
        }
    }
}
