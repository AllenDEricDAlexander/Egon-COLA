package top.egon.cola.component.yuheng.admin.openapi.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiInvocationSchemaAdapterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final GatewayOpenApiInvocationSchemaAdapter adapter =
            new GatewayOpenApiInvocationSchemaAdapter(mapper);

    @Test
    void includesOnlyReachableDefinitionsAndPreservesCyclesAndErrorReferences() throws Exception {
        JsonNode root = mapper.readTree("""
                {"components":{"schemas":{
                  "Node":{"type":"object","properties":{"child":{"$ref":"#/components/schemas/A~1B"}}},
                  "A/B":{"allOf":[{"$ref":"#/components/schemas/Node"}],"description":"Recursive child"},
                  "Error":{"type":"object","properties":{"code":{"type":"string"}}},
                  "Unused":{"type":"object","properties":{"data":{"type":"string"}}}
                }},"paths":{"/nodes":{"get":{
                  "responses":{
                    "200":{"description":"ok","content":{"application/json":{"schema":{"$ref":"#/components/schemas/Node"}}}},
                    "400":{"description":"invalid","content":{"application/json":{"schema":{"$ref":"#/components/schemas/Error"}}}}
                  }
                }}}}
                """);
        JsonNode path = root.path("paths").path("/nodes");
        JsonNode operation = path.path("get");

        assertThat(adapter.requestSchema(root, path, operation)).doesNotContainKey("$defs");
        Map<String, Object> response = adapter.responseSchema(root, operation);
        assertThat(cast(response.get("$defs"))).containsOnlyKeys("Node", "A/B");
        assertThat(mapper.valueToTree(response).at("/$defs/A~1B/allOf/0/$ref").asText())
                .isEqualTo("#/$defs/Node");
        Map<String, Object> error = cast(adapter.errorSchemas(root, operation).getFirst().get("schema"));
        assertThat(error).containsEntry("$ref", "#/$defs/Error");
        assertThat(cast(error.get("$defs"))).containsOnlyKeys("Error");
        assertThat(root.path("components").path("schemas")).hasSize(4);
    }

    @Test
    void preservesParameterAndRequestBodyDescriptionsIncludingReferenceOverrides() throws Exception {
        JsonNode root = mapper.readTree("""
                {"components":{"parameters":{"RoleId":{"name":"roleId","in":"path",
                    "description":"Shared role identifier","schema":{"type":"string","description":"Identifier type"}}},
                  "requestBodies":{"Grant":{"description":"Shared body","required":true,
                    "content":{"application/json":{"schema":{"type":"object","properties":{
                      "reason":{"type":"string","description":"变更原因"}}}}}}}},
                 "paths":{"/roles/{roleId}":{"post":{
                   "parameters":[{"$ref":"#/components/parameters/RoleId","description":"当前租户的角色 ID"}],
                   "requestBody":{"$ref":"#/components/requestBodies/Grant","description":"完整替换直接授权资源"},
                   "responses":{"204":{"description":"已更新"}}}}}}
                """);
        JsonNode original = root.deepCopy();
        JsonNode path = root.path("paths").path("/roles/{roleId}");
        Map<String, Object> request = adapter.requestSchema(root, path, path.path("post"));
        Map<String, Object> properties = cast(request.get("properties"));
        Map<String, Object> pathProperties = cast(cast(properties.get("path")).get("properties"));
        assertThat(cast(properties.get("path"))).containsEntry("description", "路径参数");
        assertThat(cast(pathProperties.get("roleId")))
                .containsEntry("description", "当前租户的角色 ID");
        assertThat(cast(properties.get("body")))
                .containsEntry("description", "完整替换直接授权资源");
        assertThat(cast(cast(cast(properties.get("body")).get("properties")).get("reason")))
                .containsEntry("description", "变更原因");
        assertThat(root).isEqualTo(original);
    }

    @Test
    void rewritesReferencesInsideRefSiblingsAndKeepsPointerEscaping() throws Exception {
        JsonNode root = mapper.readTree("""
                {"components":{"schemas":{"A/B":{"type":"object"},"Extra":{"type":"string"}}},
                 "paths":{"/value":{"get":{"responses":{"200":{"description":"ok","content":{
                   "application/json":{"schema":{"$ref":"#/components/schemas/A~1B",
                     "description":"组合对象","properties":{"extra":{"$ref":"#/components/schemas/Extra"}},
                     "allOf":[{"$ref":"#/components/schemas/A~1B"}]}}}}}}}}}
                """);
        Map<String, Object> response = adapter.responseSchema(root,
                root.path("paths").path("/value").path("get"));
        assertThat(response).containsEntry("$ref", "#/$defs/A~1B")
                .containsEntry("description", "组合对象");
        assertThat(cast(cast(response.get("properties")).get("extra")))
                .containsEntry("$ref", "#/$defs/Extra");
        assertThat(mapper.valueToTree(response).at("/allOf/0/$ref").asText())
                .isEqualTo("#/$defs/A~1B");
    }

    @Test
    void booleanSchemasMeanAcceptAllOrRejectAllRatherThanBooleanConstants() throws Exception {
        JsonNode root = mapper.readTree("""
                {"paths":{"/value":{"get":{"responses":{"200":{"description":"ok","content":{
                  "application/json":{"schema":{"type":"object","properties":{"open":true,"closed":false}}}
                }}}}}}}
                """);
        Map<String, Object> response = adapter.responseSchema(root,
                root.path("paths").path("/value").path("get"));
        Map<String, Object> properties = cast(response.get("properties"));
        assertThat(cast(properties.get("open"))).isEmpty();
        assertThat(cast(properties.get("closed"))).containsOnlyKeys("not");
        assertThat(cast(cast(properties.get("closed")).get("not"))).isEmpty();
    }

    @Test
    void mergesPathAndOperationParametersWithOperationOverride() throws Exception {
        JsonNode root = mapper.readTree("""
                {"components":{"schemas":{"Order":{"type":"object"}}},
                 "paths":{"/orders/{id}":{
                   "parameters":[{"name":"id","in":"path","required":true,
                     "schema":{"type":"string"}},
                    {"name":"q","in":"query","required":true,
                     "schema":{"type":"string"}}],
                   "get":{"parameters":[{"name":"q","in":"query","required":false,
                     "schema":{"type":"integer"}}],
                     "responses":{"200":{"description":"ok"}}}}}}
                """);
        JsonNode pathItem = root.path("paths").path("/orders/{id}");
        JsonNode operation = pathItem.path("get");

        Map<String, Object> schema = adapter.requestSchema(
                root,
                pathItem,
                operation
        );
        Map<String, Object> properties = cast(schema.get("properties"));
        Map<String, Object> query = cast(properties.get("query"));
        Map<String, Object> queryProperties = cast(query.get("properties"));

        assertThat(cast(properties.get("path")).get("required"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .containsExactly("id");
        assertThat(queryProperties.get("q"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("type", "integer");
        assertThat(query.get("required")).isEqualTo(java.util.List.of());
    }

    @Test
    void mapsMultipartToPartAndOpenApiRefsToLocalDefs() throws Exception {
        JsonNode root = mapper.readTree("""
                {"components":{"schemas":{"Order":{"type":"object","properties":{"id":{"type":"string"}}}}},
                 "paths":{"/orders":{"post":{"operationId":"orders.create",
                   "requestBody":{"required":true,"content":{"multipart/form-data":
                     {"schema":{"$ref":"#/components/schemas/Order"}}}},
                   "responses":{"204":{"description":"no content"}}}}}}
                """);
        JsonNode operation = root.path("paths").path("/orders").path("post");
        Map<String, Object> request = adapter.requestSchema(
                root,
                root.path("paths").path("/orders"),
                operation
        );
        Map<String, Object> properties = cast(request.get("properties"));
        assertThat(properties).containsKey("part");
        assertThat(request.get("required")).isEqualTo(java.util.List.of("part"));
        assertThat(cast(properties.get("part")).get("$ref"))
                .isEqualTo("#/$defs/Order");
        assertThat(request).containsKey("$defs");

        Map<String, Object> response = adapter.responseSchema(root, operation);
        assertThat(response).containsEntry("type", "null")
                .containsEntry("x-egon-schema-model", "yuheng-operation-response/v2");
    }

    @Test
    void selectsLowestTwoHundredThenDefaultResponse() throws Exception {
        JsonNode lowestSuccess = mapper.readTree("""
                {"paths":{"/orders":{"get":{"responses":{
                  "202":{"description":"accepted","content":{"application/json":
                    {"schema":{"type":"object","properties":{"accepted":{"type":"boolean"}}}}}},
                  "201":{"description":"created","content":{"application/json":
                    {"schema":{"type":"object","properties":{"created":{"type":"boolean"}}}}}}
                }}}}}
                """);
        Map<String, Object> response = adapter.responseSchema(
                lowestSuccess,
                lowestSuccess.path("paths").path("/orders").path("get")
        );
        assertThat(cast(response.get("properties"))).containsKey("created");

        JsonNode defaultResponse = mapper.readTree("""
                {"paths":{"/orders":{"get":{"responses":{
                  "default":{"description":"fallback","content":{"application/json":
                    {"schema":{"type":"object","properties":{"fallback":{"type":"boolean"}}}}}}
                }}}}}
                """);
        Map<String, Object> fallback = adapter.responseSchema(
                defaultResponse,
                defaultResponse.path("paths").path("/orders").path("get")
        );
        assertThat(cast(fallback.get("properties"))).containsKey("fallback");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }
}
