package top.egon.cola.component.yuheng.mcp.engine.mcp.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeServer;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpGatewayIdentityAuthenticatorTest {

    @Test
    void bindsIdpResourceToConfiguredMcpResourceUri() {
        McpRuntimeServer server = new McpRuntimeServer(
                "server-1", "billing", "Billing", null, null,
                Set.of(McpProtocolDialect.STABLE_2025_11_25),
                "https://resource.egon.top/mcp/billing", 30, true
        );

        assertEquals(
                Map.of("tianquan-shoubing.resource-uri",
                        "https://resource.egon.top/mcp/billing"),
                McpGatewayIdentityAuthenticator.securityAttributes(server)
        );
    }
}
