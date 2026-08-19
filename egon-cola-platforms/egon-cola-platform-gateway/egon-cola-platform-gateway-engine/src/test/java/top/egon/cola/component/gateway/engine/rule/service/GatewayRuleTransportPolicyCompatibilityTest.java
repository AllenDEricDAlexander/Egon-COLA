package top.egon.cola.component.gateway.engine.rule.service;

import top.egon.cola.component.gateway.engine.rule.adapter.json.GatewayRuleJsonCodec;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class GatewayRuleTransportPolicyCompatibilityTest {

    private final GatewayRuleJsonCodec codec = new GatewayRuleJsonCodec();

    @Test
    void legacySnapshotOmitsTransportPolicyAndKeepsPublishedChecksums() {
        GatewayRuleSnapshot snapshot = codec.readSnapshot(
                legacySnapshotJson().getBytes(StandardCharsets.UTF_8)
        );

        assertNull(snapshot.content().routes().getFirst().transportPolicy());
        assertFalse(new String(
                codec.write(snapshot.content()),
                StandardCharsets.UTF_8
        ).contains("\"transportPolicy\""));
        assertEquals(
                "161d861a8cb13996f93700f8336ffe74827579237364970192e61da70d542cda",
                snapshot.ruleContentSha256()
        );
        assertEquals(
                "b6ade90ed4a74109833620b24b9a8e40f3f674323d722d950815abbe5b93af1b",
                snapshot.artifactSha256()
        );
        assertDoesNotThrow(() -> codec.verify(snapshot));
    }

    @Test
    void newTransportPolicyUsesStableCanonicalPropertyOrder() {
        GatewayRouteTransportPolicy policy = new GatewayRouteTransportPolicy(
                GatewayRouteProfile.OPENAI_HTTP,
                GatewayTransportProtocol.HTTP,
                GatewayRequestBodyMode.STREAMING,
                GatewayTransportResponseMode.AUTO_STREAM,
                536_870_912L,
                10_000L,
                120_000L,
                90_000L,
                1_800_000L,
                300_000L,
                16_777_216L,
                false,
                false
        );

        assertEquals(
                "{\"bodyLogEnabled\":false,\"connectTimeoutMs\":10000,"
                        + "\"maxRequestBodyBytes\":536870912,"
                        + "\"profile\":\"OPENAI_HTTP\","
                        + "\"requestBodyMode\":\"STREAMING\","
                        + "\"responseHeaderTimeoutMs\":120000,"
                        + "\"responseMode\":\"AUTO_STREAM\","
                        + "\"retryEnabled\":false,"
                        + "\"streamIdleTimeoutMs\":90000,"
                        + "\"totalTimeoutMs\":1800000,"
                        + "\"transportProtocol\":\"HTTP\","
                        + "\"websocketIdleTimeoutMs\":300000,"
                        + "\"websocketMaxFrameBytes\":16777216}",
                new String(codec.write(policy), StandardCharsets.UTF_8)
        );
    }

    private String legacySnapshotJson() {
        return """
                {
                  "artifactSha256": "b6ade90ed4a74109833620b24b9a8e40f3f674323d722d950815abbe5b93af1b",
                  "content": {
                    "corsPolicies": [],
                    "env": "local",
                    "gatewayGroupCode": "orders",
                    "gatewayGroupId": "group-1",
                    "namespace": "default",
                    "operations": [
                      {
                        "attributes": {"idempotent": "true"},
                        "deprecated": false,
                        "externalAccessible": true,
                        "methodIdentity": "GET /orders",
                        "operationId": "orders",
                        "operationKey": "orders",
                        "policyRefs": [],
                        "protocol": "HTTP",
                        "providerService": {
                          "env": "local",
                          "group": "default",
                          "namespace": "default",
                          "protocol": "HTTP",
                          "serviceName": "orders",
                          "transport": "http",
                          "version": "v1"
                        },
                        "requestSchema": "{}",
                        "responseMode": "TRANSPARENT",
                        "responseSchema": "{}"
                      }
                    ],
                    "providerPolicies": [],
                    "routes": [
                      {
                        "accessZones": ["PUBLIC"],
                        "enabled": true,
                        "host": "api.example.com",
                        "httpMethod": "GET",
                        "operationId": "orders",
                        "pathPattern": "/orders",
                        "priority": 0,
                        "routeId": "orders"
                      }
                    ],
                    "rpcDescriptors": [],
                    "securityPolicies": [],
                    "trafficPolicies": []
                  },
                  "generatedAt": "2026-07-25T00:00:00Z",
                  "releaseId": "release-1",
                  "ruleContentSha256": "161d861a8cb13996f93700f8336ffe74827579237364970192e61da70d542cda",
                  "ruleSchemaVersion": "v1"
                }
                """;
    }
}
