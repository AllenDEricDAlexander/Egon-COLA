package top.egon.cola.component.gateway.admin.reporting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayOperationSchemaValidatorTest {

    private final GatewayOperationSchemaValidator validator =
            new GatewayOperationSchemaValidator(new ObjectMapper());

    @Test
    void acceptsCompleteV2HttpSchemas() {
        assertThatCode(() -> validator.validate(
                "orders:http:GET:/orders/{id}",
                "HTTP",
                requestSchema(),
                responseSchema(),
                Map.of()
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsV1MissingAndMismatchedSchemaModels() {
        Map<String, Object> request = new LinkedHashMap<>(requestSchema());
        request.put("x-egon-schema-model", "gateway-operation-request/v1");
        assertThatThrownBy(() -> validator.validate(
                "orders:get",
                "HTTP",
                request,
                responseSchema(),
                Map.of()
        )).hasMessageContaining("request/v2");

        Map<String, Object> missing = new LinkedHashMap<>(requestSchema());
        missing.remove("x-egon-schema-model");
        assertThatThrownBy(() -> validator.validate(
                "orders:get",
                "HTTP",
                missing,
                responseSchema(),
                Map.of()
        )).hasMessageContaining("schema model");
    }

    @Test
    void rejectsIllegalGroupsRefsAndRequiredArrays() {
        Map<String, Object> unknownGroup = new LinkedHashMap<>(requestSchema());
        Map<String, Object> unknownProperties = new LinkedHashMap<>(
                cast(unknownGroup.get("properties"))
        );
        unknownProperties.put("unknown", Map.of("type", "object"));
        unknownGroup.put("properties", unknownProperties);
        assertThatThrownBy(() -> validator.validate(
                "orders:unknown",
                "HTTP",
                unknownGroup,
                responseSchema(),
                Map.of()
        )).hasMessageContaining("location group");

        Map<String, Object> externalRef = new LinkedHashMap<>(requestSchema());
        Map<String, Object> query = new LinkedHashMap<>(cast(
                cast(externalRef.get("properties")).get("query")
        ));
        query.put("$ref", "https://example.com/schema");
        Map<String, Object> externalProperties = new LinkedHashMap<>(
                cast(externalRef.get("properties"))
        );
        externalProperties.put("query", query);
        externalRef.put("properties", externalProperties);
        assertThatThrownBy(() -> validator.validate(
                "orders:external",
                "HTTP",
                externalRef,
                responseSchema(),
                Map.of()
        )).hasMessageContaining("external");

        Map<String, Object> malformedRequired = new LinkedHashMap<>(requestSchema());
        Map<String, Object> malformedQuery = new LinkedHashMap<>(cast(
                cast(malformedRequired.get("properties")).get("query")
        ));
        malformedQuery.put("required", List.of("ghost"));
        Map<String, Object> malformedProperties = new LinkedHashMap<>(
                cast(malformedRequired.get("properties"))
        );
        malformedProperties.put("query", malformedQuery);
        malformedRequired.put("properties", malformedProperties);
        assertThatThrownBy(() -> validator.validate(
                "orders:required",
                "HTTP",
                malformedRequired,
                responseSchema(),
                Map.of()
        )).hasMessageContaining("required");
    }

    @Test
    void rejectsMcpHeaderCookieAndPartInputs() {
        Map<String, Object> request = new LinkedHashMap<>(requestSchema());
        Map<String, Object> properties = new LinkedHashMap<>(cast(
                request.get("properties")
        ));
        properties.put("header", grouped("tenant"));
        request.put("properties", properties);
        Map<String, Object> attributes = Map.of(
                "mcpExposure", Map.of(
                        "registerMcp", true,
                        "mcpServerCode", "trade-mcp",
                        "mcpName", "orders_get",
                        "requiredPermissions", List.of("order:read"),
                        "riskLevel", "LOW",
                        "idempotent", true
                )
        );
        assertThatThrownBy(() -> validator.validate(
                "orders:header",
                "HTTP",
                request,
                responseSchema(),
                attributes
        )).hasMessageContaining("required HEADER");
    }

    private Map<String, Object> requestSchema() {
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "x-egon-schema-model", "gateway-operation-request/v2",
                "type", "object",
                "properties", Map.of(
                        "path", grouped("id"),
                        "query", grouped("size")
                ),
                "additionalProperties", false
        );
    }

    private Map<String, Object> responseSchema() {
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "x-egon-schema-model", "gateway-operation-response/v2",
                "type", "object",
                "properties", Map.of(),
                "additionalProperties", false
        );
    }

    private Map<String, Object> grouped(String name) {
        return Map.of(
                "type", "object",
                "properties", Map.of(name, Map.of("type", "string")),
                "required", List.of(name),
                "additionalProperties", false
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }
}
