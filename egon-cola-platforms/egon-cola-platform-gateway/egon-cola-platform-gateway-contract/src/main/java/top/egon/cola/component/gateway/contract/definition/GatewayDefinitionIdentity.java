package top.egon.cola.component.gateway.contract.definition;

/**
 * 标识一次接口定义构建产物，用于定义上报的幂等处理和版本追踪。
 *
 * <p>该类型只描述构建身份，不包含业务接口树；接口树由 reporting 包中的报告类型承载。
 */
public record GatewayDefinitionIdentity(
        String definitionSetId,
        String artifactVersion,
        String buildId
) {

    public GatewayDefinitionIdentity {
        definitionSetId = required(definitionSetId, "definitionSetId");
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
