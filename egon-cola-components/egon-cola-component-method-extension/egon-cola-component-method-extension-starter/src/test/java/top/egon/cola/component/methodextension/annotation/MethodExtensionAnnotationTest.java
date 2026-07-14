package top.egon.cola.component.methodextension.annotation;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MethodExtensionAnnotationTest {

    @Test
    void shouldExposeRuntimeMethodAnnotationContract() throws NoSuchMethodException {
        Target target = MethodExtension.class.getAnnotation(Target.class);
        Retention retention = MethodExtension.class.getAnnotation(Retention.class);
        Method method = SampleService.class.getDeclaredMethod("query", String.class);
        MethodExtension annotation = method.getAnnotation(MethodExtension.class);

        assertThat(Arrays.asList(target.value())).containsExactly(ElementType.METHOD);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(annotation.handler()).isEqualTo(AllowHandler.class);
        assertThat(annotation.returnJson()).isEqualTo("{\"code\":1111}");
    }

    static class SampleService {

        @MethodExtension(handler = AllowHandler.class, returnJson = "{\"code\":1111}")
        String query(String userId) {
            return userId;
        }
    }

    static class AllowHandler implements MethodExtensionHandler {

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            return MethodExtensionDecision.allow();
        }
    }
}
