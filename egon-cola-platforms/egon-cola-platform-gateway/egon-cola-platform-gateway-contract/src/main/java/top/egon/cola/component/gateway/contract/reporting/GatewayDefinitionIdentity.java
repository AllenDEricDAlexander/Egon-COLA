package top.egon.cola.component.gateway.contract.reporting;

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
