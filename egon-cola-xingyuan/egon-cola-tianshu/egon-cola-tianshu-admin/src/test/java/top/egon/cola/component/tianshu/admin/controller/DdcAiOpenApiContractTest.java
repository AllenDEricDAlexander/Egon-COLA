package top.egon.cola.component.tianshu.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import top.egon.cola.component.tianshu.admin.config.DdcAiOpenApiConfiguration;
import top.egon.cola.component.tianshu.admin.config.DdcAiWebConfiguration;
import top.egon.cola.component.tianshu.admin.controller.ai.DdcAiResourceController;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiOptimizationService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;

import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DdcAiResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({DdcAiOpenApiConfiguration.class, DdcAiWebConfiguration.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocWebMvcConfiguration.class,
        MultipleOpenApiSupportConfiguration.class})
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class DdcAiOpenApiContractTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean(name = "ddcAiResourceService") private DdcAiResourceService service;
    @MockBean(name = "ddcAiOptimizationService") private DdcAiOptimizationService optimizationService;
    @MockBean(name = "ddcAiFeedbackService") private DdcAiFeedbackService feedbackService;

    @Test
    void generatedTianshuOpenApiContainsEveryRestOperationAndExactSecurityContract() throws Exception {
        byte[] bytes = mockMvc.perform(get("/v3/api-docs/tianshu").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        JsonNode api = objectMapper.readTree(bytes);

        assertThat(api.path("openapi").asText()).startsWith("3.1");
        JsonNode paths = api.path("paths");
        JsonNode resourcePage = paths.path("/api/v1/tianshu/ai/resources").path("get");
        assertOperation(paths, "/api/v1/tianshu/ai/resources", "get", "pageAiResources", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources", "post", "createAiResource", "201");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}", "get", "getAiResource", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/draft", "put", "replaceAiDraft", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}", "delete", "deleteAiResource", "204");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/versions", "get", "pageAiVersions", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/versions/{versionNo}", "get", "getAiVersion", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/publications", "post", "publishAiDraft", "201");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/rollbacks", "post", "rollbackAiPublication", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/promotions", "post", "promoteAiVersion", "201");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/publications", "get", "pageAiPublications", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/optimization-policy", "get",
                "getAiOptimizationPolicy", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/optimization-policy", "put",
                "replaceAiOptimizationPolicy", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/optimization-runs", "post",
                "triggerAiOptimizationRun", "202");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/optimization-runs", "get",
                "pageAiOptimizationRuns", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/optimization-runs/{runId}", "get",
                "getAiOptimizationRun", "200");
        assertOperation(paths, "/api/v1/tianshu/ai/resources/{id}/usage-feedback", "get",
                "pageAiUsageFeedback", "200");
        JsonNode trigger = paths.path("/api/v1/tianshu/ai/resources/{id}/optimization-runs").path("post");
        assertHeaderParameter(trigger, "Idempotency-Key", true);
        assertThat(trigger.path("responses").path("202").path("headers").has("Location")).isTrue();
        assertThat(trigger.path("responses").has("503")).isTrue();
        assertThat(paths.path("/api/v1/tianshu/ai/resources/{id}/optimization-runs").path("get")
                .path("parameters").findValuesAsText("name")).contains("state", "pageNo", "pageSize");
        assertThat(paths.path("/api/v1/tianshu/ai/resources/{id}/usage-feedback").path("get")
                .path("parameters").findValuesAsText("name"))
                .contains("versionNo", "outcome", "scoreSource", "maxScore", "pageNo", "pageSize");
        assertParameterPattern(resourcePage, "bizCode", "^[^\\p{Cntrl}]+$");

        JsonNode scheme = api.path("components").path("securitySchemes").path("tianshuBearerAuth");
        assertThat(scheme.path("type").asText()).isEqualTo("http");
        assertThat(scheme.path("scheme").asText()).isEqualTo("bearer");
        assertThat(scheme.path("bearerFormat").asText()).isEqualTo("JWT");

        JsonNode delete = paths.path("/api/v1/tianshu/ai/resources/{id}").path("delete");
        assertThat(delete.path("responses").path("204").has("content")).isFalse();
        assertHeaderParameter(delete, "X-Egon-Tianshu-Tenant", true);
        assertThat(delete.path("parameters").findValuesAsText("name"))
                .contains("expectedRevision");

        JsonNode publish = paths.path("/api/v1/tianshu/ai/resources/{id}/publications").path("post");
        JsonNode promote = paths.path("/api/v1/tianshu/ai/resources/{id}/promotions").path("post");
        assertHeaderParameter(publish, "Idempotency-Key", true);
        assertHeaderParameter(promote, "Idempotency-Key", true);
        assertHeaderParameter(promote, "X-Egon-Tianshu-Tenant", true);
        assertThat(publish.path("responses").path("201").path("headers").has("Location")).isTrue();
        assertThat(promote.path("responses").path("201").path("headers").has("Location")).isTrue();
        assertSecurityErrorOneOf(promote.path("responses").path("401"));
        assertSecurityErrorOneOf(promote.path("responses").path("403"));

        JsonNode createBody = paths.path("/api/v1/tianshu/ai/resources").path("post")
                .path("requestBody").path("content").path("application/json").path("schema");
        assertThat(createBody.path("$ref").asText()).contains("DdcAiCreateCommand");
        JsonNode kindEnum = api.path("components").path("schemas").path("DdcAiCreateCommand")
                .path("properties").path("kind").path("enum");
        assertThat(StreamSupport.stream(kindEnum.spliterator(), false).map(JsonNode::asText).toList())
                .contains("PROMPT", "SKILL");
        JsonNode publicationFields = api.path("components").path("schemas").path("DdcAiPublicationResult")
                .path("properties");
        assertThat(publicationFields.has("id")).isTrue();
        assertThat(publicationFields.has("publicationId")).isFalse();
        assertStringSchema(publicationFields.path("id"));
        assertThat(publicationFields.path("id").path("pattern").asText()).isEqualTo("^[1-9][0-9]{0,18}$");
        assertStringSchema(publicationFields.path("resourceRevision"));

        JsonNode versionFields = api.path("components").path("schemas").path("DdcAiVersionSummaryDTO")
                .path("properties");
        assertStringSchema(versionFields.path("id"));
        assertStringSchema(versionFields.path("resourceId"));
        assertStringSchema(versionFields.path("versionNo"));
        assertNullable(versionFields.path("sourceVersionId"));
        assertStringSchema(versionFields.path("createdBy"));

        JsonNode resourceFields = api.path("components").path("schemas").path("DdcAiResourceSummaryDTO")
                .path("properties");
        assertStringSchema(resourceFields.path("id"));
        assertStringSchema(resourceFields.path("revision"));
        assertThat(resourceFields.path("resourceKey").path("pattern").asText())
                .isEqualTo("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$");
        assertThat(resourceFields.path("displayName").path("minLength").asInt()).isEqualTo(1);
        assertNullable(resourceFields.path("currentVersionId"));
        assertNullable(resourceFields.path("currentVersionNo"));

        JsonNode promotionCommand = api.path("components").path("schemas").path("DdcAiPromotionCommand");
        assertRequired(promotionCommand, "expectedTargetRevision");
        assertNullable(promotionCommand.path("properties").path("expectedTargetRevision"));
        JsonNode content = api.path("components").path("schemas").path("DdcAiContentDTO");
        assertRequired(content, "promptText");
        assertRequired(content, "nodes");
        assertNullable(content.path("properties").path("promptText"));
        JsonNode node = api.path("components").path("schemas").path("DdcAiNodeDTO");
        assertRequired(node, "content");
        assertNullable(node.path("properties").path("content"));
        assertRequired(api.path("components").path("schemas").path("DdcAiVersionSummaryDTO"), "createdBy");
    }

    @Test
    void applicationConfigurationKeepsOpenApiDisabledByDefault() throws Exception {
        var application = new YamlPropertySourceLoader().load(
                "application", new ClassPathResource("application.yml")).getFirst();

        assertThat(application.getProperty("springdoc.api-docs.enabled"))
                .isEqualTo("${TIANSHU_HTTP_OPENAPI_ENABLED:false}");
    }

    private void assertOperation(JsonNode paths, String path, String method, String operationId,
            String successStatus) {
        JsonNode operation = paths.path(path).path(method);
        assertThat(operation.path("operationId").asText()).isEqualTo(operationId);
        assertThat(operation.path("responses").has(successStatus)).isTrue();
        for (JsonNode response : operation.path("responses")) {
            assertThat(response.path("headers").path("Cache-Control").path("schema").path("example").asText())
                    .isEqualTo("no-store");
        }
        assertThat(operation.path("security").toString()).contains("tianshuBearerAuth");
        assertThat(StreamSupport.stream(operation.path("parameters").spliterator(), false)
                .map(parameter -> parameter.path("name").asText()).toList()).doesNotContain("Authorization");
        assertHeaderParameter(operation, "X-Egon-Tianshu-Tenant", true);
    }

    private void assertHeaderParameter(JsonNode operation, String name, boolean required) {
        boolean found = false;
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equals(parameter.path("name").asText())) {
                found = true;
                assertThat(parameter.path("in").asText()).isEqualTo("header");
                assertThat(parameter.path("required").asBoolean()).isEqualTo(required);
            }
        }
        assertThat(found).as("OpenAPI parameter %s", name).isTrue();
    }

    private void assertParameterPattern(JsonNode operation, String name, String pattern) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equals(parameter.path("name").asText())) {
                assertThat(parameter.path("schema").path("pattern").asText()).isEqualTo(pattern);
                return;
            }
        }
        throw new AssertionError("Missing OpenAPI parameter: " + name);
    }

    private void assertSecurityErrorOneOf(JsonNode response) {
        JsonNode oneOf = response.path("content").path("application/json").path("schema").path("oneOf");
        assertThat(oneOf.toString()).contains("DdcAdminSecurityErrorResult", "ResultRecord");
    }

    private void assertStringSchema(JsonNode schema) {
        assertThat(schema.path("type").asText()).isEqualTo("string");
        assertThat(schema.path("pattern").asText()).isNotBlank();
    }

    private void assertNullable(JsonNode schema) {
        boolean nullable = schema.path("nullable").asBoolean()
                || StreamSupport.stream(schema.path("type").spliterator(), false)
                .anyMatch(type -> "null".equals(type.asText()));
        assertThat(nullable).isTrue();
    }

    private void assertRequired(JsonNode schema, String property) {
        assertThat(StreamSupport.stream(schema.path("required").spliterator(), false)
                .map(JsonNode::asText).toList())
                .as("required properties for schema %s", schema)
                .contains(property);
    }
}
