package top.egon.cola.component.gateway.admin.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.McpValidationException;
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpArtifactMetadataRepository;
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
        GatewayCatalogRepository catalog = mock(GatewayCatalogRepository.class);
        JdbcMcpArtifactMetadataRepository artifacts =
                mock(JdbcMcpArtifactMetadataRepository.class);
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

    @Test
    void rejectsDuplicateToolNameWithinOneServer() {
        McpValidationService service = new McpValidationService(
                mock(GatewayCatalogRepository.class),
                mock(JdbcMcpArtifactMetadataRepository.class),
                new ObjectMapper()
        );
        McpRuleContent original = content("operation-1");
        McpRuntimeTool duplicate = new McpRuntimeTool(
                "tool-2",
                "billing",
                "invoice.get",
                null,
                "REMOTE_MCP",
                null,
                null,
                "mount-1",
                "{\"type\":\"object\"}",
                "{\"type\":\"object\"}",
                Map.of(),
                Set.of(),
                "LOW",
                true,
                true
        );
        McpRuleContent duplicated = new McpRuleContent(
                original.servers(),
                List.of(original.tools().getFirst(), duplicate),
                original.resources(),
                original.resourceTemplates(),
                original.prompts(),
                original.taskPolicies(),
                original.apps(),
                original.remoteProviders(),
                original.remoteMounts()
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.requireValid(duplicated)
        );

        assertEquals(
                "duplicate MCP capability name: invoice.get",
                error.getMessage()
        );
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
                        "https://resource.egon.top/gateway-mcp",
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
                        "HTTP",
                        null,
                        "{\"type\":\"object\"}",
                        "{\"type\":\"object\"}",
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
