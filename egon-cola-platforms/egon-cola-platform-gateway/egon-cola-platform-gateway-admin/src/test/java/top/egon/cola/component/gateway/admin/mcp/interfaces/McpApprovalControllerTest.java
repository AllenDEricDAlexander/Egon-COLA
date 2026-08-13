package top.egon.cola.component.gateway.admin.mcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpApprovalRepository;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpApprovalControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-02T05:00:00Z");

    @Test
    void returnsPlaintextOnceAndPersistsOnlyBoundDigests() {
        JdbcMcpApprovalRepository store = mock(JdbcMcpApprovalRepository.class);
        SecureRandom random = mock(SecureRandom.class);
        doAnswer(invocation -> {
            byte[] value = invocation.getArgument(0);
            Arrays.fill(value, (byte) 7);
            return null;
        }).when(random).nextBytes(org.mockito.ArgumentMatchers.any());
        ObjectMapper objectMapper = new ObjectMapper();
        McpApprovalController controller = new McpApprovalController(
                store,
                objectMapper,
                random,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal());
        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("invoiceId", "invoice-7");
        arguments.put("amount", 42);

        top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO response = controller.issue(
                new top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO(
                        "billing",
                        "pay_invoice",
                        arguments,
                        90L
                ),
                authentication
        );

        ArgumentCaptor<top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO> captured =
                ArgumentCaptor.forClass(top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO.class);
        verify(store).issue(captured.capture());
        top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO approval = captured.getValue();
        assertThat(approval.subjectId()).isEqualTo("alice-sub");
        assertThat(approval.tenantId()).isEqualTo("tenant-a");
        assertThat(approval.clientId()).isEqualTo("finance-web");
        assertThat(approval.serverCode()).isEqualTo("billing");
        assertThat(approval.toolName()).isEqualTo("pay_invoice");
        assertThat(approval.tokenDigest())
                .isEqualTo(McpSecurityDigests.token(
                        response.approvalToken()
                ))
                .doesNotContain(response.approvalToken());
        assertThat(approval.argumentDigest()).isEqualTo(
                McpSecurityDigests.arguments(objectMapper, arguments)
        );
        assertThat(approval.expiresAt()).isEqualTo(NOW.plusSeconds(90));
        assertThat(response.toString()).contains("<redacted>")
                .doesNotContain(response.approvalToken());
    }

    private IdentityPrincipal principal() {
        return new IdentityPrincipal(
                "alice-sub",
                "tenant-a",
                "session-1",
                "finance-web",
                "token-1",
                2L,
                Set.of("gateway-admin"),
                NOW.minusSeconds(30),
                NOW.plusSeconds(300)
        );
    }
}
