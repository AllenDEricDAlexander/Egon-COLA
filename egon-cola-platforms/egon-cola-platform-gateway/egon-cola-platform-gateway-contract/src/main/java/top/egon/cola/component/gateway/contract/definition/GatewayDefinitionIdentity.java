package top.egon.cola.component.gateway.contract.definition;

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
