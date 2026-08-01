package top.egon.cola.component.gateway.engine.rule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeParameter;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the wire compatibility of the {@code parameters} component added to
 * {@link GatewayRuntimeOperation}: an engine on this code must still read and
 * checksum a rule snapshot that was published before the component existed.
 */
class GatewayRuleParameterCompatibilityTest {

    private static final Instant GENERATED_AT =
            Instant.parse("2026-07-25T00:00:00Z");

    private final GatewayRuleJsonCodec codec = new GatewayRuleJsonCodec();

    @Test
    void snapshotPublishedWithoutParametersStillDeserialises() {
        GatewayRuleSnapshot snapshot = codec.readSnapshot(
                legacySnapshotJson().getBytes(StandardCharsets.UTF_8)
        );

        GatewayRuntimeOperation operation =
                snapshot.content().operations().getFirst();
        assertEquals(List.of(), operation.parameters());
        assertEquals("orders", operation.operationId());
        assertEquals("GET /orders", operation.methodIdentity());
    }

    @Test
    void operationWithoutParametersKeepsItsPreviousWireShape() {
        String json = new String(
                codec.write(content(operation(List.of()))),
                StandardCharsets.UTF_8
        );

        assertFalse(json.contains("\"parameters\""));
    }

    /**
     * Reading is not enough: the engine re-serializes a snapshot and compares
     * it against the checksums the publisher stored. This replays the legacy
     * artifact as untyped JSON so the checksums are the ones the previous code
     * would have produced, then feeds it through the typed path.
     */
    @Test
    void legacySnapshotStillMatchesItsPublishedChecksums() throws Exception {
        Map<String, Object> legacy = new ObjectMapper().readValue(
                legacySnapshotJson(),
                new TypeReference<>() {
                }
        );
        Object content = legacy.get("content");
        String contentSha = GatewayRuleJsonCodec.sha256(codec.write(content));
        legacy.put("ruleContentSha256", contentSha);
        legacy.put("artifactSha256", GatewayRuleJsonCodec.sha256(
                codec.write(Map.of(
                        "content", content,
                        "generatedAt", legacy.get("generatedAt"),
                        "releaseId", legacy.get("releaseId"),
                        "ruleContentSha256", contentSha,
                        "ruleSchemaVersion", legacy.get("ruleSchemaVersion")
                ))
        ));

        GatewayRuleSnapshot snapshot = codec.readSnapshot(codec.write(legacy));

        assertDoesNotThrow(() -> codec.verify(snapshot));
    }

    @Test
    void parametersSurviveARoundTripAndAreCanonicallyOrdered() {
        GatewayRuntimeOperation operation = operation(List.of(
                new GatewayRuntimeParameter(
                        "size",
                        "query",
                        false,
                        "java.lang.Integer",
                        "20",
                        "page size"
                ),
                new GatewayRuntimeParameter(
                        "orderId",
                        "PATH",
                        true,
                        "java.lang.String",
                        null,
                        null
                )
        ));
        GatewayRuleSnapshot snapshot = snapshot(content(operation));

        GatewayRuleSnapshot decoded = codec.readSnapshot(
                codec.write(snapshot)
        );

        List<GatewayRuntimeParameter> parameters =
                decoded.content().operations().getFirst().parameters();
        assertEquals(
                List.of("orderId", "size"),
                parameters.stream().map(GatewayRuntimeParameter::name).toList()
        );
        GatewayRuntimeParameter path = parameters.getFirst();
        assertEquals("PATH", path.location());
        assertTrue(path.required());
        assertEquals("page size", parameters.getLast().description());
        assertEquals("QUERY", parameters.getLast().location());
        assertDoesNotThrow(() -> codec.verify(decoded));
    }

    private GatewayRuleSnapshot snapshot(GatewayRuleContent content) {
        String contentSha = GatewayRuleJsonCodec.sha256(codec.write(content));
        String artifactSha = GatewayRuleJsonCodec.sha256(codec.write(Map.of(
                "content", content,
                "generatedAt", GENERATED_AT,
                "releaseId", "release-1",
                "ruleContentSha256", contentSha,
                "ruleSchemaVersion", "v1"
        )));
        return new GatewayRuleSnapshot(
                "v1",
                "release-1",
                GENERATED_AT,
                contentSha,
                artifactSha,
                content
        );
    }

    private GatewayRuleContent content(GatewayRuntimeOperation operation) {
        return new GatewayRuleContent(
                "group-1",
                "orders",
                "local",
                "default",
                List.of(operation),
                List.of(new GatewayRuntimeRoute(
                        "orders",
                        "orders",
                        "api.example.com",
                        "GET",
                        "/orders",
                        Set.of(AccessZone.PUBLIC),
                        0,
                        true
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private GatewayRuntimeOperation operation(
            List<GatewayRuntimeParameter> parameters) {
        return new GatewayRuntimeOperation(
                "orders",
                "orders",
                GatewayProtocol.HTTP,
                "GET /orders",
                "{}",
                "{}",
                parameters,
                true,
                new GatewayProviderServiceRef(
                        "test-biz",
                        "test-app",
                        "local",
                        "default",
                        GatewayProtocol.HTTP,
                        "orders",
                        "default",
                        "v1",
                        "http"
                ),
                "TRANSPARENT",
                Set.of(),
                Map.of(),
                false
        );
    }

    /**
     * Literal snapshot as the previous code published it: the operation has no
     * {@code parameters} key at all.
     */
    private String legacySnapshotJson() {
        return """
                {
                  "artifactSha256": "legacy-artifact-sha",
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
                  "ruleContentSha256": "legacy-content-sha",
                  "ruleSchemaVersion": "v1"
                }
                """;
    }
}
