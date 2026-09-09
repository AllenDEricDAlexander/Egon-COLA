package top.egon.cola.component.yuheng.admin.openapi.validation.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.admin.openapi.GatewayOpenApiValidationTestFixture;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiEgonExtensionValidatorTest {

    @Test
    void acceptsMatchingServiceExtension() {
        assertThat(new GatewayOpenApiEgonExtensionValidator().validate(
                GatewayOpenApiValidationTestFixture.document()
        ).valid()).isTrue();
    }

    @Test
    void rejectsBuildOrGroupMismatch() {
        assertThat(new GatewayOpenApiEgonExtensionValidator().validate(
                GatewayOpenApiValidationTestFixture.document(
                        """
                        {"openapi":"3.1.0","info":{"title":"t","version":"v"},
                         "paths":{},"x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                           "artifactVersion":"1.0.0","buildId":"other-build","openapiGroup":"orders"}}
                        """
                )
        ).code()).isEqualTo("YUHENG_OPENAPI_BUILD_MISMATCH");
    }
}
