package top.egon.cola.component.accessguard.reject;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static top.egon.cola.component.accessguard.annotation.FailStrategy.FAIL_OPEN;
import static top.egon.cola.component.accessguard.annotation.TimeoutExecutorType.THREAD_POOL;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.GATEKEEPER;

class RejectResponseInvokerTest {

    private final ReflectionFallbackInvoker invoker = new ReflectionFallbackInvoker(new JsonRejectResponseParser());

    @Test
    void shouldInvokeSameArgumentFallback() throws NoSuchMethodException {
        SampleService target = new SampleService();
        Method method = SampleService.class.getDeclaredMethod("guarded", String.class);

        Object result = invoker.reject(target, method, rule("fallback", ""), context(), new Object[]{"u-001"});

        assertThat(result).isEqualTo("fallback:u-001");
    }

    @Test
    void shouldInvokeFallbackWithContext() throws NoSuchMethodException {
        SampleService target = new SampleService();
        Method method = SampleService.class.getDeclaredMethod("guarded", String.class);

        Object result = invoker.reject(target, method, rule("fallbackWithContext", ""), context(), new Object[]{"u-001"});

        assertThat(result).isEqualTo("draw-api:u-001");
    }

    @Test
    void shouldInvokeNoArgumentFallback() throws NoSuchMethodException {
        SampleService target = new SampleService();
        Method method = SampleService.class.getDeclaredMethod("guarded", String.class);

        Object result = invoker.reject(target, method, rule("noArgFallback", ""), context(), new Object[]{"u-001"});

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void shouldParseReturnJsonIntoMethodReturnType() throws NoSuchMethodException {
        SampleService target = new SampleService();
        Method method = SampleService.class.getDeclaredMethod("payload", String.class);

        Object result = invoker.reject(target, method, rule("", "{\"code\":\"limited\"}"), context(), new Object[]{"u-001"});

        assertThat(result).isEqualTo(new Payload("limited"));
    }

    @Test
    void shouldInvokeStaticFallbackWithoutTarget() throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod("staticGuarded", String.class);

        Object result = invoker.reject(
                null, method, rule("staticFallback", ""), context(), new Object[]{"u-001"});

        assertThat(result).isEqualTo("static-fallback:u-001");
    }

    @Test
    void shouldRejectInstanceFallbackForStaticTarget() throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod("staticGuarded", String.class);

        assertThatThrownBy(() -> invoker.reject(
                null, method, rule("fallback", ""), context(), new Object[]{"u-001"}))
                .isInstanceOf(top.egon.cola.component.accessguard.exception.AccessGuardRejectResponseException.class)
                .hasMessageContaining("requires a static fallback");
    }

    private AccessGuardContext context() {
        AccessGuardContext context = new AccessGuardContext();
        context.setRuleName("draw-api");
        return context;
    }

    private AccessGuardRule rule(String fallbackMethod, String returnJson) {
        return new AccessGuardRule(
                "draw-api",
                "userId",
                "",
                false,
                List.of(),
                GATEKEEPER,
                true,
                1L,
                1L,
                TimeUnit.SECONDS,
                false,
                0L,
                Duration.ofHours(24),
                false,
                false,
                Duration.ofMillis(350),
                THREAD_POOL,
                false,
                true,
                fallbackMethod,
                returnJson,
                FAIL_OPEN
        );
    }

    static class SampleService {

        String guarded(String userId) {
            return userId;
        }

        Payload payload(String userId) {
            return new Payload(userId);
        }

        String fallback(String userId) {
            return "fallback:" + userId;
        }

        String fallbackWithContext(String userId, AccessGuardContext context) {
            return context.ruleName() + ":" + userId;
        }

        String noArgFallback() {
            return "fallback";
        }

        static String staticGuarded(String userId) {
            return userId;
        }

        static String staticFallback(String userId) {
            return "static-fallback:" + userId;
        }
    }

    record Payload(String code) {
    }
}
