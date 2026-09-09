package top.egon.cola.component.yuheng.admin.openapi.validation.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.admin.openapi.GatewayOpenApiValidationTestFixture;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationLimits;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiLimitValidatorTest {

    @Test
    void rejectsDocumentsAboveTheConfiguredByteLimit() {
        GatewayOpenApiLimitValidator validator =
                new GatewayOpenApiLimitValidator(
                        new GatewayOpenApiValidationLimits(5, 10, 10, 4)
                );

        assertThat(validator.validate(GatewayOpenApiValidationTestFixture.document(
                GatewayOpenApiValidationTestFixture.candidate(),
                "{\"a\":1}".getBytes(StandardCharsets.UTF_8),
                "application/json",
                200
        )).code()).isEqualTo("GATEWAY_OPENAPI_DOCUMENT_TOO_LARGE");
    }

    @Test
    void rejectsTooManyOperationsAndSchemaNodes() {
        GatewayOpenApiLimitValidator validator =
                new GatewayOpenApiLimitValidator(
                        new GatewayOpenApiValidationLimits(1024, 1, 1, 4)
                );
        String json = """
                {"openapi":"3.1.0","info":{"title":"t","version":"v"},
                 "paths":{"/a":{"get":{"operationId":"a.get","responses":{"200":{"description":"ok"}}}},
                          "/b":{"get":{"operationId":"b.get","responses":{"200":{"description":"ok"}}}}},
                 "components":{"schemas":{"A":{"type":"object"},"B":{"type":"object"}}},
                 "x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                   "artifactVersion":"1.0.0","buildId":"build-1","openapiGroup":"orders"}}
                """;

        assertThat(validator.validate(
                GatewayOpenApiValidationTestFixture.document(json)
        ).code()).isEqualTo("GATEWAY_OPENAPI_OPERATION_LIMIT");
    }
}
