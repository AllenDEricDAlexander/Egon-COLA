package top.egon.cola.component.yuheng.admin.openapi.domain.bo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code GatewayOpenApiSnapshotBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code GatewayOpenApiSnapshotBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class GatewayOpenApiSnapshotBO {

    private static final Pattern SHA256 = Pattern.compile(
            "^[0-9a-f]{64}$"
    );

    private static final Pattern GROUP = Pattern.compile(
            "^[a-z][a-z0-9-]{0,63}$"
    );

    private String id;
    private String applicationId;
    private String definitionSetId;
    private String buildId;
    private String artifactVersion;
    private String openapiGroup;
    private String openapiVersion;
    private String documentSha256;
    private String canonicalSha256;
    private Map<String, Object> documentJson;
    private String validationStatus;
    private List<String> validationMessages;
    private int operationCount;
    private int schemaCount;
    private String fetchedFromInstanceId;
    private Instant fetchedAt;
    private Instant validatedAt;
    private Instant createdAt;

    /**
     * 中文说明：按原持久化载体的构造契约逐项校验并规范化 {@code snapshot} 的业务字段，失败时抛出与原实现一致的 {@code IllegalArgumentException} 或 {@code NullPointerException}。
     * English summary: Validates and normalizes each business field of {@code snapshot} against the construction contract of the legacy carrier, raising the same {@code IllegalArgumentException} or {@code NullPointerException} the original implementation raised.
     *
     * 用法 / Usage: 由 {@code JdbcGatewayOpenApiSnapshotRepository} 的写入与装载边界调用；/ Call it from the write and load boundaries of {@code JdbcGatewayOpenApiSnapshotRepository}.
     * @param snapshot 参数 快照；parameter snapshot。
     * @return 返回规范化后的同一载体；returns the same carrier after normalization.
     */
    public static GatewayOpenApiSnapshotBO validated(
            GatewayOpenApiSnapshotBO snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        snapshot.setId(required(snapshot.getId(), "id", 64));
        snapshot.setApplicationId(required(
                snapshot.getApplicationId(),
                "applicationId",
                64
        ));
        snapshot.setDefinitionSetId(optional(
                snapshot.getDefinitionSetId(),
                "definitionSetId",
                64
        ));
        snapshot.setBuildId(required(
                snapshot.getBuildId(),
                "buildId",
                256
        ));
        snapshot.setArtifactVersion(required(
                snapshot.getArtifactVersion(),
                "artifactVersion",
                128
        ));
        snapshot.setOpenapiGroup(required(
                snapshot.getOpenapiGroup(),
                "openapiGroup",
                128
        ));
        if (!GROUP.matcher(snapshot.getOpenapiGroup()).matches()) {
            throw new IllegalArgumentException(
                    "openapiGroup must start with a lowercase letter and "
                            + "contain only lowercase letters, digits or hyphens"
            );
        }
        snapshot.setOpenapiVersion(required(
                snapshot.getOpenapiVersion(),
                "openapiVersion",
                32
        ));
        if (!snapshot.getOpenapiVersion().startsWith("3.1.")) {
            throw new IllegalArgumentException(
                    "openapiVersion must start with 3.1."
            );
        }
        snapshot.setDocumentSha256(hash(
                snapshot.getDocumentSha256(),
                "documentSha256"
        ));
        snapshot.setCanonicalSha256(hash(
                snapshot.getCanonicalSha256(),
                "canonicalSha256"
        ));
        snapshot.setDocumentJson(immutableObject(
                snapshot.getDocumentJson(),
                "documentJson"
        ));
        snapshot.setValidationStatus(required(
                snapshot.getValidationStatus(),
                "validationStatus",
                32
        ));
        if (!Set.of("VALID", "INVALID").contains(
                snapshot.getValidationStatus())) {
            throw new IllegalArgumentException(
                    "validationStatus must be VALID or INVALID"
            );
        }
        snapshot.setValidationMessages(immutableMessages(
                snapshot.getValidationMessages()
        ));
        if (snapshot.getOperationCount() < 0) {
            throw new IllegalArgumentException(
                    "operationCount must not be negative"
            );
        }
        if (snapshot.getSchemaCount() < 0) {
            throw new IllegalArgumentException(
                    "schemaCount must not be negative"
            );
        }
        snapshot.setFetchedFromInstanceId(required(
                snapshot.getFetchedFromInstanceId(),
                "fetchedFromInstanceId",
                256
        ));
        snapshot.setFetchedAt(Objects.requireNonNull(
                snapshot.getFetchedAt(),
                "fetchedAt"
        ));
        snapshot.setValidatedAt(Objects.requireNonNull(
                snapshot.getValidatedAt(),
                "validatedAt"
        ));
        snapshot.setCreatedAt(Objects.requireNonNull(
                snapshot.getCreatedAt(),
                "createdAt"
        ));
        return snapshot;
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
