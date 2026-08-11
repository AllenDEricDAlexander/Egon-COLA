package top.egon.cola.component.gateway.engine.mcp;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;

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
                Map.of("idp.resource-uri",
                        "https://resource.egon.top/mcp/billing"),
                McpGatewayIdentityAuthenticator.securityAttributes(server)
        );
    }
}
