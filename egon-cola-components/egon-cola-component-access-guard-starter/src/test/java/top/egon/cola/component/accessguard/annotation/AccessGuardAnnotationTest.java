package top.egon.cola.component.accessguard.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardAnnotationTest {

    @Test
    void rateLimiterAnnotationShouldExposeCompatibilityDefaults() throws NoSuchMethodException {
        RateLimiterAccessInterceptor annotation = Sample.class.getDeclaredMethod("rateLimited", String.class)
                .getAnnotation(RateLimiterAccessInterceptor.class);

        assertThat(annotation.name()).isEqualTo("draw-api");
        assertThat(annotation.key()).isEqualTo("userId");
        assertThat(annotation.permitsPerSecond()).isEqualTo(1.0d);
        assertThat(annotation.permits()).isEqualTo(1L);
        assertThat(annotation.interval()).isEqualTo(1L);
        assertThat(annotation.intervalUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(annotation.blacklistCount()).isEqualTo(3L);
        assertThat(annotation.fallbackMethod()).isEqualTo("fallback");
    }

    @Test
    void timeoutAnnotationShouldBeMethodLevelOnly() {
        Target target = TimeoutCircuitBreaker.class.getAnnotation(Target.class);

        assertThat(target.value()).containsExactly(ElementType.METHOD);
    }

    @Test
    void aggregateAnnotationShouldSupportMethodsAndConstructorsOnly() throws NoSuchMethodException {
        Target target = AccessGuard.class.getAnnotation(Target.class);
        AccessGuard annotation = ConstructorSample.class.getDeclaredConstructor(String.class)
                .getAnnotation(AccessGuard.class);

        assertThat(target.value()).containsExactlyInAnyOrder(
                ElementType.METHOD, ElementType.CONSTRUCTOR);
        assertThat(annotation.failStrategy()).isEqualTo(FailStrategy.GLOBAL_DEFAULT);
    }

    @Test
    void dedicatedAndCompatibilityAnnotationsShouldRemainMethodOnly() {
        assertThat(WhiteListAccessInterceptor.class.getAnnotation(Target.class).value())
                .containsExactly(ElementType.METHOD);
        assertThat(RateLimiterAccessInterceptor.class.getAnnotation(Target.class).value())
                .containsExactly(ElementType.METHOD);
        assertThat(TimeoutCircuitBreaker.class.getAnnotation(Target.class).value())
                .containsExactly(ElementType.METHOD);
        assertThat(DoWhiteList.class.getAnnotation(Target.class).value())
                .containsExactly(ElementType.METHOD);
        assertThat(DoRateLimiter.class.getAnnotation(Target.class).value())
                .containsExactly(ElementType.METHOD);
        assertThat(DoHystrix.class.getAnnotation(Target.class).value())
                .containsExactly(ElementType.METHOD);
    }

    static class Sample {

        @RateLimiterAccessInterceptor(
                name = "draw-api",
                key = "userId",
                permitsPerSecond = 1.0d,
                blacklistCount = 3,
                fallbackMethod = "fallback"
        )
        String rateLimited(String userId) {
            return userId;
        }
    }

    static class ConstructorSample {

        @AccessGuard
        ConstructorSample(String value) {
        }
    }
}
