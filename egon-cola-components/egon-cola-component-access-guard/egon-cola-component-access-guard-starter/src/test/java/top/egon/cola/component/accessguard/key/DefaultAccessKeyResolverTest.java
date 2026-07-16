package top.egon.cola.component.accessguard.key;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.config.AccessGuardRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static top.egon.cola.component.accessguard.annotation.FailStrategy.FAIL_OPEN;
import static top.egon.cola.component.accessguard.annotation.TimeoutExecutorType.THREAD_POOL;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.GATEKEEPER;

class DefaultAccessKeyResolverTest {

    private final DefaultAccessKeyResolver resolver = new DefaultAccessKeyResolver(
            AccessGuardProperties.KeyResolveFailureStrategy.USE_ALL
    );

    @Test
    void shouldResolveSimpleParameterName() throws NoSuchMethodException {
        Method method = Sample.class.getDeclaredMethod("simple", String.class);

        AccessKeyResolution resolution = resolver.resolve(method, new Object[]{"u-001"}, rule("userId"));

        assertThat(resolution.rawKey()).isEqualTo("u-001");
        assertThat(resolution.normalizedKey()).isEqualTo("u-001");
        assertThat(resolution.keyHash()).hasSize(64);
    }

    @Test
    void shouldResolveScalarConstructorParameterWithoutReflectingIntoJdkType()
            throws NoSuchMethodException {
        Constructor<Sample> constructor = Sample.class.getDeclaredConstructor(int.class);

        AccessKeyResolution resolution = resolver.resolve(
                constructor, new Object[]{7}, rule("value"));

        assertThat(resolution.rawKey()).isEqualTo("7");
    }

    @Test
    void shouldResolveNestedField() throws NoSuchMethodException {
        Method method = Sample.class.getDeclaredMethod("nested", Request.class);

        AccessKeyResolution resolution = resolver.resolve(method, new Object[]{new Request(new User("u-001"))}, rule("user.id"));

        assertThat(resolution.rawKey()).isEqualTo("u-001");
        assertThat(resolution.normalizedKey()).isEqualTo("u-001");
    }

    @Test
    void shouldResolveAll() throws NoSuchMethodException {
        Method method = Sample.class.getDeclaredMethod("simple", String.class);

        AccessKeyResolution resolution = resolver.resolve(method, new Object[]{"u-001"}, rule("all"));

        assertThat(resolution.rawKey()).isEqualTo("all");
        assertThat(resolution.normalizedKey()).isEqualTo("all");
    }

    @Test
    void shouldRejectBlankKeyWhenConfigured() throws NoSuchMethodException {
        Method method = Sample.class.getDeclaredMethod("nested", Request.class);
        DefaultAccessKeyResolver rejectResolver = new DefaultAccessKeyResolver(
                AccessGuardProperties.KeyResolveFailureStrategy.REJECT
        );

        assertThatThrownBy(() -> rejectResolver.resolve(method, new Object[]{new Request(new User(""))}, rule("user.id")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access key resolved blank");
    }

    private AccessGuardRule rule(String key) {
        return new AccessGuardRule(
                "draw-api",
                key,
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
                "",
                "",
                FAIL_OPEN
        );
    }

    static class Sample {

        Sample(int value) {
        }

        Sample() {
        }

        void simple(String userId) {
        }

        void nested(Request request) {
        }
    }

    record Request(User user) {
    }

    record User(String id) {
    }
}
