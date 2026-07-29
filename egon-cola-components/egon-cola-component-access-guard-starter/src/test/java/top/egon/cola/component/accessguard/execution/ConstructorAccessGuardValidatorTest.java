package top.egon.cola.component.accessguard.execution;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.annotation.AccessGuard;
import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.config.AccessGuardAnnotationResolver;
import top.egon.cola.component.accessguard.config.AccessGuardRule;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConstructorAccessGuardValidatorTest {

    private final AccessGuardAnnotationResolver resolver = new AccessGuardAnnotationResolver();
    private final ConstructorAccessGuardValidator validator = new ConstructorAccessGuardValidator();

    @Test
    void acceptsPublicAndPrivateAggregateConstructors() throws Exception {
        assertValid(Fixture.class.getDeclaredConstructor(String.class));
        assertValid(Fixture.class.getDeclaredConstructor(int.class));
    }

    @Test
    void rejectsUnsupportedVisibilityAndConstructorFeatures() throws Exception {
        assertInvalid(Fixture.class.getDeclaredConstructor(long.class), "public and private");
        assertInvalid(Fixture.class.getDeclaredConstructor(double.class), "public and private");
        assertInvalid(Fixture.class.getDeclaredConstructor(boolean.class), "timeout");
        assertInvalid(Fixture.class.getDeclaredConstructor(byte.class), "fallbackMethod");
        assertInvalid(Fixture.class.getDeclaredConstructor(short.class), "returnJson");
        assertInvalid(Fixture.class.getDeclaredConstructor(char.class), "LOCAL_FALLBACK");
        assertInvalid(Fixture.class.getDeclaredConstructor(float.class), "instance state");
    }

    private void assertValid(Constructor<?> constructor) {
        AccessGuardRule rule = resolver.resolve(constructor);
        assertThatCode(() -> validator.validate(constructor, rule)).doesNotThrowAnyException();
    }

    private void assertInvalid(Constructor<?> constructor, String message) {
        AccessGuardRule rule = resolver.resolve(constructor);
        assertThatThrownBy(() -> validator.validate(constructor, rule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(message);
    }

    static class Fixture {

        @AccessGuard(key = "value")
        public Fixture(String value) {
        }

        @AccessGuard
        private Fixture(int value) {
        }

        @AccessGuard
        protected Fixture(long value) {
        }

        @AccessGuard
        Fixture(double value) {
        }

        @AccessGuard(timeoutBreaker = true)
        public Fixture(boolean value) {
        }

        @AccessGuard(fallbackMethod = "fallback")
        public Fixture(byte value) {
        }

        @AccessGuard(returnJson = "{}")
        public Fixture(short value) {
        }

        @AccessGuard(failStrategy = FailStrategy.LOCAL_FALLBACK)
        public Fixture(char value) {
        }

        @AccessGuard(keyExpression = "this.value")
        public Fixture(float value) {
        }
    }
}
