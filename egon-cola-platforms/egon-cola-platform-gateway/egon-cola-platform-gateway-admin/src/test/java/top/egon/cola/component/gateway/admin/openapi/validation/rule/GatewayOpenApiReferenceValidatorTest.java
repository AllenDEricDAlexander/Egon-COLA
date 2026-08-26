package top.egon.cola.component.gateway.admin.openapi.validation.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.openapi.GatewayOpenApiValidationTestFixture;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiReferenceValidatorTest {

    @Test
    void acceptsLocalReferences() {
        assertThat(new GatewayOpenApiReferenceValidator().validate(
                GatewayOpenApiValidationTestFixture.document(
                        """
                        {"openapi":"3.1.0","info":{"title":"t","version":"v"},
                         "paths":{"/orders":{"get":{"operationId":"orders.list",
                           "responses":{"200":{"description":"ok","content":
                             {"application/json":{"schema":{"$ref":"#/components/schemas/Order"}}}}}}}},
                         "components":{"schemas":{"Order":{"type":"object"}}},
                         "x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                           "artifactVersion":"1.0.0","buildId":"build-1","openapiGroup":"orders"}}
                        """
                )
        ).valid()).isTrue();
    }

    @Test
    void rejectsExternalAndRemoteReferences() {
        assertThat(new GatewayOpenApiReferenceValidator().validate(
                GatewayOpenApiValidationTestFixture.document(
                        """
                        {"openapi":"3.1.0","info":{"title":"t","version":"v"},
                         "paths":{},"components":{"schemas":{"Order":{"$ref":"https://evil.test/order.json"}}},
                         "x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                           "artifactVersion":"1.0.0","buildId":"build-1","openapiGroup":"orders"}}
                        """
                )
        ).code()).isEqualTo("GATEWAY_OPENAPI_EXTERNAL_REF");
    }
}
