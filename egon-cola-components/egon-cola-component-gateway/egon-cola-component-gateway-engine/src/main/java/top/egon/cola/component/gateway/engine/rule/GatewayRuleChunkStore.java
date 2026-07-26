package top.egon.cola.component.gateway.engine.rule;

import top.egon.cola.component.ddc.service.DdcConfigApplier;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleChunkRef;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GatewayRuleChunkStore implements DdcConfigApplier {

    private final Map<String, byte[]> chunks = new ConcurrentHashMap<>();

    @Override
    public void apply(String key, String value, long version) {
        if (!key.startsWith("gateway.rules.chunk.")) {
            throw new IllegalArgumentException("unexpected rule chunk key");
        }
        try {
            chunks.put(key, Base64.getDecoder().decode(value));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHUNK_CHECKSUM_MISMATCH",
                    invalid
            );
        }
    }

    public byte[] assemble(GatewayRuleActivation activation) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(
                activation.totalSize()
        );
        for (int expectedIndex = 0;
             expectedIndex < activation.chunks().size();
             expectedIndex++) {
            GatewayRuleChunkRef reference = activation.chunks()
                    .get(expectedIndex);
            if (reference.index() != expectedIndex) {
                throw new IllegalArgumentException(
                        "GATEWAY_RULE_CHUNK_MISSING: non-contiguous index"
                );
            }
            byte[] value = chunks.get(reference.configKey());
            if (value == null || value.length != reference.size()) {
                throw new IllegalArgumentException(
                        "GATEWAY_RULE_CHUNK_MISSING: "
                                + reference.configKey()
                );
            }
            if (!GatewayRuleJsonCodec.sha256(value)
                    .equals(reference.sha256())) {
                throw new IllegalArgumentException(
                        "GATEWAY_RULE_CHUNK_CHECKSUM_MISMATCH"
                );
            }
            output.writeBytes(value);
        }
        byte[] assembled = output.toByteArray();
        if (assembled.length != activation.totalSize()) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHUNK_MISSING: total size mismatch"
            );
        }
        return assembled;
    }

    public int size() {
        return chunks.size();
    }

    public int removeRelease(String releaseId) {
        if (releaseId == null || releaseId.isBlank()) {
            throw new IllegalArgumentException("releaseId is required");
        }
        String prefix = "gateway.rules.chunk." + releaseId + ".";
        int removed = 0;
        for (Map.Entry<String, byte[]> entry : chunks.entrySet()) {
            if (entry.getKey().startsWith(prefix)
                    && chunks.remove(entry.getKey(), entry.getValue())) {
                removed++;
            }
        }
        return removed;
    }
}
