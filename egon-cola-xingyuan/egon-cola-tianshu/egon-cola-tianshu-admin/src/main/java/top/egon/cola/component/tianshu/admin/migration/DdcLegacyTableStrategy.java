package top.egon.cola.component.tianshu.admin.migration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Explicit V9-to-managed-table mappings used only by the offline migration CLI. */
@Slf4j
public final class DdcLegacyTableStrategy {

    private static final TenantResolver SINGLE_TENANT = (assignment, registered) -> {
        long tenantId = tenantId(assignment);
        if (!registered.contains(tenantId)) throw new IllegalArgumentException("TIANSHU_MIGRATION_TENANT_NOT_REGISTERED");
        return List.of(tenantId);
    };
    private static final TenantResolver TENANT_FAN_OUT = (assignment, registered) -> {
        if (assignment == null || !assignment.isArray() || assignment.isEmpty()) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_ENV_TENANT_MAPPING_INVALID");
        }
        List<Long> tenants = new ArrayList<>();
        Set<Long> unique = new HashSet<>();
        for (JsonNode tenant : assignment) {
            long tenantId = tenantId(tenant);
            if (!registered.contains(tenantId) || !unique.add(tenantId)) {
                throw new IllegalArgumentException("TIANSHU_MIGRATION_TENANT_NOT_REGISTERED");
            }
            tenants.add(tenantId);
        }
        tenants.sort(Long::compareTo);
        return List.copyOf(tenants);
    };

    private final List<DdcLegacyTableMappingBO> orderedMappings;
    private final Map<String, DdcLegacyTableMappingBO> mappings;

    public DdcLegacyTableStrategy() {
        List<DdcLegacyTableMappingBO> ordered = new ArrayList<>();
        register(ordered, "ddc_biz", List.of("id", "biz_code", "biz_name", "description", "enabled",
                "created_at", "updated_at"), List.of("biz_code", "biz_name", "description", "enabled",
                "created_at", "updated_at"), Map.of(), null);
        register(ordered, "ddc_env", List.of("id", "env_code", "description", "sort_order", "enabled",
                "created_at", "updated_at"), List.of("env_code", "description", "sort_order", "enabled",
                "created_at", "updated_at"), Map.of(), null, Map.of(), Set.of(), TENANT_FAN_OUT);
        register(ordered, "ddc_app", List.of("id", "app_code", "app_name", "owner", "description", "enabled",
                "created_at", "updated_at", "biz_code"), List.of("app_code", "app_name", "owner", "description",
                "enabled", "created_at", "updated_at", "biz_code"), Map.of(), null);
        register(ordered, "ddc_namespace", List.of("id", "biz_code", "namespace_code", "namespace", "description",
                "enabled", "created_at", "updated_at"), List.of("namespace", "description", "enabled", "created_at",
                "updated_at", "namespace_code", "biz_code"), Map.of(), null);
        register(ordered, "ddc_namespace_env_app", List.of("id", "namespace_id", "env_code", "app_id", "enabled",
                "created_at", "updated_at"), List.of("namespace_id", "env_code", "app_id", "enabled", "created_at",
                "updated_at"), Map.of(), null,
                Map.of("namespace_id", "ddc_namespace", "app_id", "ddc_app"),
                Set.of("namespace_id", "app_id"), SINGLE_TENANT);
        register(ordered, "ddc_config_item", List.of("id", "biz_code", "app_code", "env", "namespace", "config_key",
                "config_value", "default_value", "value_type", "current_version", "published_version", "description",
                "enabled", "deleted", "lock_version", "created_at", "updated_at"), List.of("app_code", "env",
                "namespace", "config_key", "config_value", "default_value", "value_type", "current_version",
                "description", "enabled", "deleted", "created_at", "updated_at", "published_version", "biz_code"),
                Map.of(), "lock_version");
        register(ordered, "ddc_config_version", List.of("id", "config_id", "app_code", "env", "namespace", "config_key",
                "version", "old_value", "new_value", "value_type", "change_type", "change_reason", "operator",
                "operator_ip", "created_at", "biz_code"), List.of("config_id", "app_code", "env", "namespace",
                "config_key", "config_revision", "old_value", "new_value", "value_type", "change_type", "change_reason",
                "operator", "operator_ip", "created_at", "biz_code"), Map.of("config_revision", "version"), null,
                Map.of("config_id", "ddc_config_item"), Set.of("config_id"), SINGLE_TENANT);
        register(ordered, "ddc_publish_task", List.of("id", "change_id", "config_id", "app_code", "env", "namespace",
                "config_key", "target_version", "publish_mode", "status", "target_count", "ack_count", "failed_count",
                "ignored_count", "timeout_count", "timeout_ms", "operator", "error_message", "created_at", "updated_at",
                "content_checksum", "attempt_count", "dispatched_at", "completed_at", "failure_stage", "biz_code"),
                List.of("change_id", "config_id", "app_code", "env", "namespace", "config_key", "target_version",
                        "publish_mode", "status", "target_count", "ack_count", "failed_count", "ignored_count",
                        "timeout_count", "timeout_ms", "operator", "error_message", "created_at", "updated_at",
                        "content_checksum", "attempt_count", "dispatched_at", "completed_at", "failure_stage", "biz_code"),
                Map.of(), null, Map.of("config_id", "ddc_config_item"), Set.of(), SINGLE_TENANT);
        register(ordered, "ddc_publish_ack", List.of("id", "change_id", "instance_id", "app_code", "env", "namespace",
                "config_key", "target_version", "current_version", "ack_status", "error_message", "ack_at", "lease_id",
                "content_checksum", "biz_code"), List.of("change_id", "instance_id", "app_code", "env", "namespace",
                "config_key", "target_version", "current_version", "ack_status", "error_message", "ack_at", "lease_id",
                "content_checksum", "biz_code"), Map.of(), null);
        register(ordered, "ddc_instance", List.of("id", "instance_id", "app_code", "env", "namespace", "host", "port",
                "pid", "sdk_version", "status", "last_heartbeat_at", "created_at", "updated_at", "lease_id",
                "lease_expire_at", "runtime_metadata", "biz_code", "resource_server_id", "resource_version",
                "credential_id", "admission_expires_at"), List.of("instance_id", "app_code", "env", "namespace", "host",
                "port", "pid", "sdk_version", "status", "last_heartbeat_at", "created_at", "updated_at", "lease_id",
                "lease_expire_at", "runtime_metadata", "biz_code", "resource_server_id", "resource_version",
                "credential_id", "admission_expires_at"), Map.of(), null);
        register(ordered, "ddc_operation_log", List.of("id", "app_code", "env", "namespace", "config_key", "operation_type",
                "operator", "operator_ip", "operation_content", "created_at", "biz_code"), List.of("app_code", "env",
                "namespace", "config_key", "operation_type", "operator", "operator_ip", "operation_content", "created_at",
                "biz_code"), Map.of(), null);
        orderedMappings = List.copyOf(ordered);
        Map<String, DdcLegacyTableMappingBO> index = new LinkedHashMap<>();
        orderedMappings.forEach(mapping -> index.put(mapping.sourceTable(), mapping));
        mappings = Map.copyOf(index);
    }

    public List<String> orderedLegacyTableNames() {
        return orderedMappings.stream().map(DdcLegacyTableMappingBO::sourceTable).toList();
    }

    public List<String> sourceColumnNames(String sourceTable) {
        return mapping(sourceTable).sourceColumns();
    }

    public List<String> targetColumnNames(String sourceTable) {
        return mapping(sourceTable).targetColumns();
    }

    public String targetTableName(String sourceTable) {
        return mapping(sourceTable).targetTable();
    }

    public List<Long> tenantIds(JsonNode tenantMappings, String sourceTable, String legacyId,
                                Set<Long> registeredTenants) {
        DdcLegacyTableMappingBO table = mapping(sourceTable);
        JsonNode assignments = tenantMappings == null ? null : tenantMappings.path(sourceTable);
        JsonNode assignment = assignments == null ? null : assignments.get(legacyId);
        if (assignment == null) throw new IllegalArgumentException("TIANSHU_LEGACY_TENANT_REQUIRED: " + sourceTable);
        return table.tenantResolver().resolve(assignment, registeredTenants);
    }

    public void validateTenantMappings(JsonNode tenantMappings, Set<Long> registeredTenants) {
        if (tenantMappings == null || !tenantMappings.isObject()
                || tenantMappings.size() != orderedMappings.size()) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_TENANT_MAPPING_INVALID");
        }
        Set<String> seenTables = new HashSet<>();
        tenantMappings.fields().forEachRemaining(entry -> {
            DdcLegacyTableMappingBO table = mappings.get(entry.getKey());
            if (table == null || !entry.getValue().isObject()) {
                throw new IllegalArgumentException("TIANSHU_MIGRATION_TENANT_MAPPING_INVALID");
            }
            seenTables.add(entry.getKey());
            entry.getValue().fields().forEachRemaining(rule ->
                    table.tenantResolver().resolve(rule.getValue(), registeredTenants));
        });
        if (!seenTables.equals(Set.copyOf(orderedLegacyTableNames()))) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_TENANT_MAPPING_INVALID");
        }
    }

    public List<DdcLegacyParentReferenceVO> parentReferences(String sourceTable, Map<String, Object> sourceValues) {
        DdcLegacyTableMappingBO table = mapping(sourceTable);
        List<DdcLegacyParentReferenceVO> references = new ArrayList<>();
        for (String targetColumn : table.targetColumns()) {
            String parentTable = table.parentTables().get(targetColumn);
            if (parentTable == null) continue;
            Object legacyId = sourceValues.get(targetColumn);
            if (legacyId == null || legacyId.toString().isBlank()) {
                if (table.requiredParents().contains(targetColumn)) {
                    throw new IllegalArgumentException("TIANSHU_LEGACY_PARENT_ID_REQUIRED");
                }
                continue;
            }
            references.add(parentReference(parentTable, targetColumn, legacyId));
        }
        return List.copyOf(references);
    }

    public DdcLegacyRowDTO mapSourceRow(String sourceTable, Map<String, Object> sourceValues, Long tenantId,
                                        Map<String, Long> resolvedParentIds) {
        DdcLegacyTableMappingBO table = mapping(sourceTable);
        if (sourceValues == null || tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("TIANSHU_LEGACY_TENANT_REQUIRED");
        }
        String legacyId = text(sourceValues.get("id"));
        if (legacyId.isBlank()) throw new IllegalArgumentException("TIANSHU_LEGACY_PUBLIC_ID_REQUIRED");
        Map<String, Long> parentIds = resolvedParentIds == null ? Map.of() : resolvedParentIds;
        List<DdcLegacyParentReferenceVO> references = parentReferences(sourceTable, sourceValues);
        Map<String, Object> targetValues = new LinkedHashMap<>();
        for (String targetColumn : table.targetColumns()) {
            DdcLegacyParentReferenceVO parent = references.stream()
                    .filter(reference -> reference.targetColumn().equals(targetColumn)).findFirst().orElse(null);
            Object value;
            if (parent != null) {
                value = parentIds.get(parentKey(parent));
                if (value == null) throw new IllegalArgumentException("TIANSHU_LEGACY_PARENT_MAPPING_REQUIRED");
            } else {
                String sourceColumn = table.renamedColumns().getOrDefault(targetColumn, targetColumn);
                if (!sourceValues.containsKey(sourceColumn)) {
                    throw new IllegalArgumentException("TIANSHU_LEGACY_SOURCE_COLUMN_REQUIRED: " + sourceColumn);
                }
                value = normalizeValue(targetColumn, sourceValues.get(sourceColumn));
            }
            targetValues.put(targetColumn, value);
        }
        Long technicalVersion = table.technicalVersionColumn() == null ? 0L
                : number(sourceValues.get(table.technicalVersionColumn()), 0L);
        return DdcLegacyRowDTO.builder()
                .sourceTable(table.sourceTable())
                .targetTable(table.targetTable())
                .legacyId(legacyId)
                .tenantId(tenantId)
                .publicId(legacyId)
                .sourceHash(sourceHash(table, sourceValues))
                .targetValues(targetValues)
                .technicalVersion(technicalVersion)
                .build();
    }

    private static DdcLegacyParentReferenceVO parentReference(String table, String targetColumn, Object value) {
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("TIANSHU_LEGACY_PARENT_ID_REQUIRED");
        }
        return new DdcLegacyParentReferenceVO(table, targetColumn, value.toString());
    }

    private static String parentKey(DdcLegacyParentReferenceVO reference) {
        return reference.sourceTable() + ":" + reference.legacyPublicId();
    }

    private DdcLegacyTableMappingBO mapping(String sourceTable) {
        DdcLegacyTableMappingBO mapping = mappings.get(sourceTable);
        if (mapping == null) throw new IllegalArgumentException("TIANSHU_LEGACY_TABLE_UNSUPPORTED");
        return mapping;
    }

    private static void register(List<DdcLegacyTableMappingBO> mappings, String sourceTable,
                                 List<String> sourceColumns, List<String> targetColumns,
                                 Map<String, String> renamedColumns, String technicalVersionColumn) {
        register(mappings, sourceTable, sourceColumns, targetColumns, renamedColumns, technicalVersionColumn,
                Map.of(), Set.of(), SINGLE_TENANT);
    }

    private static void register(List<DdcLegacyTableMappingBO> mappings, String sourceTable,
                                 List<String> sourceColumns, List<String> targetColumns,
                                 Map<String, String> renamedColumns, String technicalVersionColumn,
                                 Map<String, String> parentTables, Set<String> requiredParents,
                                 TenantResolver tenantResolver) {
        mappings.add(new DdcLegacyTableMappingBO(sourceTable, sourceTable + "_t0", sourceColumns,
                targetColumns, renamedColumns, technicalVersionColumn, parentTables, requiredParents,
                tenantResolver));
    }

    private static Object normalizeValue(String column, Object value) {
        if (value == null) return null;
        if (column.endsWith("_at")) return localDateTime(value);
        if ("enabled".equals(column) || "deleted".equals(column)) return booleanValue(value);
        if ("config_revision".equals(column) || "current_version".equals(column)
                || "published_version".equals(column) || "target_version".equals(column)
                || "resource_version".equals(column)) {
            if (value == null) {
                if ("config_revision".equals(column) || "current_version".equals(column)) {
                    throw new IllegalArgumentException("TIANSHU_LEGACY_REQUIRED_VERSION_MISSING");
                }
                return null;
            }
            return number(value, 0L);
        }
        return value;
    }

    private static LocalDateTime localDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof String text) {
            try {
                return LocalDateTime.parse(text.replace(' ', 'T'));
            } catch (DateTimeParseException failure) {
                throw new IllegalArgumentException("TIANSHU_LEGACY_LOCAL_TIME_INVALID");
            }
        }
        throw new IllegalArgumentException("TIANSHU_LEGACY_LOCAL_TIME_INVALID");
    }

    private static Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) return true;
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) return false;
        }
        throw new IllegalArgumentException("TIANSHU_LEGACY_BOOLEAN_INVALID");
    }

    private static Long number(Object value, long defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("TIANSHU_LEGACY_NUMBER_INVALID");
        }
    }

    private static String sourceHash(DdcLegacyTableMappingBO table, Map<String, Object> sourceValues) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String column : table.sourceColumns()) {
                String value = canonical(sourceValues.get(column));
                digest.update(column.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(Integer.toString(value.length()).getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) ':');
                digest.update(value.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA256_UNAVAILABLE", failure);
        }
    }

    private static String canonical(Object value) {
        if (value == null) return "<NULL>";
        if (value instanceof LocalDateTime localDateTime) return localDateTime.toString();
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime().toString();
        if (value instanceof Boolean bool) return bool ? "true" : "false";
        if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString();
        return value.toString();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private static long tenantId(JsonNode tenant) {
        if (tenant == null || tenant.isNull() || !(tenant.isIntegralNumber() || tenant.isTextual())) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_TENANT_ID_INVALID");
        }
        try {
            long value = tenant.isIntegralNumber() ? tenant.longValue() : Long.parseLong(tenant.asText());
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_TENANT_ID_INVALID");
        }
    }

    @FunctionalInterface
    private interface TenantResolver {
        List<Long> resolve(JsonNode assignment, Set<Long> registeredTenants);
    }

    private record DdcLegacyTableMappingBO(String sourceTable, String targetTable, List<String> sourceColumns,
                                           List<String> targetColumns, Map<String, String> renamedColumns,
                                           String technicalVersionColumn, Map<String, String> parentTables,
                                           Set<String> requiredParents, TenantResolver tenantResolver) {
        private DdcLegacyTableMappingBO {
            Objects.requireNonNull(sourceTable);
            Objects.requireNonNull(targetTable);
            sourceColumns = List.copyOf(sourceColumns);
            targetColumns = List.copyOf(targetColumns);
            renamedColumns = Map.copyOf(renamedColumns);
            parentTables = Map.copyOf(parentTables);
            requiredParents = Set.copyOf(requiredParents);
            Objects.requireNonNull(tenantResolver);
        }
    }

    public record DdcLegacyParentReferenceVO(String sourceTable, String targetColumn, String legacyPublicId) {
    }
}
