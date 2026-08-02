package top.egon.cola.component.gateway.admin.mcp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpUnifiedReleaseTest {

    @Test
    void publishingGatewayReleaseRejectsUnknownLocalOperation() {
        GatewayCatalogStore catalog = mock(GatewayCatalogStore.class);
        JdbcMcpArtifactMetadataStore artifacts =
                mock(JdbcMcpArtifactMetadataStore.class);
        when(catalog.findOperation("missing-operation"))
                .thenReturn(Optional.empty());
        McpValidationService service = new McpValidationService(
                catalog,
                artifacts,
                new ObjectMapper()
        );

        McpValidationException error = assertThrows(
                McpValidationException.class,
                () -> service.requireValid(content("missing-operation"))
        );

        assertEquals("GATEWAY_MCP_OPERATION_NOT_FOUND", error.code());
    }

    private McpRuleContent content(String operationId) {
        return new McpRuleContent(
                List.of(new McpRuntimeServer(
                        "server-1",
                        "billing",
                        "Billing",
                        null,
                        null,
                        Set.of(McpProtocolDialect.STABLE_2025_11_25),
                        "gateway-mcp",
                        30,
                        true
                )),
                List.of(new McpRuntimeTool(
                        "tool-1",
                        "billing",
                        "invoice.get",
                        null,
                        "LOCAL_OPERATION",
                        operationId,
                        null,
                        "{\"type\":\"object\"}",
                        "{\"type\":\"object\"}",
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Set.of("mcp:billing:tool:invoice.get:call"),
                        "LOW",
                        true,
                        true
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
