package top.egon.cola.component.yuheng.admin.openapi.validation.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.admin.openapi.GatewayOpenApiValidationTestFixture;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApi31ValidatorTest {

    @Test
    void acceptsACompleteOpenApi31Envelope() {
        assertThat(new GatewayOpenApi31Validator().validate(
                GatewayOpenApiValidationTestFixture.document()
        ).valid()).isTrue();
    }

    @Test
    void rejectsUnsupportedVersionAndMissingInfo() {
        assertThat(new GatewayOpenApi31Validator().validate(
                GatewayOpenApiValidationTestFixture.document(
                        "{\"openapi\":\"3.0.3\",\"info\":{},\"paths\":{}}"
                )
        ).code()).isEqualTo("GATEWAY_OPENAPI_SPEC_VERSION");
        assertThat(new GatewayOpenApi31Validator().validate(
                GatewayOpenApiValidationTestFixture.document(
                        "{\"openapi\":\"3.1.0\",\"paths\":{}}"
                )
        ).code()).isEqualTo("GATEWAY_OPENAPI_INFO");
    }
}
