package top.egon.cola.component.gateway.contract.rule;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 告知 Engine 如何读取某次发布规则的激活元数据。
 *
 * <p>规则体较小时内联传输，较大时通过有序分片引用读取；两种模式互斥。
 */
public record GatewayRuleActivation(
        String activationSchemaVersion,
        String releaseId,
        GatewayRuleActivationMode mode,
        String ruleSchemaVersion,
        int totalSize,
        String ruleContentSha256,
        String artifactSha256,
        String inlineSnapshot,
        List<GatewayRuleChunkRef> chunks
) {

    public GatewayRuleActivation {
        activationSchemaVersion = required(
                activationSchemaVersion,
                "activationSchemaVersion"
        );
        releaseId = required(releaseId, "releaseId");
        mode = Objects.requireNonNull(mode, "mode");
        ruleSchemaVersion = required(
                ruleSchemaVersion,
                "ruleSchemaVersion"
        );
        if (totalSize < 1) {
            throw new IllegalArgumentException("totalSize must be positive");
        }
        ruleContentSha256 = required(
                ruleContentSha256,
                "ruleContentSha256"
        );
        artifactSha256 = required(artifactSha256, "artifactSha256");
        chunks = Objects.requireNonNull(chunks, "chunks").stream()
                .sorted(Comparator.comparingInt(GatewayRuleChunkRef::index))
                .toList();
        if (mode == GatewayRuleActivationMode.INLINE
                && (inlineSnapshot == null || inlineSnapshot.isBlank())) {
            throw new IllegalArgumentException(
                    "INLINE activation requires inlineSnapshot"
            );
        }
        if (mode == GatewayRuleActivationMode.INLINE && !chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "INLINE activation must not contain chunks"
            );
        }
        if (mode == GatewayRuleActivationMode.CHUNKED
                && (inlineSnapshot != null || chunks.isEmpty())) {
            throw new IllegalArgumentException(
                    "CHUNKED activation requires chunks only"
            );
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
