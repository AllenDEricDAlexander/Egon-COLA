package top.egon.cola.component.gateway.starter.discovery;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOperationSemanticsTest {

    @Test
    void derivesExplicitIdempotencyWithoutConfusingNegativeTag()
            throws Exception {
        assertThat(GatewayOperationSemantics.idempotent(annotation("query")))
                .isTrue();
        assertThat(GatewayOperationSemantics.idempotent(annotation("command")))
                .isFalse();
        assertThat(GatewayOperationSemantics.idempotent(null)).isFalse();
    }

    @Test
    void preservesDeclaredTagsAndUsesAnImmutableEmptyDefault()
            throws Exception {
        GatewayOperation operation = annotation("query");

        assertThat(GatewayOperationSemantics.tags(operation))
                .containsExactly("rpc", "idempotent");
        assertThat(GatewayOperationSemantics.tags(null)).isEmpty();
    }

    private GatewayOperation annotation(String methodName) throws Exception {
        Method method = Samples.class.getDeclaredMethod(methodName);
        return method.getAnnotation(GatewayOperation.class);
    }

    private static final class Samples {

        @GatewayOperation(tags = {"rpc", "idempotent"})
        private void query() {
        }

        @GatewayOperation(tags = {"rpc", "non-idempotent"})
        private void command() {
        }
    }
}
