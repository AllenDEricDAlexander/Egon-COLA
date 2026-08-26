package top.egon.cola.component.gateway.admin.openapi.validation.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.openapi.GatewayOpenApiValidationTestFixture;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiDocumentEnvelopeValidatorTest {

    @Test
    void acceptsJsonSuccessEnvelope() {
        assertThat(new GatewayOpenApiDocumentEnvelopeValidator().validate(
                GatewayOpenApiValidationTestFixture.document()
        ).valid()).isTrue();
    }

    @Test
    void rejectsRedirectAndNonJsonEnvelope() {
        assertThat(new GatewayOpenApiDocumentEnvelopeValidator().validate(
                GatewayOpenApiValidationTestFixture.document(
                        GatewayOpenApiValidationTestFixture.candidate(),
                        "{}".getBytes(StandardCharsets.UTF_8),
                        "text/html",
                        200
                )
        )).satisfies(result -> {
            assertThat(result.valid()).isFalse();
            assertThat(result.code()).isEqualTo(
                    "GATEWAY_OPENAPI_CONTENT_TYPE"
            );
        });
        assertThat(new GatewayOpenApiDocumentEnvelopeValidator().validate(
                GatewayOpenApiValidationTestFixture.document(
                        GatewayOpenApiValidationTestFixture.candidate(),
                        "{}".getBytes(StandardCharsets.UTF_8),
                        "application/json",
                        302
                )
        ).code()).isEqualTo("GATEWAY_OPENAPI_HTTP_STATUS");
    }
}
