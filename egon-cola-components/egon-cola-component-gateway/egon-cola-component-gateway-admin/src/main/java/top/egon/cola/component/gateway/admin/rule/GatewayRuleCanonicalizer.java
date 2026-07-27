package top.egon.cola.component.gateway.admin.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeParameter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class GatewayRuleCanonicalizer {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .addMixIn(GatewayRuntimeOperation.class, OperationWireShape.class)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    /**
     * Drops an empty parameter list from the wire form, so that publishing an
     * operation without parameters produces the bytes the engine already
     * checksums. Must mirror the mix-in in {@code GatewayRuleJsonCodec}.
     */
    private abstract static class OperationWireShape {

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public abstract List<GatewayRuntimeParameter> parameters();
    }

    public byte[] canonicalBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway rule cannot be serialized",
                    failure
            );
        }
    }

    public GatewayRuleSnapshot snapshot(
            String releaseId,
            Instant generatedAt,
            GatewayRuleContent content) {
        byte[] contentBytes = canonicalBytes(content);
        String contentSha = sha256(contentBytes);
        Map<String, Object> material = Map.of(
                "content", content,
                "generatedAt", generatedAt,
                "releaseId", releaseId,
                "ruleContentSha256", contentSha,
                "ruleSchemaVersion", "v1"
        );
        String artifactSha = sha256(canonicalBytes(material));
        return new GatewayRuleSnapshot(
                "v1",
                releaseId,
                generatedAt,
                contentSha,
                artifactSha,
                content
        );
    }

    public void verify(GatewayRuleSnapshot snapshot) {
        String contentSha = sha256(canonicalBytes(snapshot.content()));
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
        if (!sha256(canonicalBytes(material))
                .equals(snapshot.artifactSha256())) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHECKSUM_MISMATCH: artifact"
            );
        }
    }

    public String json(Object value) {
        return new String(canonicalBytes(value), StandardCharsets.UTF_8);
    }

    public ObjectMapper objectMapper() {
        return objectMapper.copy();
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
}
