package top.egon.cola.component.gateway.admin.mcp.interfaces;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.component.gateway.admin.interfaces.management.GatewayAdminExceptionHandler;
import top.egon.cola.component.gateway.admin.mcp.application.McpControlPlaneService;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class McpAdminApiIT {

    @Test
    void serverApiListsDraftsAndRequiresIdempotencyKeyForWrites()
            throws Exception {
        McpControlPlaneService service = mock(McpControlPlaneService.class);
        when(service.listServers("group-1")).thenReturn(List.of(
                new McpControlPlaneService.ServerView(
                        "server-1",
                        "group-1",
                        "billing",
                        "Billing",
                        null,
                        null,
                        Set.of("STABLE_2025_11_25"),
                        "https://resource.egon.top/gateway-mcp",
                        30,
                        true,
                        0,
                        Instant.parse("2026-08-02T00:00:00Z"),
                        Instant.parse("2026-08-02T00:00:00Z")
                )
        ));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new McpServerController(service)
                )
                .setControllerAdvice(new GatewayAdminExceptionHandler())
                .build();

        mvc.perform(get("/api/v1/gateway/admin/mcp/servers")
                        .queryParam("gatewayGroupId", "group-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serverCode").value("billing"));

        mvc.perform(post("/api/v1/gateway/admin/mcp/servers")
                        .contentType("application/json")
                        .content("""
                                {
                                  "gatewayGroupId": "group-1",
                                  "serverCode": "billing",
                                  "displayName": "Billing",
                                  "dialects": ["STABLE_2025_11_25"],
                                  "resourceUri": "https://resource.egon.top/gateway-mcp",
                                  "listCacheTtlSeconds": 30,
                                  "expectedRevision": 0,
                                  "expectedDraftRevision": 0,
                                  "changeReason": "create billing MCP"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
