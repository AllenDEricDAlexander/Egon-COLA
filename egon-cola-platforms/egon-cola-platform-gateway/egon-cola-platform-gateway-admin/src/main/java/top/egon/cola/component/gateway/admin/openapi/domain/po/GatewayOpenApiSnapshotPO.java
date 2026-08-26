package top.egon.cola.component.gateway.admin.openapi.domain.po;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable persistence row for one fetched OpenAPI Group document.
 *
 * <p>中文：保存一个 OpenAPI Group 文档及其不可变校验结果；Definition Set 链接
 * 在 ingestion 事务中最多设置一次。
 */
public record GatewayOpenApiSnapshotPO(
        String id,
        String applicationId,
        String definitionSetId,
        String buildId,
        String artifactVersion,
        String openapiGroup,
        String openapiVersion,
        String documentSha256,
        String canonicalSha256,
        Map<String, Object> documentJson,
        String validationStatus,
        List<String> validationMessages,
        int operationCount,
        int schemaCount,
        String fetchedFromInstanceId,
        Instant fetchedAt,
        Instant validatedAt,
        Instant createdAt
) {

    private static final Pattern SHA256 = Pattern.compile(
            "^[0-9a-f]{64}$"
    );
    private static final Pattern GROUP = Pattern.compile(
            "^[a-z][a-z0-9-]{0,63}$"
    );

    public GatewayOpenApiSnapshotPO {
        id = required(id, "id", 64);
        applicationId = required(applicationId, "applicationId", 64);
        definitionSetId = optional(definitionSetId, "definitionSetId", 64);
        buildId = required(buildId, "buildId", 256);
        artifactVersion = required(artifactVersion, "artifactVersion", 128);
        openapiGroup = required(openapiGroup, "openapiGroup", 128);
        if (!GROUP.matcher(openapiGroup).matches()) {
            throw new IllegalArgumentException(
                    "openapiGroup must start with a lowercase letter and "
                            + "contain only lowercase letters, digits or hyphens"
            );
        }
        openapiVersion = required(openapiVersion, "openapiVersion", 32);
        if (!openapiVersion.startsWith("3.1.")) {
            throw new IllegalArgumentException(
                    "openapiVersion must start with 3.1."
            );
        }
        documentSha256 = hash(documentSha256, "documentSha256");
        canonicalSha256 = hash(canonicalSha256, "canonicalSha256");
        documentJson = immutableObject(documentJson, "documentJson");
        validationStatus = required(validationStatus, "validationStatus", 32);
        if (!Set.of("VALID", "INVALID").contains(validationStatus)) {
            throw new IllegalArgumentException(
                    "validationStatus must be VALID or INVALID"
            );
        }
        validationMessages = immutableMessages(validationMessages);
        if (operationCount < 0) {
            throw new IllegalArgumentException(
                    "operationCount must not be negative"
            );
        }
        if (schemaCount < 0) {
            throw new IllegalArgumentException(
                    "schemaCount must not be negative"
            );
        }
        fetchedFromInstanceId = required(
                fetchedFromInstanceId,
                "fetchedFromInstanceId",
                256
        );
        fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt");
        validatedAt = Objects.requireNonNull(validatedAt, "validatedAt");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " exceeds " + max + " characters"
            );
        }
        return normalized;
    }

    private static String optional(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return required(value, field, max);
    }

    private static String hash(String value, String field) {
        String normalized = required(value, field, 64);
        if (!SHA256.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    field + " must be lowercase hexadecimal SHA-256"
            );
        }
        return normalized;
    }

    private static Map<String, Object> immutableObject(
            Map<String, Object> value,
            String field) {
        Objects.requireNonNull(value, field);
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }

    private static List<String> immutableMessages(List<String> value) {
        Objects.requireNonNull(value, "validationMessages");
        List<String> copy = new ArrayList<>(value.size());
        for (String message : value) {
            copy.add(required(message, "validationMessages entry", 1024));
        }
        return Collections.unmodifiableList(copy);
    }

}
