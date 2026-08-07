package top.egon.cola.component.gateway.contract.rule;

/**
 * 一段分片规则内容在配置存储中的引用及完整性校验信息。
 */
public record GatewayRuleChunkRef(
        String configKey,
        int index,
        int size,
        String sha256
) {

    public GatewayRuleChunkRef {
        if (configKey == null || configKey.isBlank()) {
            throw new IllegalArgumentException("configKey is required");
        }
        if (index < 0 || size < 1) {
            throw new IllegalArgumentException("invalid chunk index or size");
        }
        if (sha256 == null || sha256.isBlank()) {
            throw new IllegalArgumentException("sha256 is required");
        }
    }
}
