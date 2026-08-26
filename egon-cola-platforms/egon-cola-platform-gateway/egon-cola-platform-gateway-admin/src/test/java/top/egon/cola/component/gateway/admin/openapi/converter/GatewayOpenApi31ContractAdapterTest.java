package top.egon.cola.component.gateway.admin.openapi.converter;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.openapi.GatewayOpenApiValidationTestFixture;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDefinitionDTO;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayOpenApi31ContractAdapterTest {

    private final GatewayOpenApi31ContractAdapter adapter =
            new GatewayOpenApi31ContractAdapter();

    @Test
    void serversAndObjectOrderingDoNotChangeCanonicalSha() {
        GatewayOpenApiDocumentDTO first = document("""
                {
                  "openapi":"3.1.0",
                  "servers":[{"url":"https://10.0.0.1:9443"}],
                  "info":{"version":"1.0.0","title":"Orders"},
                  "paths":{"/orders":{"get":{"servers":[{"url":"https://10.0.0.1"}],
                    "responses":{"200":{"description":"ok"}},"operationId":"orders.list",
                    "tags":["z","a"]}}},
                  "x-egon-service":{"openapiGroup":"orders","buildId":"build-1",
                    "artifactVersion":"1.0.0","applicationCode":"orders","bizCode":"trade","version":1}
                }
                """);
        GatewayOpenApiDocumentDTO second = document("""
                {
                  "x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                    "artifactVersion":"1.0.0","buildId":"build-1","openapiGroup":"orders"},
                  "paths":{"/orders":{"get":{"operationId":"orders.list",
                    "tags":["a","z"],"responses":{"200":{"description":"ok"}},
                    "servers":[{"url":"https://10.0.0.2"}]}}},
                  "info":{"title":"Orders","version":"1.0.0"},
                  "servers":[{"url":"https://10.0.0.2:9443"}],
                  "openapi":"3.1.0"
                }
                """);

        assertThat(adapter.adapt(first).canonicalSha256())
                .isEqualTo(adapter.adapt(second).canonicalSha256());
    }

    @Test
    void mapsOperationKeyResponsePriorityErrorsAndHttpAttributes() {
        GatewayOpenApiDefinitionDTO.BusinessDomain definition = adapter.adapt(document("""
                {
                  "openapi":"3.1.0",
                  "info":{"title":"Orders","version":"1.0.0"},
                  "paths":{"/orders/{id}":{
                    "parameters":[{"name":"id","in":"path","required":true,
                      "schema":{"type":"string"}}],
                    "get":{"operationId":"orders.get","summary":"Get order",
                      "parameters":[{"name":"q","in":"query","required":false,
                        "schema":{"type":"integer"}}],
                      "responses":{
                        "201":{"description":"created","content":{"application/json":
                          {"schema":{"type":"object","properties":{"created":{"type":"boolean"}}}}}},
                        "200":{"description":"ok","content":{"application/json":
                          {"schema":{"type":"object","properties":{"id":{"type":"string"}}}}}},
                        "400":{"description":"bad","content":{"application/problem+json":
                          {"schema":{"type":"object","properties":{"error":{"type":"string"}}}}}}
                      }
                    }
                  }},
                  "x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                    "artifactVersion":"1.0.0","buildId":"build-1","openapiGroup":"orders"}
                }
                """)).businessDomains().getFirst();

        GatewayOpenApiDefinitionDTO.InterfaceGroup group = definition
                .entityDomains().getFirst().interfaceGroups().getFirst();
        GatewayOpenApiDefinitionDTO.Operation operation = group.operations()
                .getFirst();

        assertThat(operation.operationKey())
                .isEqualTo("orders:http:GET:/orders/{id}");
        assertThat(operation.requestSchema())
                .containsKey("properties");
        assertThat(operation.requestSchema().get("properties"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsKeys("path", "query");
        assertThat(operation.responseSchema().get("properties"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsKey("id");
        assertThat(operation.errorSchema()).hasSize(2);
        assertThat(operation.errorSchema().getFirst()).containsEntry(
                "status", "201"
        );
        assertThat(operation.attributes())
                .containsEntry("httpMethod", "GET")
                .containsEntry("path", "/orders/{id}")
                .containsEntry("openapiGroup", "orders")
                .containsEntry("responseMode", "TRANSPARENT");
    }

    @Test
    void optionalMcpProducesExposureOnlyWhenExtensionIsPresent() {
        GatewayOpenApiDefinitionDTO ordinary = adapter.adapt(
                GatewayOpenApiValidationTestFixture.document()
        );
        assertThat(ordinary.businessDomains().getFirst().entityDomains()
                .getFirst().interfaceGroups().getFirst().operations().getFirst()
                .attributes()).doesNotContainKey("mcpExposure");

        GatewayOpenApiDefinitionDTO mcp = adapter.adapt(document("""
                {"openapi":"3.1.0","info":{"title":"Orders","version":"1.0.0"},
                 "paths":{"/orders":{"get":{"operationId":"orders.list",
                   "x-egon":{"version":1,"mcp":{"serverCode":"orders",
                     "name":"list","permissions":["orders:read"],"riskLevel":"LOW"}},
                   "responses":{"200":{"description":"ok"}}}}},
                 "x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                   "artifactVersion":"1.0.0","buildId":"build-1","openapiGroup":"orders"}}
                """));
        assertThat(mcp.businessDomains().getFirst().entityDomains().getFirst()
                .interfaceGroups().getFirst().operations().getFirst()
                .attributes()).containsKey("mcpExposure");
    }

    @Test
    void conflictingPreferredJsonMediaSchemasFailClosed() {
        assertThatThrownBy(() -> adapter.adapt(document("""
                {"openapi":"3.1.0","info":{"title":"Orders","version":"1.0.0"},
                 "paths":{"/orders":{"post":{"operationId":"orders.create",
                   "requestBody":{"content":{
                     "application/vnd.first+json":{"schema":{"type":"object","properties":{"a":{"type":"string"}}}},
                     "application/vnd.second+json":{"schema":{"type":"object","properties":{"b":{"type":"string"}}}}
                   }},"responses":{"200":{"description":"ok"}}}}},
                 "x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                   "artifactVersion":"1.0.0","buildId":"build-1","openapiGroup":"orders"}}
                """))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("media");
    }

    @Test
    void schemaPropertyServersAndOneOfOrderRemainContractMaterial() {
        String first = """
                {"openapi":"3.1.0","info":{"title":"Orders","version":"1.0.0"},
                 "paths":{"/orders":{"get":{"operationId":"orders.list",
                   "responses":{"200":{"description":"ok"}}}}},
                 "components":{"schemas":{"Order":{"type":"object",
                   "properties":{"servers":{"type":"array","items":{"type":"string"}}},
                   "oneOf":[{"required":["a"]},{"required":["b"]}]}}},
                 "x-egon-service":{"version":1,"bizCode":"trade","applicationCode":"orders",
                   "artifactVersion":"1.0.0","buildId":"build-1","openapiGroup":"orders"}}
                """;
        String second = first.replace(
                "\"oneOf\":[{\"required\":[\"a\"]},{\"required\":[\"b\"]}]",
                "\"oneOf\":[{\"required\":[\"b\"]},{\"required\":[\"a\"]}]"
        );
        assertThat(adapter.canonicalSha256(document(first)))
                .isNotEqualTo(adapter.canonicalSha256(document(second)));
    }

    private GatewayOpenApiDocumentDTO document(String json) {
        return GatewayOpenApiValidationTestFixture.document(
                GatewayOpenApiValidationTestFixture.candidate(),
                json.getBytes(StandardCharsets.UTF_8),
                "application/json",
                200
        );
    }
}
