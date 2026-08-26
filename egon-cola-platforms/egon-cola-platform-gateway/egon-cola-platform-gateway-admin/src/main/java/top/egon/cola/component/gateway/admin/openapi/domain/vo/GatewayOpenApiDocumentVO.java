package top.egon.cola.component.gateway.admin.openapi.domain.vo;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable OpenAPI snapshot response used for viewing or downloading JSON.
 *
 * <p>中文：Raw document 保存在响应中但不进入 {@link #toString()}，避免操作日志
 * 意外记录完整文档或其中的敏感扩展字段。</p>
 */
public record GatewayOpenApiDocumentVO(
        String snapshotId,
        String applicationId,
        String buildId,
        String openapiVersion,
        String documentSha256,
        String canonicalSha256,
        Map<String, Object> document,
        Instant fetchedAt,
        Instant validatedAt
) {

    private static final Pattern SHA256 = Pattern.compile(
            "^[0-9a-f]{64}$");

    /** Validates hashes and recursively freezes the raw JSON value. */
    public GatewayOpenApiDocumentVO {
        snapshotId = required(snapshotId, "snapshotId", 64);
        applicationId = required(applicationId, "applicationId", 64);
        buildId = required(buildId, "buildId", 256);
        openapiVersion = required(openapiVersion, "openapiVersion", 32);
        documentSha256 = hash(documentSha256, "documentSha256");
        canonicalSha256 = hash(canonicalSha256, "canonicalSha256");
        document = immutableObject(document, "document");
        fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt");
        validatedAt = Objects.requireNonNull(validatedAt, "validatedAt");
    }

    @Override
    public String toString() {
        return "GatewayOpenApiDocumentVO{" +
                "snapshotId='" + snapshotId + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", buildId='" + buildId + '\'' +
                ", openapiVersion='" + openapiVersion + '\'' +
                ", documentSha256='" + documentSha256 + '\'' +
                ", canonicalSha256='" + canonicalSha256 + '\'' +
                ", fetchedAt=" + fetchedAt +
                ", validatedAt=" + validatedAt +
                '}';
    }

    private static String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " exceeds " + max + " characters");
        }
        return normalized;
    }

    private static String hash(String value, String field) {
        String normalized = required(value, field, 64);
        if (!SHA256.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    field + " must be lowercase hexadecimal SHA-256");
        }
        return normalized;
    }

    private static Map<String, Object> immutableObject(
            Map<String, Object> value,
            String field) {
        Objects.requireNonNull(value, field);
        Map<String, Object> copy = new LinkedHashMap<>();
        value.forEach((key, item) -> copy.put(
                required(key, field + " key", 256), freeze(item)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object freeze(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(
                    Objects.requireNonNull(key, "document key").toString(),
                    freeze(item)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof java.util.List<?> list) {
            return list.stream().map(GatewayOpenApiDocumentVO::freeze).toList();
        }
        return value;
    }
}
