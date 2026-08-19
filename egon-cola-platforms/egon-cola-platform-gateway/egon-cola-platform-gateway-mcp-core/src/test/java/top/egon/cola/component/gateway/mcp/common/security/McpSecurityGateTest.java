package top.egon.cola.component.gateway.mcp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpSecurityGateTest {

    private static final Instant NOW = Instant.parse("2026-08-02T05:00:00Z");

    @Test
    void highRiskToolRequiresApprovalBoundToExactRequestDigest() {
        McpRuntimeTool tool = tool("HIGH");
        Map<String, Object> approvedArguments = Map.of(
                "invoiceId", "invoice-7",
                "amount", 42
        );
        String token = "single-use-approval";
        String expectedTokenDigest = McpSecurityDigests.token(token);
        String expectedArgumentDigest = McpSecurityDigests.arguments(
                new ObjectMapper(),
                approvedArguments
        );
        AtomicBoolean consumed = new AtomicBoolean();
        McpApprovalPort approvals = request -> {
            if (!expectedTokenDigest.equals(request.tokenDigest())
                    || !expectedArgumentDigest.equals(
                    request.argumentDigest())) {
                return Mono.just(McpApprovalPort.Result.MISMATCH);
            }
            return Mono.just(consumed.compareAndSet(false, true)
                    ? McpApprovalPort.Result.APPROVED
                    : McpApprovalPort.Result.CONSUMED);
        };
        McpSecurityGate gate = new McpSecurityGate(
                request -> Mono.just(McpAuthorizationPort.Decision.allowed(
                        7L,
                        3L,
                        11L
                )),
                approvals,
                new ObjectMapper()
        );

        assertDenied(
                gate,
                tool,
                approvedArguments,
                null,
                McpErrorCode.MCP_APPROVAL_REQUIRED
        );
        assertDenied(
                gate,
                tool,
                Map.of("invoiceId", "invoice-7", "amount", 43),
                token,
                McpErrorCode.MCP_APPROVAL_MISMATCH
        );
        assertDoesNotThrow(() -> authorize(
                gate,
                tool,
                approvedArguments,
                token
        ));
        assertDenied(
                gate,
                tool,
                approvedArguments,
                token,
                McpErrorCode.MCP_APPROVAL_CONSUMED
        );
    }

    @Test
    void authorizationUsesExactPrimitiveAndDeclaredPermissionKeys() {
        McpRuntimeTool tool = tool("LOW");
        McpSecurityGate gate = new McpSecurityGate(
                request -> {
                    assertEquals(Set.of(
                            "invoice:pay",
                            "mcp:billing:tool:pay_invoice:call"
                    ), request.requiredPermissions());
                    return Mono.just(McpAuthorizationPort.Decision.denied(
                            "RBAC3_PERMISSION_DENIED",
                            7L,
                            3L,
                            11L
                    ));
                },
                request -> Mono.just(McpApprovalPort.Result.APPROVED),
                new ObjectMapper()
        );

        assertDenied(
                gate,
                tool,
                Map.of(),
                null,
                McpErrorCode.MCP_FORBIDDEN
        );
    }

    @Test
    void promptAuthorizationUsesExactPrimitivePermissionKey() {
        McpRuntimePrompt prompt = new McpRuntimePrompt(
                "prompt-1",
                "billing",
                "review_invoice",
                null,
                "STRICT_TEMPLATE",
                "Review ${invoiceId}",
                null,
                null,
                java.util.List.of("invoiceId"),
                Set.of("invoice:read"),
                true
        );
        McpSecurityGate gate = new McpSecurityGate(
                request -> {
                    assertEquals(Set.of(
                            "invoice:read",
                            "mcp:billing:prompt:review_invoice:get"
                    ), request.requiredPermissions());
                    return Mono.just(
                            McpAuthorizationPort.Decision.allowed(7L, 3L, 11L)
                    );
                },
                request -> Mono.just(McpApprovalPort.Result.UNAVAILABLE),
                new ObjectMapper()
        );

        assertDoesNotThrow(() -> Mono.from(gate.authorizePrompt(
                prompt,
                identity()
        )).block());
    }

    @Test
    void taskAuthorizationUsesActionSpecificPermissionKey() {
        McpSecurityGate gate = new McpSecurityGate(
                request -> {
                    assertEquals(Set.of(
                            "mcp:billing:tool:export_invoice:task:cancel"
                    ), request.requiredPermissions());
                    return Mono.just(
                            McpAuthorizationPort.Decision.allowed(7L, 3L, 11L)
                    );
                },
                request -> Mono.just(McpApprovalPort.Result.UNAVAILABLE),
                new ObjectMapper()
        );

        assertDoesNotThrow(() -> Mono.from(gate.authorizeTaskAction(
                "billing",
                "export_invoice",
                "cancel",
                identity()
        )).block());
    }

    @Test
    void userAudienceCanSupplyMcpProtocolClientWithoutUserClientClaim() {
        McpSecurityGate.IdentityContext context =
                McpSecurityGate.IdentityContext.from(Map.of(
                        "identity.issuer", "https://idp.internal",
                        "identity.subject", "alice-sub",
                        "identity.tenant-id", "tenant-a",
                        "idp.audience", "platform",
                        "idp.token-id", "token-1",
                        "idp.resource-uri", "https://resource.egon.top/gateway-mcp",
                        "idp.issued-at", NOW.minusSeconds(30).toString(),
                        "idp.expires-at", NOW.plusSeconds(300).toString()
                ));

        assertEquals("platform", context.clientId());
    }

    private void authorize(
            McpSecurityGate gate,
            McpRuntimeTool tool,
            Map<String, Object> arguments,
            String approvalToken) {
        Mono.from(gate.authorizeToolCall(
                tool,
                identity(),
                arguments,
                approvalToken
        )).block();
    }

    private void assertDenied(
            McpSecurityGate gate,
            McpRuntimeTool tool,
            Map<String, Object> arguments,
            String token,
            McpErrorCode expected) {
        McpProtocolException error = assertThrows(
                McpProtocolException.class,
                () -> authorize(gate, tool, arguments, token)
        );
        assertEquals(expected, error.code());
    }

    private McpSecurityGate.IdentityContext identity() {
        return new McpSecurityGate.IdentityContext(
                "https://idp.internal",
                "alice-sub",
                "tenant-a",
                "finance-web",
                "token-1",
                "https://resource.egon.top/gateway-mcp",
                NOW.minusSeconds(30),
                NOW.plusSeconds(300),
                7L,
                3L,
                11L
        );
    }

    private McpRuntimeTool tool(String riskLevel) {
        return new McpRuntimeTool(
                "tool-1",
                "billing",
                "pay_invoice",
                "Pay an invoice",
                "LOCAL_OPERATION",
                "operation-42",
                "HTTP",
                null,
                "{\"type\":\"object\"}",
                "{\"type\":\"object\"}",
                Map.of(),
                Set.of("invoice:pay"),
                riskLevel,
                false,
                true
        );
    }
}
