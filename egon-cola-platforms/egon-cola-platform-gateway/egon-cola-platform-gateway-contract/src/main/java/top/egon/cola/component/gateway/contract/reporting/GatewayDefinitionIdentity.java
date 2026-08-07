package top.egon.cola.component.gateway.contract.reporting;

/**
 * 接口定义报告对应的构建身份和内容指纹。
 *
 * <p>与 definition 包的轻量构建身份不同，本类型包含完整报告指纹，用于服务端判断报告是否
 * 已处理以及是否可以安全替换同一组定义。
 */
public record GatewayDefinitionIdentity(
        String definitionSetId,
        String definitionFingerprint,
        String artifactVersion,
        String buildId
) {

    public GatewayDefinitionIdentity {
        definitionSetId = required(definitionSetId, "definitionSetId");
        definitionFingerprint = required(
                definitionFingerprint,
                "definitionFingerprint"
        );
        artifactVersion = required(artifactVersion, "artifactVersion");
        buildId = required(buildId, "buildId");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
