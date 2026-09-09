package top.egon.cola.component.yuheng.admin.openapi.validation.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.admin.openapi.GatewayOpenApiValidationTestFixture;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiMcpProjectionValidatorTest {

    @Test
    void missingMcpExtensionIsAValidNonMcpOperation() {
        assertThat(new GatewayOpenApiMcpProjectionValidator().validate(
                GatewayOpenApiValidationTestFixture.document()
        ).valid()).isTrue();
    }

    @Test
    void providerMcpShapeWithoutEnabledFlagIsValidatedAsEnabled() {
        assertThat(new GatewayOpenApiMcpProjectionValidator().validate(
                GatewayOpenApiValidationTestFixture.document(
                        """
                        {"openapi":"3.1.0","info":{"title":"t","version":"v"},
                         "paths":{"/orders":{"get":{"operationId":"orders.list",
                           "x-egon":{"version":1,"mcp":{"serverCode":"orders","name":"list",
                             "permissions":["orders:read"],"riskLevel":"LOW"}},
                           "responses":{"200":{"description":"ok"}}}}},
                         "x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                           "artifactVersion":"1.0.0","buildId":"build-1","openapiGroup":"orders"}}
                        """
                )
        ).valid()).isTrue();
    }

    @Test
    void enabledMcpRequiresServerCodeAndSortedPermissions() {
        assertThat(new GatewayOpenApiMcpProjectionValidator().validate(
                GatewayOpenApiValidationTestFixture.document(
                        """
                        {"openapi":"3.1.0","info":{"title":"t","version":"v"},
                         "paths":{"/orders":{"get":{"operationId":"orders.list",
                           "x-egon":{"version":1,"mcp":{"enabled":true}},
                           "responses":{"200":{"description":"ok"}}}}},
                         "x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                           "artifactVersion":"1.0.0","buildId":"build-1","openapiGroup":"orders"}}
                        """
                )
        ).code()).isEqualTo("GATEWAY_OPENAPI_MCP_SERVER");
    }
}
