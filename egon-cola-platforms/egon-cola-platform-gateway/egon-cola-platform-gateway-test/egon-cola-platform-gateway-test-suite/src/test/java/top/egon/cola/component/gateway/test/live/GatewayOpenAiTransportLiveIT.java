package top.egon.cola.component.gateway.test.live;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenAiTransportLiveIT {

    @Test
    @EnabledIfSystemProperty(
            named = "gateway.live.openai.test",
            matches = "true"
    )
    void createsManualRealtimeOperationAndProxiesWebSocketFrames()
            throws Exception {
        String runId = System.getProperty(
                "gateway.live.openai.run-id",
                Long.toString(System.currentTimeMillis())
        );
        String applicationId = required("gateway.live.openai.application-id");
        String gatewayGroupId = required(
                "gateway.live.openai.gateway-group-id"
        );
        URI gatewayUri = URI.create(required(
                "gateway.live.openai.websocket-uri"
        ));
        GatewayAdminTestClient admin = new GatewayAdminTestClient(
                URI.create(required("gateway.live.openai.admin-base-uri")),
                required("gateway.live.openai.admin-token")
        );

        JsonNode interfaceGroup = admin.createManualInterfaceGroup(
                applicationId,
                Map.of(
                        "businessCode", "gateway-live",
                        "businessName", "Gateway Live",
                        "entityCode", "openai-realtime-" + runId,
                        "entityName", "OpenAI Realtime",
                        "interfaceGroupCode", "openai-realtime-" + runId,
                        "interfaceGroupName", "OpenAI Realtime " + runId,
                        "description", "Opt-in OpenAI WebSocket live test"
                )
        );
        JsonNode operation = admin.createManualOperation(
                interfaceGroup.required("id").asText(),
                Map.ofEntries(
                        Map.entry("protocol", "HTTP"),
                        Map.entry("httpMethod", "GET"),
                        Map.entry("path", "/test/transport/realtime"),
                        Map.entry(
                                "providerServiceName",
                                System.getProperty(
                                        "gateway.live.openai.provider-service",
                                        "gateway-test-http-provider"
                                )
                        ),
                        Map.entry("group", "default"),
                        Map.entry(
                                "version",
                                System.getProperty(
                                        "gateway.live.openai.provider-version",
                                        "1.0.0-live"
                                )
                        ),
                        Map.entry("transport", "http"),
                        Map.entry("externalAccessible", true),
                        Map.entry("definition", Map.of(
                                "summary", "OpenAI Realtime WebSocket",
                                "tags", List.of("openai", "websocket"),
                                "requestSchema", Map.of(),
                                "responseSchema", Map.of(),
                                "errorSchema", List.of(),
                                "attributes", Map.of(
                                        "responseMode", "TRANSPARENT"
                                ),
                                "externalAccessible", true
                        ))
                )
        );
        long revision = admin.getDraft(gatewayGroupId)
                .required("revision")
                .asLong();
        JsonNode mutation = admin.putRoute(
                gatewayGroupId,
                "openai-realtime-" + runId,
                Map.of(
                        "operationId",
                        operation.required("operation")
                                .required("id").asText(),
                        "content", Map.of(
                                "host", gatewayUri.getHost(),
                                "httpMethod", "GET",
                                "pathPattern", gatewayUri.getPath(),
                                "accessZones", List.of("PUBLIC"),
                                "priority", 0,
                                "transportPolicy", Map.of(
                                        "profile", "OPENAI_HTTP",
                                        "transportProtocol", "WEBSOCKET",
                                        "websocketIdleTimeoutMs", 300_000,
                                        "websocketMaxFrameBytes", 16_777_216,
                                        "bodyLogEnabled", false,
                                        "retryEnabled", false
                                )
                        ),
                        "enabled", true,
                        "expectedRevision", revision,
                        "idempotencyKey", "openai-realtime-" + runId,
                        "changeReason", "OpenAI transport live test"
                )
        );
        revision = mutation.required("revision").asLong();
        JsonNode validation = admin.validateDraft(gatewayGroupId);
        assertThat(validation.required("valid").asBoolean())
                .as("OpenAI live draft validation: %s", validation)
                .isTrue();
        JsonNode release = admin.release(
                gatewayGroupId,
                Map.of(
                        "expectedDraftRevision", revision,
                        "changeReason", "OpenAI transport live release"
                )
        );
        assertThat(release.required("status").asText())
                .as("OpenAI live release: %s", release)
                .isEqualTo("SUCCESS");

        byte[] binary = new byte[] {0x00, (byte) 0xff, 0x31};
        byte[] ping = new byte[] {0x01, 0x02};
        GatewayWebSocketTestClient.Transcript transcript =
                new GatewayWebSocketTestClient().exchange(
                        gatewayUri,
                        Map.of(),
                        "openai-realtime",
                        binary,
                        ping,
                        Duration.ofSeconds(10)
                );

        assertThat(transcript.textFrames()).contains("openai-realtime");
        assertThat(transcript.binaryFrames())
                .anyMatch(frame -> Arrays.equals(frame, binary));
        assertThat(transcript.pongFrames())
                .anyMatch(frame -> Arrays.equals(frame, ping));
        assertThat(transcript.closeCode()).isEqualTo(1000);
    }

    private String required(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "System property " + name + " is required"
            );
        }
        return value.trim();
    }
}
