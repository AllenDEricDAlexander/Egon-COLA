package top.egon.cola.component.gateway.engine.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public final class GatewayRuleJsonCodec {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    public GatewayRuleActivation readActivation(String json) {
        return read(json.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                GatewayRuleActivation.class);
    }

    public GatewayRuleSnapshot readSnapshot(byte[] json) {
        return read(json, GatewayRuleSnapshot.class);
    }

    public byte[] write(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway rule cannot be serialized",
                    failure
            );
        }
    }

    public void verify(GatewayRuleSnapshot snapshot) {
        if (!"v1".equals(snapshot.ruleSchemaVersion())) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_SCHEMA_UNSUPPORTED"
            );
        }
        Object checksumContent = checksumContent(snapshot);
        Map<String, Object> material = Map.of(
                "content", checksumContent,
                "generatedAt", snapshot.generatedAt(),
                "releaseId", snapshot.releaseId(),
                "ruleContentSha256", snapshot.ruleContentSha256(),
                "ruleSchemaVersion", snapshot.ruleSchemaVersion()
        );
        if (!sha256(write(material)).equals(snapshot.artifactSha256())) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHECKSUM_MISMATCH: artifact"
            );
        }
    }

    private Object checksumContent(GatewayRuleSnapshot snapshot) {
        String expected = snapshot.ruleContentSha256();
        if (sha256(write(snapshot.content())).equals(expected)) {
            return snapshot.content();
        }
        GatewayRuleContent content = snapshot.content();
        Map<String, Object> legacy = legacyContent(content);
        if (content.mcp().equals(McpRuleContent.empty())
                && sha256(write(legacy)).equals(expected)) {
            return legacy;
        }
        throw new IllegalArgumentException(
                "GATEWAY_RULE_CHECKSUM_MISMATCH: content"
        );
    }

    private Map<String, Object> legacyContent(GatewayRuleContent content) {
        return Map.ofEntries(
                Map.entry("gatewayGroupId", content.gatewayGroupId()),
                Map.entry("gatewayGroupCode", content.gatewayGroupCode()),
                Map.entry("env", content.env()),
                Map.entry("namespace", content.namespace()),
                Map.entry("operations", content.operations()),
                Map.entry("routes", content.routes()),
                Map.entry("providerPolicies", content.providerPolicies()),
                Map.entry("trafficPolicies", content.trafficPolicies()),
                Map.entry("securityPolicies", content.securityPolicies()),
                Map.entry("corsPolicies", content.corsPolicies()),
                Map.entry("rpcDescriptors", content.rpcDescriptors())
        );
    }

    public static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private <T> T read(byte[] json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception failure) {
            throw new IllegalArgumentException(
                    "gateway rule JSON is invalid",
                    failure
            );
        }
    }
}
