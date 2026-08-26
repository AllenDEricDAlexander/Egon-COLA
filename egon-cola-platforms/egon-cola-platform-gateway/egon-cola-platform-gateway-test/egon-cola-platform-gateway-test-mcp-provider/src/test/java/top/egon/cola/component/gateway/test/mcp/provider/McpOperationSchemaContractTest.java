package top.egon.cola.component.gateway.test.mcp.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonMcpTool;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class McpOperationSchemaContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void everyEnabledMcpOperationDeclaresAStableToolContract() {
        Arrays.stream(McpJobController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(EgonMcpTool.class))
                .forEach(method -> {
                    EgonMcpTool tool = method.getAnnotation(EgonMcpTool.class);
                    Operation operation = method.getAnnotation(Operation.class);
                    assertThat(operation).isNotNull();
                    assertThat(operation.operationId()).isNotBlank();
                    assertThat(tool.enabled()).isTrue();
                    assertThat(tool.serverCode()).isEqualTo("unified-local");
                    assertThat(tool.name()).isNotBlank();
                    assertThat(tool.permissions()).containsAnyOf(
                            "mock:read", "mock:admin");
                    assertThat(tool.riskLevel()).isNotNull();
                });
    }

    @Test
    void keepsTheJobsOpenApiGoldenProjectionStable() throws Exception {
        assertThat(McpJobController.class.getAnnotation(EgonApiCatalog.class)
                .interfaceGroupCode()).isEqualTo("jobs");

        Set<String> operationIds = new TreeSet<>();
        Set<String> tools = new TreeSet<>();
        Arrays.stream(McpJobController.class.getDeclaredMethods())
                .forEach(method -> {
                    Operation operation = method.getAnnotation(Operation.class);
                    if (operation != null) {
                        operationIds.add(operation.operationId());
                    }
                    EgonMcpTool tool = method.getAnnotation(EgonMcpTool.class);
                    if (tool != null && tool.enabled()) {
                        tools.add(tool.name());
                    }
                });

        JsonNode golden;
        try (InputStream stream = getClass().getResourceAsStream(
                "/openapi/jobs-golden.json")) {
            assertThat(stream).isNotNull();
            golden = objectMapper.readTree(stream);
        }
        assertThat(golden.path("group").asText()).isEqualTo("jobs");
        assertThat(operationIds).isEqualTo(values(golden.path("operationIds")));
        assertThat(tools).isEqualTo(values(golden.path("mcpTools")));
    }

    private Set<String> values(JsonNode nodes) {
        Set<String> values = new HashSet<>();
        nodes.elements().forEachRemaining(node -> values.add(node.asText()));
        return values;
    }
}
