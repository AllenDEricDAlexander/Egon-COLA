package top.egon.cola.component.gateway.test.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayLiveTopologyEngineLeaseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
