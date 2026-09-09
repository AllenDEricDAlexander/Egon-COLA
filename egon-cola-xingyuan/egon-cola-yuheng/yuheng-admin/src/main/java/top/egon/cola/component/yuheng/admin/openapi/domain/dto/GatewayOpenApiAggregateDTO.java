package top.egon.cola.component.yuheng.admin.openapi.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable complete OpenAPI Group set handed from synchronization to the
 * aggregate coordinator.
 *
 * <p>中文：该 DTO 只表达一个 application/build 的完整 Group 集合；文档和
 * snapshot id 都必须与 manifest 的 Group key 一一对应，不能把半套文档交给
 * Definition ingestion。</p>
 */
public record GatewayOpenApiAggregateDTO(
        @NotBlank
        @Size(max = 64)
        String applicationId,
        @NotBlank
        @Size(max = 256)
        String buildId,
        @NotEmpty
        @Size(min = 1, max = 16)
        List<@NotBlank @Pattern(regexp = "[a-z][a-z0-9-]{0,63}") String> groups,
        @NotEmpty
        Map<String, @Valid GatewayOpenApiDocumentDTO> documents,
        @NotEmpty
        @Size(min = 1, max = 16)
        List<@NotBlank String> snapshotIds,
        @NotBlank
        @Pattern(regexp = "[0-9a-f]{64}")
        String aggregateSha256
) {

    /**
     * Creates an aggregate and derives its source scope from the immutable
     * document hashes. The coordinator repeats this calculation after mapping
     * to prevent manifest drift from being ingested.
     */
    public static GatewayOpenApiAggregateDTO of(
            String applicationId,
            String buildId,
            List<String> groups,
            Map<String, GatewayOpenApiDocumentDTO> documents,
            Map<String, String> snapshotIds) {
        List<String> normalizedGroups = normalizeGroups(groups);
        Map<String, GatewayOpenApiDocumentDTO> normalizedDocuments =
                normalizeDocuments(documents, normalizedGroups);
        Map<String, String> normalizedSnapshotMap = normalizeSnapshotMap(
                snapshotIds,
                normalizedGroups
        );
        return new GatewayOpenApiAggregateDTO(
                applicationId,
                buildId,
                normalizedGroups,
                normalizedDocuments,
                normalizedSnapshotMap.values().stream().sorted().toList(),
                calculateAggregateSha256(
                        normalizedGroups,
                        normalizedDocuments
                )
        );
    }

    /** Creates an aggregate when snapshot IDs are already manifest-ordered. */
    public static GatewayOpenApiAggregateDTO of(
            String applicationId,
            String buildId,
            List<String> groups,
            Map<String, GatewayOpenApiDocumentDTO> documents,
            List<String> snapshotIds) {
        return new GatewayOpenApiAggregateDTO(
                applicationId,
                buildId,
                groups,
                documents,
                snapshotIds,
                calculateAggregateSha256(groups, documents)
        );
    }

    public GatewayOpenApiAggregateDTO {
        applicationId = required(applicationId, "applicationId", 64);
        buildId = required(buildId, "buildId", 256);
        groups = normalizeGroups(groups);
        documents = normalizeDocuments(documents, groups);
        snapshotIds = normalizeSnapshots(snapshotIds, groups);
        aggregateSha256 = requiredHash(aggregateSha256, "aggregateSha256");
    }

    /**
     * Calculates the deterministic source scope used by the coordinator.
     * Group order is manifest order and each value is the exact received
     * document hash; canonical document hashing remains the adapter's concern.
     */
    public static String calculateAggregateSha256(
            List<String> groups,
            Map<String, GatewayOpenApiDocumentDTO> documents) {
        List<String> normalizedGroups = normalizeGroups(groups);
        Map<String, GatewayOpenApiDocumentDTO> normalizedDocuments =
                normalizeDocuments(documents, normalizedGroups);
        StringBuilder source = new StringBuilder();
        for (String group : normalizedGroups) {
            source.append(group)
                    .append('\0')
                    .append(normalizedDocuments.get(group).documentSha256())
                    .append('\n');
        }
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(
                            source.toString().getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    impossible
            );
        }
    }

    /** Alias used by coordinators and recovery code. */
    public String sourceScope() {
        return aggregateSha256;
    }

    private static List<String> normalizeGroups(List<String> values) {
        Objects.requireNonNull(values, "groups");
        if (values.isEmpty() || values.size() > 16) {
            throw new IllegalArgumentException(
                    "groups must contain between 1 and 16 values"
            );
        }
        List<String> normalized = values.stream()
                .map(value -> required(value, "group", 64))
                .sorted()
                .toList();
        if (normalized.stream().distinct().count() != normalized.size()) {
            throw new IllegalArgumentException("groups must be unique");
        }
        for (String group : normalized) {
            if (!group.matches("[a-z][a-z0-9-]{0,63}")) {
                throw new IllegalArgumentException(
                        "group has an invalid code: " + group
                );
            }
        }
        return List.copyOf(normalized);
    }

    private static Map<String, GatewayOpenApiDocumentDTO> normalizeDocuments(
            Map<String, GatewayOpenApiDocumentDTO> values,
            List<String> groups) {
        Objects.requireNonNull(values, "documents");
        Map<String, GatewayOpenApiDocumentDTO> normalized =
                new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String group = required(key, "document group", 64);
            if (!groups.contains(group)) {
                throw new IllegalArgumentException(
                        "documents contain an unadvertised group: " + group
                );
            }
            normalized.put(
                    group,
                    Objects.requireNonNull(value, "document " + group)
            );
        });
        if (!normalized.keySet().equals(new java.util.LinkedHashSet<>(groups))) {
            throw new IllegalArgumentException(
                    "documents must contain exactly the advertised groups"
            );
        }
        return Map.copyOf(normalized);
    }

    private static Map<String, String> normalizeSnapshotMap(
            Map<String, String> values,
            List<String> groups) {
        Objects.requireNonNull(values, "snapshotIds");
        Map<String, String> normalized = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String group = required(key, "snapshot group", 64);
            if (!groups.contains(group)) {
                throw new IllegalArgumentException(
                        "snapshotIds contain an unadvertised group: " + group
                );
            }
            normalized.put(group, required(value, "snapshotId", 64));
        });
        if (!normalized.keySet().equals(new java.util.LinkedHashSet<>(groups))) {
            throw new IllegalArgumentException(
                    "snapshotIds must contain exactly the advertised groups"
            );
        }
        if (normalized.values().stream().distinct().count()
                != normalized.size()) {
            throw new IllegalArgumentException("snapshotIds must be unique");
        }
        return Map.copyOf(normalized);
    }

    private static List<String> normalizeSnapshots(
            List<String> values,
            List<String> groups) {
        Objects.requireNonNull(values, "snapshotIds");
        if (values.size() != groups.size()) {
            throw new IllegalArgumentException(
                    "snapshotIds must contain one value per advertised group"
            );
        }
        List<String> normalized = values.stream()
                .map(value -> required(value, "snapshotId", 64))
                .sorted()
                .toList();
        if (normalized.stream().distinct().count() != normalized.size()) {
            throw new IllegalArgumentException("snapshotIds must be unique");
        }
        return List.copyOf(normalized);
    }

    private static String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " is blank or exceeds " + max + " characters"
            );
        }
        return normalized;
    }

    private static String requiredHash(String value, String field) {
        String normalized = required(value, field, 64);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    field + " must be lowercase hexadecimal SHA-256"
            );
        }
        return normalized;
    }
}
