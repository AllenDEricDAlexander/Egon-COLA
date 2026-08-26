package top.egon.cola.component.gateway.admin.openapi.domain.vo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only OpenAPI fragment associated with the current Gateway operation.
 *
 * <p>中文：Fragment 从不可变 Snapshot 中提取；它不允许调用方覆盖 Group、路径
 * 或 Provider 地址。</p>
 */
public record GatewayOperationOpenApiVO(
        String operationId,
        String operationKey,
        String snapshotId,
        String openapiVersion,
        String openapiGroup,
        String path,
        String method,
        String openapiOperationId,
        List<String> requestContentTypes,
        List<String> responseContentTypes,
        Map<String, Object> operation,
        Instant syncedAt
) {

    /** Normalizes identifiers and freezes the raw fragment collections. */
    public GatewayOperationOpenApiVO {
        operationId = required(operationId, "operationId", 64);
        operationKey = required(operationKey, "operationKey", 512);
        snapshotId = required(snapshotId, "snapshotId", 64);
        openapiVersion = required(openapiVersion, "openapiVersion", 32);
        openapiGroup = required(openapiGroup, "openapiGroup", 64);
        path = required(path, "path", 2048);
        method = required(method, "method", 16).toUpperCase(java.util.Locale.ROOT);
        openapiOperationId = required(
                openapiOperationId, "openapiOperationId", 256);
        requestContentTypes = sortedStrings(
                requestContentTypes, "requestContentTypes");
        responseContentTypes = sortedStrings(
                responseContentTypes, "responseContentTypes");
        operation = immutableObject(operation, "operation");
        syncedAt = Objects.requireNonNull(syncedAt, "syncedAt");
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

    private static List<String> sortedStrings(
            List<String> values,
            String field) {
        Objects.requireNonNull(values, field);
        List<String> normalized = new ArrayList<>(values.size());
        for (String value : values) {
            normalized.add(required(value, field + " entry", 256));
        }
        Collections.sort(normalized);
        return List.copyOf(normalized);
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
                    Objects.requireNonNull(key, "operation key").toString(),
                    freeze(item)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(GatewayOperationOpenApiVO::freeze).toList();
        }
        return value;
    }
}
