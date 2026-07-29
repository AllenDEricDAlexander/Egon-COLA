package top.egon.cola.component.accessguard.execution;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.GuardInvocationKind;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.core.plan.FailurePolicies;
import top.egon.cola.component.accessguard.core.plan.GuardPlan;
import top.egon.cola.component.accessguard.core.plan.GuardPlanValidator;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.core.plan.ObservabilityConfig;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodHandleFallbackHandlerTest {

    @Test
    void validatesAndCachesArgumentsPlusOutcomeBeforeExecution() throws Throwable {
        Method original = Sample.class.getDeclaredMethod("draw", String.class);
        FallbackMethodCache cache = new FallbackMethodCache();
        new GuardPlanValidator().validateExecution(
                original,
                plan(new ExecutionConfig.RejectionConfig(RejectionMode.FALLBACK, "drawFallback", "")),
                cache,
                new JsonRejectValueParser(new ObjectMapper()));
        MethodHandle handle = cache.lookup(original, "drawFallback").orElseThrow();
        MethodHandleFallbackHandler handler = new MethodHandleFallbackHandler(cache);
        GuardOutcome outcome = GuardOutcome.rejected("draw", top.egon.cola.component.accessguard.core.GuardDecision.RATE_LIMITED,
                "rate-limit", 1L);

        Object value = handler.execute(invocation(new Sample(), original, "user-1"), outcome, "drawFallback");

        assertThat(value).isEqualTo("fallback:user-1:RATE_LIMITED");
        assertThat(cache.lookup(original, "drawFallback")).containsSame(handle);
    }

    @Test
    void supportsNoArgumentAndStaticFallbacks() throws Throwable {
        FallbackMethodCache cache = new FallbackMethodCache();
        Method noArgs = Sample.class.getDeclaredMethod("noArgs", String.class);
        Method staticMethod = Sample.class.getDeclaredMethod("staticDraw", String.class);
        cache.validateAndCache(noArgs, "noArgsFallback");
        cache.validateAndCache(staticMethod, "staticFallback");
        MethodHandleFallbackHandler handler = new MethodHandleFallbackHandler(cache);
        GuardOutcome outcome = GuardOutcome.rejected("draw", top.egon.cola.component.accessguard.core.GuardDecision.RATE_LIMITED,
                "rate-limit", 1L);

        assertThat(handler.execute(invocation(new Sample(), noArgs, "ignored"), outcome, "noArgsFallback"))
                .isEqualTo("no-args");
        assertThat(handler.execute(invocation(new Sample(), staticMethod, "user-1"), outcome, "staticFallback"))
                .isEqualTo("static:user-1");
    }

    @Test
    void rejectsIncompatibleReturnTypeDuringValidation() throws Exception {
        Method original = Sample.class.getDeclaredMethod("draw", String.class);

        assertThatThrownBy(() -> new FallbackMethodCache().validateAndCache(original, "incompatible"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("return type");
    }

    @Test
    void doesNotHideFallbackExceptions() throws Exception {
        Method original = Sample.class.getDeclaredMethod("draw", String.class);
        FallbackMethodCache cache = new FallbackMethodCache();
        cache.validateAndCache(original, "failedFallback");
        MethodHandleFallbackHandler handler = new MethodHandleFallbackHandler(cache);

        assertThatThrownBy(() -> handler.execute(
                invocation(new Sample(), original, "user-1"),
                GuardOutcome.rejected("draw", top.egon.cola.component.accessguard.core.GuardDecision.RATE_LIMITED,
                        "rate-limit", 1L),
                "failedFallback"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fallback failed");
    }

    private static GuardInvocation invocation(Object target, Method method, Object argument) {
        return new GuardInvocation(
                "draw", target, Sample.class, method, new Object[]{argument}, Map.of(),
                GuardEntryType.AOP, GuardInvocationKind.METHOD, () -> "business");
    }

    private static GuardPlan plan(ExecutionConfig.RejectionConfig rejection) {
        return new GuardPlan(
                "draw",
                true,
                new KeyConfig(List.of("GLOBAL"), List.of(), "secret"),
                new AdmissionConfig(
                        new AdmissionConfig.DenyListConfig(false),
                        new AdmissionConfig.AllowListConfig(false, AllowListMode.GATE),
                        new AdmissionConfig.PenaltyBoxConfig(
                                false, 3, Duration.ofMinutes(1), Duration.ofMinutes(10)),
                        new AdmissionConfig.RateLimitConfig(
                                false,
                                AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                                10,
                                10,
                                Duration.ofSeconds(1),
                                1)),
                new ExecutionConfig(
                        new ExecutionConfig.TimeLimitConfig(
                                false,
                                TimeLimitMode.DISABLED,
                                TimeLimiterType.CALLER_THREAD,
                                Duration.ofSeconds(1),
                                true),
                        rejection),
                FailurePolicies.defaults(),
                ObservabilityConfig.defaults(),
                "state-v1");
    }

    static class Sample {

        String draw(String user) {
            return user;
        }

        String drawFallback(String user, GuardOutcome outcome) {
            return "fallback:" + user + ":" + outcome.decision();
        }

        String noArgs(String user) {
            return user;
        }

        String noArgsFallback() {
            return "no-args";
        }

        static String staticDraw(String user) {
            return user;
        }

        static String staticFallback(String user) {
            return "static:" + user;
        }

        Integer incompatible(String user) {
            return 1;
        }

        String failedFallback(String user) {
            throw new IllegalStateException("fallback failed");
        }
    }
}
