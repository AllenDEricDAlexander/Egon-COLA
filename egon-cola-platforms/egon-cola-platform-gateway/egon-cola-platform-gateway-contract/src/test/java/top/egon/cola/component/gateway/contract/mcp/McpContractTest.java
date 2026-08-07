package top.egon.cola.component.gateway.contract.mcp;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpContractTest {

    @Test
    void ruleContentIsDeterministicAndRejectsDuplicateCapabilityNames() {
        McpRuntimeTool second = tool("tool-2", "invoice.list");
        McpRuntimeTool first = tool("tool-1", "invoice.get");
        ArrayList<McpRuntimeTool> source = new ArrayList<>(List.of(second, first));

        McpRuleContent content = content(source);

        assertEquals(
                List.of("invoice.get", "invoice.list"),
                content.tools().stream().map(McpRuntimeTool::name).toList()
        );
        source.clear();
        assertEquals(2, content.tools().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> content.tools().add(tool("tool-3", "invoice.create"))
        );

        McpRuleContent duplicate = content(List.of(
                tool("tool-1", "invoice.get"),
                tool("tool-2", "invoice.get")
        ));
        assertThrows(IllegalArgumentException.class, duplicate::validate);
    }

    @Test
    void emptyRuleContentHasEveryPrimitiveCollection() {
        McpRuleContent empty = McpRuleContent.empty();

        assertEquals(List.of(), empty.servers());
        assertEquals(List.of(), empty.tools());
        assertEquals(List.of(), empty.resources());
        assertEquals(List.of(), empty.resourceTemplates());
        assertEquals(List.of(), empty.prompts());
        assertEquals(List.of(), empty.taskPolicies());
        assertEquals(List.of(), empty.apps());
        assertEquals(List.of(), empty.remoteProviders());
        assertEquals(List.of(), empty.remoteMounts());
        empty.validate();
    }

    @Test
    void jsonRpcContractsAreDefensiveAndExposeStableErrorCodes() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("invoiceId", "inv-1");
        McpJsonRpcRequest request = new McpJsonRpcRequest(
                "2.0",
                7L,
                "tools/call",
                params,
                Map.of("traceparent", "00-test")
        );
        params.clear();

        assertEquals("inv-1", request.params().get("invoiceId"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> request.params().put("providerUrl", "http://invalid")
        );

        McpJsonRpcResponse response = McpJsonRpcResponse.methodNotFound(7L);
        assertEquals(-32601, response.error().code());
        assertEquals(
                McpErrorCode.MCP_METHOD_NOT_FOUND,
                response.error().dataCode()
        );
        assertEquals(
                "MCP_METHOD_NOT_FOUND",
                response.error().data().get("code")
        );
    }

    private McpRuleContent content(List<McpRuntimeTool> tools) {
        return new McpRuleContent(
                List.of(server()),
                tools,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private McpRuntimeServer server() {
        return new McpRuntimeServer(
                "server-1",
                "billing",
                "Billing",
                "Billing capabilities",
                "Use billing capabilities for approved business operations.",
                Set.of(McpProtocolDialect.STABLE_2025_11_25),
                "gateway-mcp",
                30,
                true
        );
    }

    private McpRuntimeTool tool(String id, String name) {
        return new McpRuntimeTool(
                id,
                "billing",
                name,
                "Invoice operation",
                "LOCAL_OPERATION",
                "operation-1",
                "HTTP",
                null,
                "{\"type\":\"object\"}",
                "{\"type\":\"object\"}",
                Map.of("readOnlyHint", "true"),
                Set.of("mcp:billing:tool:" + name + ":call"),
                "LOW",
                true,
                true
        );
    }
}
