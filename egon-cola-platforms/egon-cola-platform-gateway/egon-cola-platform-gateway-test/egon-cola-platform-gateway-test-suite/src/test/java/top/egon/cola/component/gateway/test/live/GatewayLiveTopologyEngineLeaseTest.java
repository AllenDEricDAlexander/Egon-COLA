package top.egon.cola.component.gateway.test.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import top.egon.cola.component.gateway.contract.runtime.GatewayEngineRoleEnum;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayLiveTopologyEngineLeaseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requiresBothRolesAndOneAcknowledgedIdentityWhileIgnoringOfflineHistory() {
        Instant now = Instant.parse("2026-07-27T11:00:00Z");
        var api = node("api", "API_RPC", "sha", "ONLINE");
        var mcp = node("mcp", "MCP", "sha", "ONLINE");
        var offline = node("old", "COMBINED", "old", "OFFLINE");
        JsonNode healthy = objectMapper.valueToTree(Map.of("value", List.of(api, mcp, offline)));
        assertThat(GatewayLiveTopologyIT.hasUnifiedRoleAcks(healthy, "release-1", 2, now)).isTrue();
        assertThat(GatewayLiveTopologyIT.activeEngineLeases(healthy, now, GatewayEngineRoleEnum.API_RPC))
                .containsOnlyKeys("api");
        assertThat(GatewayLiveTopologyIT.activeEngineLeases(healthy, now, GatewayEngineRoleEnum.MCP))
                .containsOnlyKeys("mcp");
        for (var broken : List.of(List.of(api), List.of(api, node("mcp", "MCP", "old", "ONLINE")),
                List.of(api, node("old", "COMBINED", "sha", "ONLINE")))) {
            assertThat(GatewayLiveTopologyIT.hasUnifiedRoleAcks(
                    objectMapper.valueToTree(Map.of("value", broken)), "release-1", broken.size(), now)).isFalse();
        }
    }

    private Map<String, Object> node(String id, String role, String checksum, String status) {
        return Map.of("instanceId", id, "leaseId", "lease-" + id, "status", status,
                "expireAt", "2026-07-27T11:01:00Z", "metadata", Map.of(
                        "gateway.engine.role", role, "activeReleaseId", "release-1",
                        "activeRuleVersion", "12", "activeRuleChecksum", checksum,
                        "lastApplyStatus", "ACK_SUCCESS", "lastAckAt", "2026-07-27T10:59:00Z"));
    }

    @Test
    void includesOnlyOnlineUnexpiredEngineLeases() throws Exception {
        JsonNode projection = objectMapper.readTree("""
                {
                  "value": [
                    {
                      "instanceId": "online-engine",
                      "leaseId": "online-lease",
                      "status": "ONLINE",
                      "expireAt": "2026-07-27T11:01:00Z"
                    },
                    {
                      "instanceId": "offline-engine",
                      "leaseId": "offline-lease",
                      "status": "OFFLINE",
                      "expireAt": "2026-07-27T11:01:00Z"
                    },
                    {
                      "instanceId": "expired-engine",
                      "leaseId": "expired-lease",
                      "status": "ONLINE",
                      "expireAt": "2026-07-27T10:59:59Z"
                    }
                  ]
                }
                """);

        assertThat(GatewayLiveTopologyIT.activeEngineLeases(
                projection,
                Instant.parse("2026-07-27T11:00:00Z")
        )).containsOnlyKeys("online-engine")
                .containsEntry("online-engine", "online-lease");
    }
}
