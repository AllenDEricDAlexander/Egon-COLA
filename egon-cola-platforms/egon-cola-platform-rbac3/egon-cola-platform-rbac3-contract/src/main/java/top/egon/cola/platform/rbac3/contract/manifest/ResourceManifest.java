package top.egon.cola.platform.rbac3.contract.manifest;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ResourceManifest(
        String schemaVersion,
        String applicationCode,
        String applicationName,
        String artifactVersion,
        String buildId,
        long manifestVersion,
        Instant generatedAt,
        String checksum,
        List<ManifestResource> apps,
        List<ManifestResource> menus,
        List<ManifestResource> routes,
        List<ManifestResource> actions,
        List<ManifestResource> apis,
        List<FieldDefinition> fieldDefinitions
) {

    public ResourceManifest {
        schemaVersion = required(schemaVersion, "schemaVersion");
        applicationCode = required(applicationCode, "applicationCode");
        applicationName = required(applicationName, "applicationName");
        artifactVersion = required(artifactVersion, "artifactVersion");
        buildId = required(buildId, "buildId");
        nonNegative(manifestVersion, "manifestVersion");
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
        checksum = required(checksum, "checksum");
        apps = immutable(apps, "apps");
        menus = immutable(menus, "menus");
        routes = immutable(routes, "routes");
        actions = immutable(actions, "actions");
        apis = immutable(apis, "apis");
        fieldDefinitions = List.copyOf(Objects.requireNonNull(
                fieldDefinitions,
                "fieldDefinitions"
        ));
    }

    public record FieldDefinition(
            String resourceCode,
            String fieldCode,
            String dataType,
            String sensitivity,
            String defaultAccess,
            String maskingStrategy,
            boolean writable,
            boolean exportable
    ) {

        public FieldDefinition {
            resourceCode = required(resourceCode, "resourceCode");
            fieldCode = required(fieldCode, "fieldCode");
            dataType = required(dataType, "dataType");
            sensitivity = required(sensitivity, "sensitivity");
            defaultAccess = required(defaultAccess, "defaultAccess");
            maskingStrategy = optional(
                    maskingStrategy,
                    "maskingStrategy"
            );
        }
    }

    private static List<ManifestResource> immutable(
            List<ManifestResource> values,
            String fieldName) {
        return List.copyOf(Objects.requireNonNull(values, fieldName));
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String optional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
        return value.trim();
    }

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
