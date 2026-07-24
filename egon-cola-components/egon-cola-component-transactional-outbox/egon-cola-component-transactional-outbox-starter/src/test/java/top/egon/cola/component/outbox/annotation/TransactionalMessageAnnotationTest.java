package top.egon.cola.component.outbox.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionalMessageAnnotationTest {

    @Test
    void shouldExposeOneRuntimeMethodExpression() {
        Target target = TransactionalMessage.class.getAnnotation(Target.class);
        Retention retention = TransactionalMessage.class.getAnnotation(Retention.class);

        assertThat(target.value()).containsExactly(ElementType.METHOD);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(TransactionalMessage.class.isAnnotationPresent(Documented.class)).isTrue();
        assertThat(TransactionalMessage.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactly("message");
    }
}
