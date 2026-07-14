package top.egon.cola.component.methodextension.context;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class MethodExtensionContextTest {

    @Test
    void shouldExposeTargetMethodAndDefensiveArgumentCopies() throws NoSuchMethodException {
        SampleService target = new SampleService();
        Method method = SampleService.class.getDeclaredMethod("query", String.class);
        Object[] source = {"u-001"};

        MethodExtensionContext context = new MethodExtensionContext(target, method, source);
        source[0] = "changed-at-source";
        Object[] returned = context.arguments();
        returned[0] = "changed-at-caller";

        assertThat(context.target()).isSameAs(target);
        assertThat(context.method()).isEqualTo(method);
        assertThat(context.arguments()).containsExactly("u-001");
    }

    static class SampleService {

        String query(String userId) {
            return userId;
        }
    }
}
