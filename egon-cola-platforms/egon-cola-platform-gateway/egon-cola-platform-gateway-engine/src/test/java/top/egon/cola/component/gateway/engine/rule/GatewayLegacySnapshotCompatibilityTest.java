package top.egon.cola.component.gateway.engine.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayLegacySnapshotCompatibilityTest {

    @Test
    void snapshotWithoutMcpFieldLoadsAndVerifiesAsEmptyMcpRules() {
        GatewayRuleJsonCodec codec = new GatewayRuleJsonCodec();
        Map<String, Object> content = legacyContent();
        Instant generatedAt = Instant.parse("2026-07-25T00:00:00Z");
        String contentSha = GatewayRuleJsonCodec.sha256(
                codec.write(content)
        );
        Map<String, Object> material = new LinkedHashMap<>();
        material.put("content", content);
        material.put("generatedAt", generatedAt);
        material.put("releaseId", "legacy-release");
        material.put("ruleContentSha256", contentSha);
        material.put("ruleSchemaVersion", "v1");
        String artifactSha = GatewayRuleJsonCodec.sha256(
                codec.write(material)
        );
        Map<String, Object> snapshotJson = new LinkedHashMap<>();
        snapshotJson.put("artifactSha256", artifactSha);
        snapshotJson.put("content", content);
        snapshotJson.put("generatedAt", generatedAt);
        snapshotJson.put("releaseId", "legacy-release");
        snapshotJson.put("ruleContentSha256", contentSha);
        snapshotJson.put("ruleSchemaVersion", "v1");

        GatewayRuleSnapshot snapshot = codec.readSnapshot(
                codec.write(snapshotJson)
        );

        assertTrue(snapshot.content().mcp().servers().isEmpty());
        codec.verify(snapshot);
    }

    private Map<String, Object> legacyContent() {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("corsPolicies", List.of());
        content.put("env", "local");
        content.put("gatewayGroupCode", "orders");
        content.put("gatewayGroupId", "group-1");
        content.put("namespace", "default");
        content.put("operations", List.of());
        content.put("providerPolicies", List.of());
        content.put("routes", List.of());
        content.put("rpcDescriptors", List.of());
        content.put("securityPolicies", List.of());
        content.put("trafficPolicies", List.of());
        return Map.copyOf(content);
    }
}
