package top.egon.cola.component.gateway.contract.reporting.openapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

/**
 * The bounded, normalized OpenAPI group membership contract published through
 * DDC and consumed by Gateway Admin. It deliberately contains locations and
 * build identity only; it never carries a document or credential.
 */
public record GatewayOpenApiGroupManifestDTO(
        @NotNull
        @Size(min = 1, max = MAX_GROUPS)
        List<@NotBlank @Pattern(regexp = GROUP_PATTERN) String> groups,
        @NotBlank String pathTemplate,
        @NotBlank String resourceUri,
        @NotBlank @Size(max = MAX_ARTIFACT_VERSION_LENGTH)
        String artifactVersion,
        @NotBlank @Size(max = MAX_BUILD_ID_LENGTH)
        String buildId
) {

    public static final String PATH_TEMPLATE = "/v3/api-docs/{group}";

    public static final String GROUP_PATTERN = "[a-z][a-z0-9-]{0,63}";

    public static final int MAX_GROUPS = 16;

    public static final int MAX_GROUP_CSV_BYTES = 512;

    public static final int MAX_ARTIFACT_VERSION_LENGTH = 128;

    public static final int MAX_BUILD_ID_LENGTH = 256;

    public GatewayOpenApiGroupManifestDTO {
        groups = normalizeGroups(groups);
        pathTemplate = required(pathTemplate, "pathTemplate");
        if (!PATH_TEMPLATE.equals(pathTemplate)) {
            throw new IllegalArgumentException(
                    "pathTemplate must equal " + PATH_TEMPLATE
            );
        }
        resourceUri = normalizeResourceUri(resourceUri);
        artifactVersion = bounded(
                artifactVersion,
                "artifactVersion",
                MAX_ARTIFACT_VERSION_LENGTH
        );
        buildId = bounded(buildId, "buildId", MAX_BUILD_ID_LENGTH);

        int csvBytes = String.join(",", groups)
                .getBytes(StandardCharsets.UTF_8)
                .length;
        if (csvBytes > MAX_GROUP_CSV_BYTES) {
            throw new IllegalArgumentException(
                    "groups CSV exceeds " + MAX_GROUP_CSV_BYTES + " bytes"
            );
        }
    }

    private static List<String> normalizeGroups(List<String> values) {
        if (values == null || values.isEmpty() || values.size() > MAX_GROUPS) {
            throw new IllegalArgumentException(
                    "groups must contain between 1 and " + MAX_GROUPS
                            + " values"
            );
        }

        List<String> normalized = values.stream()
                .map(value -> required(value, "group"))
                .peek(value -> {
                    if (!value.matches(GROUP_PATTERN)) {
                        throw new IllegalArgumentException(
                                "invalid OpenAPI group code: " + value
                        );
                    }
                })
                .sorted(Comparator.naturalOrder())
                .toList();
        if (normalized.stream().distinct().count() != normalized.size()) {
            throw new IllegalArgumentException("groups must be unique");
        }
        return List.copyOf(normalized);
    }

    private static String normalizeResourceUri(String value) {
        String normalized = required(value, "resourceUri");
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "resourceUri must be a valid absolute URI",
                    exception
            );
        }
        if (!uri.isAbsolute()
                || !"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "resourceUri must be an absolute HTTPS URI without credentials or query"
            );
        }
        return uri.normalize().toString();
    }

    private static String bounded(String value, String field, int maxLength) {
        String normalized = required(value, field);
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " exceeds " + maxLength + " characters"
            );
        }
        return normalized;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
