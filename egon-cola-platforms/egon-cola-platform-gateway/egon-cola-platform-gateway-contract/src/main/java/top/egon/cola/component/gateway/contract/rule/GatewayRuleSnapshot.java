package top.egon.cola.component.gateway.contract.rule;

import java.time.Instant;
import java.util.Objects;

/**
 * 已生成的不可变 Gateway 规则快照。
 *
 * <p>快照把发布版本、生成时间、内容摘要和规则内容绑定在一起，Engine 加载后以此作为运行时
 * 配置的单一事实来源。
 */
public record GatewayRuleSnapshot(
        String ruleSchemaVersion,
        String releaseId,
        Instant generatedAt,
        String ruleContentSha256,
        String artifactSha256,
        GatewayRuleContent content
) {

    public GatewayRuleSnapshot {
        ruleSchemaVersion = required(
                ruleSchemaVersion,
                "ruleSchemaVersion"
        );
        releaseId = required(releaseId, "releaseId");
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
        ruleContentSha256 = required(
                ruleContentSha256,
                "ruleContentSha256"
        );
        artifactSha256 = required(artifactSha256, "artifactSha256");
        content = Objects.requireNonNull(content, "content");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
