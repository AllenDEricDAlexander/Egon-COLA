package top.egon.cola.component.gateway.admin.runtime.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayEngineNodeConsistencyVO;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayRuntimeConsistencyVO;
import top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionService;
import top.egon.cola.component.gateway.admin.shared.controller.GatewayAdminExceptionHandler;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GatewayProjectionControllerContractTest {

    @Test
    void retainsEnvelopeMetadataAndConsistencyJsonShape() throws Exception {
        Instant now = Instant.parse("2026-09-05T00:00:00Z");
        var service = mock(GatewayProjectionService.class);
        var node = new DdcManagementConfigClientInstance("infra", "test", "ge", "node-1", "lease-1",
                "127.0.0.1", 18081, "CONFIG_CLIENT", "ONLINE", now, now, now.plusSeconds(30),
                Map.of("gateway.engine.role", "API_RPC"));
        when(service.engineNodes("group-1")).thenReturn(
                new GatewayProjectionEnvelopeVO<>(List.of(node), now, "DDC_CONFIG_CLIENT", false, null));
        when(service.runtimeConsistency("group-1")).thenReturn(new GatewayRuntimeConsistencyVO(
                "release-1", "SUCCESS", 1, 0, false, now, "DDC_CONFIG_CLIENT", false,
                List.of(new GatewayEngineNodeConsistencyVO("node-1", "lease-1", "ONLINE",
                        "NOT_READY", "ROLE_UNKNOWN", "release-1", 12L, "sha", "ACK_SUCCESS", now))));
        MockMvc mvc = mvc(service);
        String envelope = mvc.perform(get("/api/v1/gateway/admin/gateway-groups/group-1/engine-nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value[0].metadata['gateway.engine.role']").value("API_RPC"))
                .andReturn().getResponse().getContentAsString();
        assertThat(fields(envelope)).containsExactlyInAnyOrder("value", "observedAt", "source", "stale", "refreshError");
        String consistency = mvc.perform(get("/api/v1/gateway/admin/gateway-groups/group-1/runtime-consistency"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.consistent").value(false))
                .andExpect(jsonPath("$.nodes[0].reason").value("ROLE_UNKNOWN"))
                .andReturn().getResponse().getContentAsString();
        assertThat(fields(consistency)).containsExactlyInAnyOrder("targetReleaseId", "targetReleaseStatus",
                "engineNodeCount", "readyEngineNodeCount", "consistent", "observedAt", "source", "stale", "nodes");
        var tree = new ObjectMapper().readTree(consistency).get("nodes").get(0);
        var nodeFields = new HashSet<String>();
        tree.fieldNames().forEachRemaining(nodeFields::add);
        assertThat(nodeFields).containsExactlyInAnyOrder("instanceId", "leaseId", "leaseStatus", "status",
                "reason", "activeReleaseId", "activeRuleVersion", "activeRuleChecksum", "lastApplyStatus", "lastAckAt");
    }

    @Test
    void preservesExistingUnavailableStatusAndErrorCode() throws Exception {
        var service = mock(GatewayProjectionService.class);
        when(service.engineNodes("group-1")).thenThrow(new IllegalStateException("DDC management client is not configured"));
        mvc(service).perform(get("/api/v1/gateway/admin/gateway-groups/group-1/engine-nodes"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("GATEWAY_ADMIN_DDC_UNAVAILABLE"));
    }

    private HashSet<String> fields(String json) throws Exception {
        var fields = new HashSet<String>();
        new ObjectMapper().readTree(json).fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private MockMvc mvc(GatewayProjectionService service) {
        return MockMvcBuilders.standaloneSetup(new GatewayProjectionController(service))
                .setControllerAdvice(new GatewayAdminExceptionHandler()).build();
    }
}
