package top.egon.cola.component.gateway.openapi.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiAnnotationContractTest {

    @Test
    void exposesTheExactThreeGovernanceAnnotations() {
        assertThat(EgonApiCatalog.class.getAnnotation(Target.class).value())
                .containsExactly(ElementType.TYPE);
        assertThat(EgonGatewayPolicy.class.getAnnotation(Target.class).value())
                .containsExactly(ElementType.METHOD);
        assertThat(EgonMcpTool.class.getAnnotation(Target.class).value())
                .containsExactly(ElementType.METHOD);
        assertThat(EgonApiCatalog.class.getAnnotation(Retention.class).value())
                .isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(EgonGatewayPolicy.class.getAnnotation(Retention.class).value())
                .isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(EgonMcpTool.class.getAnnotation(Retention.class).value())
                .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void keepsTheApprovedEnumDefaults() throws Exception {
        assertThat(EgonGatewayPolicy.class.getDeclaredMethod("exposure")
                .getDefaultValue())
                .isEqualTo(EgonGatewayPolicy.Exposure.INTERNAL);
        assertThat(EgonGatewayPolicy.class.getDeclaredMethod("idempotency")
                .getDefaultValue())
                .isEqualTo(EgonGatewayPolicy.Idempotency.AUTO);
        assertThat(EgonMcpTool.class.getDeclaredMethod("enabled")
                .getDefaultValue())
                .isEqualTo(false);
    }
}
