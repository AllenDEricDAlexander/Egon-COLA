package top.egon.cola.component.gateway.admin.openapi.converter;

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
                .containsEntry("x-egon-schema-model", "gateway-operation-response/v2");
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
