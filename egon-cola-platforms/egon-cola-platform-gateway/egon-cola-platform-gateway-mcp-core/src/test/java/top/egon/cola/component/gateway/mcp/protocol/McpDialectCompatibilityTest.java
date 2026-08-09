package top.egon.cola.component.gateway.mcp.protocol;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpDialectCompatibilityTest {

    private final McpJsonRpcCodec codec = new McpJsonRpcCodec();
    private final McpDialectAdapter stable = new StableMcpDialectAdapter(codec);
    private final McpDialectAdapter rc = new RcMcpDialectAdapter(codec);
    private final McpDialectAdapter legacy = new LegacySseMcpAdapter(codec);

    @Test
    void rcRequiresPerRequestMetadataAndHeaderBodyAgreement() {
        HttpMcpRequest mismatch = request(
                "/mcp/billing",
                Map.of(
                        "Mcp-Protocol-Version", "2026-07-28",
                        "Mcp-Method", "tools/list"
                ),
                """
                        {"jsonrpc":"2.0","id":1,"method":"tools/call",
                         "params":{"name":"invoice.get","_meta":{"client":"qa"}}}
                        """
        );

        McpProtocolException mismatchError = assertThrows(
                McpProtocolException.class,
                () -> rc.decode(mismatch)
        );
        assertEquals(McpErrorCode.MCP_HEADER_MISMATCH, mismatchError.code());

        HttpMcpRequest missingMeta = request(
                "/mcp/billing",
                Map.of(
                        "Mcp-Protocol-Version", "2026-07-28",
                        "Mcp-Method", "tools/list"
                ),
                """
                        {"jsonrpc":"2.0","id":1,"method":"tools/list",
                         "params":{}}
                        """
        );
        McpProtocolException metadataError = assertThrows(
                McpProtocolException.class,
                () -> rc.decode(missingMeta)
        );
        assertEquals(
                McpErrorCode.MCP_CLIENT_CAPABILITY_REQUIRED,
                metadataError.code()
        );
    }

    @Test
    void stableRcAndLegacyNormalizeValidRequests() {
        McpJsonRpcRequest stableRequest = stable.decode(request(
                "/mcp/billing",
                Map.of("Mcp-Protocol-Version", "2025-11-25"),
                """
                        {"jsonrpc":"2.0","id":"s-1","method":"initialize",
                         "params":{"protocolVersion":"2025-11-25"}}
                        """
        ));
        McpJsonRpcRequest rcRequest = rc.decode(request(
                "/mcp/billing",
                Map.of(
                        "Mcp-Protocol-Version", "2026-07-28",
                        "Mcp-Method", "server/discover"
                ),
                """
                        {"jsonrpc":"2.0","id":"r-1","method":"server/discover",
                         "params":{"_meta":{"capabilities":{"tasks":true}}}}
                        """
        ));
        McpJsonRpcRequest legacyRequest = legacy.decode(request(
                "/legacy/mcp/billing/message",
                Map.of(),
                "{" +
                        "\"jsonrpc\":\"2.0\",\"id\":9," +
                        "\"method\":\"ping\"}"
        ));

        assertEquals("initialize", stableRequest.method());
        assertEquals("server/discover", rcRequest.method());
        assertEquals("ping", legacyRequest.method());
        assertEquals(
                McpProtocolDialect.STABLE_2025_11_25,
                stable.dialect()
        );
        assertEquals(McpProtocolDialect.RC_2026_07_28, rc.dialect());
        assertEquals(McpProtocolDialect.LEGACY_2024_SSE, legacy.dialect());
    }

    @Test
    void codecRejectsBatchDeepJsonAndInvalidIds() {
        assertCode(
                "[{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}]",
                McpErrorCode.MCP_INVALID_REQUEST
        );
        assertCode(
                "{\"jsonrpc\":\"2.0\",\"id\":true,\"method\":\"ping\"}",
                McpErrorCode.MCP_INVALID_REQUEST
        );
        String deep = "{\"jsonrpc\":\"2.0\",\"id\":1,"
                + "\"method\":\"ping\",\"params\":"
                + "{\"x\":".repeat(65)
                + "0"
                + "}".repeat(65)
                + "}";
        assertCode(deep, McpErrorCode.MCP_INVALID_REQUEST);
    }

    private void assertCode(String body, McpErrorCode expected) {
        McpProtocolException error = assertThrows(
                McpProtocolException.class,
                () -> stable.decode(request(
                        "/mcp/billing",
                        Map.of("Mcp-Protocol-Version", "2025-11-25"),
                        body
                ))
        );
        assertEquals(expected, error.code());
    }

    private HttpMcpRequest request(
            String path,
            Map<String, String> headers,
            String body) {
        return new HttpMcpRequest(
                path,
                "POST",
                "application/json",
                headers,
                body
        );
    }
}
