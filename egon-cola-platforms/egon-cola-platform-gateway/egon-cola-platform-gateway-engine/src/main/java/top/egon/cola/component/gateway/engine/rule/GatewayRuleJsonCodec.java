package top.egon.cola.component.gateway.engine.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeParameter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class GatewayRuleJsonCodec {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .addMixIn(GatewayRuntimeOperation.class, OperationWireShape.class)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    /**
     * Drops an empty parameter list from the wire form, so that an operation
     * without parameters serializes exactly as it did before the component
     * existed and still matches its published {@code ruleContentSha256}.
     *
     * <p>The contract module is serializer-free by design, so this lives with
     * the mapper. {@code GatewayRuleCanonicalizer} on the publishing side
     * declares the same mix-in: the two configurations must stay in step or
     * published and recomputed checksums diverge.
     */
    private abstract static class OperationWireShape {

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public abstract List<GatewayRuntimeParameter> parameters();
    }

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
        String contentSha = sha256(write(snapshot.content()));
        if (!contentSha.equals(snapshot.ruleContentSha256())) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHECKSUM_MISMATCH: content"
            );
        }
        Map<String, Object> material = Map.of(
                "content", snapshot.content(),
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
