package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayDefinitionReportStore;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

@Repository
public class JdbcGatewayDefinitionReportStore
        implements GatewayDefinitionReportStore {

    private final JdbcTemplate jdbc;

    private final ObjectMapper objectMapper;

    private final ObjectMapper canonicalMapper = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    public JdbcGatewayDefinitionReportStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<String> findBuildFingerprint(
            String applicationId,
            String buildId) {
        return jdbc.queryForList("""
                        SELECT DISTINCT fingerprint
                          FROM gateway_definition_set
                         WHERE application_id = ? AND build_id = ?
                        """,
                String.class,
                applicationId,
                buildId
        ).stream().findFirst();
    }

    @Override
    public boolean definitionSetExists(
            String applicationId,
            String definitionSetId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM gateway_definition_set
                 WHERE application_id = ? AND id = ?
                """, Integer.class, applicationId, definitionSetId);
        return count != null && count > 0;
    }

    @Override
    public int countStarterOperations(String applicationId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM gateway_operation
                 WHERE application_id = ? AND source_type = 'STARTER'
                """, Integer.class, applicationId);
        return count == null ? 0 : count;
    }

    @Override
    public StoredReport ingest(
            String applicationId,
            GatewayInterfaceDefinitionReport report,
            Instant now) {
        int operationCount = operationCount(report);
        jdbc.update("""
                INSERT INTO gateway_definition_set(
                    id, application_id, report_id, build_id, protocol,
                    fingerprint, complete_set, status, operation_count,
                    accepted_count, conflict_count, received_at, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'VERIFIED', ?, ?, 0, ?, ?)
                """,
                report.definitionSetId(),
                applicationId,
                report.reportId(),
                report.build().buildId(),
                protocol(report),
                report.definitionFingerprint(),
                report.complete(),
                operationCount,
                operationCount,
                timestamp(now),
                timestamp(now)
        );
        MutableStored stored = new MutableStored();
        for (GatewayInterfaceDefinitionReport.BusinessDomain business
                : report.businessDomains()) {
            String businessId = hierarchy(
                    "gateway_business_domain",
                    "application_id",
                    applicationId,
                    business.code(),
                    business.name(),
                    business.description(),
                    now
            );
            for (GatewayInterfaceDefinitionReport.EntityDomain entity
                    : business.entityDomains()) {
                String entityId = hierarchy(
                        "gateway_entity_domain",
                        "business_domain_id",
                        businessId,
                        entity.code(),
                        entity.name(),
                        entity.description(),
                        now
                );
                for (GatewayInterfaceDefinitionReport.InterfaceGroup group
                        : entity.interfaceGroups()) {
                    String groupId = interfaceGroup(entityId, group, now);
                    for (GatewayInterfaceDefinitionReport.Operation operation
                            : group.operations()) {
                        storeOperation(
                                applicationId,
                                groupId,
                                report.definitionSetId(),
                                operation,
                                now,
                                stored
                        );
                    }
                }
            }
        }
        return stored.freeze();
    }

    private String hierarchy(
            String table,
            String parentColumn,
            String parentId,
            String code,
            String name,
            String description,
            Instant now) {
        List<String> existing = jdbc.queryForList(
                "SELECT id FROM "
                        + table
                        + " WHERE "
                        + parentColumn
                        + " = ? AND code = ? AND deleted = FALSE",
                String.class,
                parentId,
                code
        );
        if (!existing.isEmpty()) {
            jdbc.update(
                    "UPDATE "
                            + table
                            + " SET display_name = ?, description = ?, "
                            + "updated_at = ? WHERE id = ?",
                    name,
                    description,
                    timestamp(now),
                    existing.getFirst()
            );
            return existing.getFirst();
        }
        String id = UuidV7.simpleString();
        jdbc.update(
                "INSERT INTO "
                        + table
                        + "(id, "
                        + parentColumn
                        + ", code, display_name, description, deleted, "
                        + "created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, FALSE, ?, ?)",
                id,
                parentId,
                code,
                name,
                description,
                timestamp(now),
                timestamp(now)
        );
        return id;
    }

    private String interfaceGroup(
            String entityId,
            GatewayInterfaceDefinitionReport.InterfaceGroup group,
            Instant now) {
        List<GroupRow> existing = jdbc.query("""
                SELECT id, source_type
                  FROM gateway_interface_group
                 WHERE entity_domain_id = ? AND code = ?
                   AND deleted = FALSE
                """, (result, row) -> new GroupRow(
                result.getString("id"),
                result.getString("source_type")
        ), entityId, group.code());
        if (!existing.isEmpty()) {
            GroupRow row = existing.getFirst();
            if (!"STARTER".equals(row.sourceType)) {
                throw new IllegalStateException(
                        "GATEWAY_ADMIN_STARTER_MANUAL_CONFLICT: "
                                + group.code()
                );
            }
            jdbc.update("""
                    UPDATE gateway_interface_group
                       SET display_name = ?, class_name = ?,
                           description = ?, updated_at = ?
                     WHERE id = ?
                    """,
                    group.name(),
                    group.className(),
                    group.description(),
                    timestamp(now),
                    row.id
            );
            return row.id;
        }
        String id = UuidV7.simpleString();
        jdbc.update("""
                INSERT INTO gateway_interface_group(
                    id, entity_domain_id, code, display_name, source_type,
                    class_name, description, deleted, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'STARTER', ?, ?, FALSE, ?, ?)
                """,
                id,
                entityId,
                group.code(),
                group.name(),
                group.className(),
                group.description(),
                timestamp(now),
                timestamp(now)
        );
        return id;
    }

    private void storeOperation(
            String applicationId,
            String groupId,
            String definitionSetId,
            GatewayInterfaceDefinitionReport.Operation operation,
            Instant now,
            MutableStored stored) {
        List<OperationRow> existing = jdbc.query("""
                SELECT o.id, o.interface_group_id, o.source_type,
                       o.current_definition_id,
                       d.definition_sha256,
                       COALESCE(MAX(all_d.definition_version), 0) AS max_version
                  FROM gateway_operation o
                  LEFT JOIN gateway_operation_definition d
                    ON d.id = o.current_definition_id
                  LEFT JOIN gateway_operation_definition all_d
                    ON all_d.operation_id = o.id
                 WHERE o.application_id = ? AND o.operation_key = ?
                 GROUP BY o.id, o.interface_group_id, o.source_type,
                          o.current_definition_id, d.definition_sha256
                """, (result, row) -> new OperationRow(
                result.getString("id"),
                result.getString("interface_group_id"),
                result.getString("source_type"),
                result.getString("current_definition_id"),
                result.getString("definition_sha256"),
                result.getLong("max_version")
        ), applicationId, operation.operationKey());
        String definitionSha = sha256(canonical(operation));
        if (existing.isEmpty()) {
            String operationId = UuidV7.simpleString();
            jdbc.update("""
                    INSERT INTO gateway_operation(
                        id, application_id, interface_group_id, operation_key,
                        protocol, method_identity, external_accessible,
                        provider_service_identity, source_type,
                        lifecycle_status, current_definition_id, revision,
                        created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'STARTER',
                              'DISCOVERED', NULL, 0, ?, ?)
                    """,
                    operationId,
                    applicationId,
                    groupId,
                    operation.operationKey(),
                    operation.protocol(),
                    operation.methodIdentity(),
                    operation.externalAccessible(),
                    json(operation.providerService()),
                    timestamp(now),
                    timestamp(now)
            );
            String definitionId = appendDefinition(
                    operationId,
                    definitionSetId,
                    1,
                    definitionSha,
                    operation,
                    now
            );
            linkDefinitionSet(
                    definitionSetId,
                    operationId,
                    definitionId,
                    operation,
                    now
            );
            pointPending(operationId, definitionId, operation, now);
            stored.created++;
            stored.refs.add(ref(operation, operationId, "CREATED"));
            return;
        }
        OperationRow row = existing.getFirst();
        if (!"STARTER".equals(row.sourceType)) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_STARTER_MANUAL_CONFLICT: "
                            + operation.operationKey()
            );
        }
        if (!groupId.equals(row.interfaceGroupId)) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_STARTER_GROUP_CONFLICT: "
                            + operation.operationKey()
            );
        }
        if (definitionSha.equals(row.definitionSha256)) {
            linkDefinitionSet(
                    definitionSetId,
                    row.id,
                    row.currentDefinitionId,
                    operation,
                    now
            );
            stored.refs.add(ref(operation, row.id, "UNCHANGED"));
            return;
        }
        String definitionId = findDefinition(row.id, definitionSha)
                .orElseGet(() -> appendDefinition(
                        row.id,
                        definitionSetId,
                        row.maxVersion + 1,
                        definitionSha,
                        operation,
                        now
                ));
        linkDefinitionSet(
                definitionSetId,
                row.id,
                definitionId,
                operation,
                now
        );
        pointPending(row.id, definitionId, operation, now);
        stored.updated++;
        stored.refs.add(ref(operation, row.id, "UPDATED"));
    }

    private String appendDefinition(
            String operationId,
            String definitionSetId,
            long version,
            String definitionSha,
            GatewayInterfaceDefinitionReport.Operation operation,
            Instant now) {
        String id = UuidV7.simpleString();
        jdbc.update("""
                INSERT INTO gateway_operation_definition(
                    id, operation_id, definition_set_id, definition_version,
                    definition_sha256, summary, tags, request_schema,
                    response_schema, error_schema, descriptor_snapshot,
                    attributes, external_accessible, created_at, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb,
                          ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, 'STARTER')
                """,
                id,
                operationId,
                definitionSetId,
                version,
                definitionSha,
                operation.summary(),
                json(operation.tags()),
                json(operation.requestSchema()),
                json(operation.responseSchema()),
                json(operation.errorSchema()),
                operation.descriptorSnapshot() == null
                        ? null
                        : json(operation.descriptorSnapshot()),
                json(attributes(operation)),
                operation.externalAccessible(),
                timestamp(now)
        );
        return id;
    }

    private Optional<String> findDefinition(
            String operationId,
            String definitionSha) {
        return jdbc.queryForList("""
                SELECT id
                  FROM gateway_operation_definition
                 WHERE operation_id = ? AND definition_sha256 = ?
                """, String.class, operationId, definitionSha)
                .stream()
                .findFirst();
    }

    private void linkDefinitionSet(
            String definitionSetId,
            String operationId,
            String definitionId,
            GatewayInterfaceDefinitionReport.Operation operation,
            Instant now) {
        jdbc.update("""
                INSERT INTO gateway_definition_set_operation(
                    definition_set_id, operation_id, definition_id,
                    method_identity, provider_service_identity,
                    external_accessible, deprecated, created_at
                ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                ON CONFLICT (definition_set_id, operation_id) DO NOTHING
                """,
                definitionSetId,
                operationId,
                definitionId,
                operation.methodIdentity(),
                json(operation.providerService()),
                operation.externalAccessible(),
                operation.deprecated(),
                timestamp(now)
        );
    }

    private void pointPending(
            String operationId,
            String definitionId,
            GatewayInterfaceDefinitionReport.Operation operation,
            Instant now) {
        jdbc.update("""
                UPDATE gateway_operation
                   SET current_definition_id = ?,
                       method_identity = ?,
                       external_accessible = ?,
                       provider_service_identity = ?::jsonb,
                       lifecycle_status = 'DISCOVERED',
                       deprecated_at = NULL,
                       revision = revision + 1,
                       updated_at = ?
                 WHERE id = ?
                   AND lifecycle_status IN ('DISCOVERED', 'OFFLINE')
                """,
                definitionId,
                operation.methodIdentity(),
                operation.externalAccessible(),
                json(operation.providerService()),
                timestamp(now),
                operationId
        );
    }

    private Map<String, Object> attributes(
            GatewayInterfaceDefinitionReport.Operation operation) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.putAll(operation.attributes());
        attributes.put("name", nullable(operation.name()));
        attributes.put("description", nullable(operation.description()));
        attributes.put("owner", nullable(operation.owner()));
        attributes.put("gatewaySupport", operation.gatewaySupport());
        attributes.put("parameters", operation.parameters());
        attributes.put("deprecated", operation.deprecated());
        return attributes;
    }

    private String nullable(String value) {
        return value == null ? "" : value;
    }

    private GatewayInterfaceDefinitionReportResult.OperationRef ref(
            GatewayInterfaceDefinitionReport.Operation operation,
            String operationId,
            String changeType) {
        return new GatewayInterfaceDefinitionReportResult.OperationRef(
                operation.operationKey(),
                operationId,
                changeType
        );
    }

    private int operationCount(GatewayInterfaceDefinitionReport report) {
        return report.businessDomains().stream()
                .flatMap(business -> business.entityDomains().stream())
                .flatMap(entity -> entity.interfaceGroups().stream())
                .mapToInt(group -> group.operations().size())
                .sum();
    }

    private String protocol(GatewayInterfaceDefinitionReport report) {
        List<String> protocols = report.businessDomains().stream()
                .flatMap(business -> business.entityDomains().stream())
                .flatMap(entity -> entity.interfaceGroups().stream())
                .flatMap(group -> group.operations().stream())
                .map(GatewayInterfaceDefinitionReport.Operation::protocol)
                .distinct()
                .toList();
        return protocols.size() == 1 ? protocols.getFirst() : "MIXED";
    }

    private byte[] canonical(Object value) {
        try {
            return canonicalMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway definition cannot be canonicalized",
                    failure
            );
        }
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway definition cannot be serialized",
                    failure
            );
        }
    }

    private record GroupRow(String id, String sourceType) {
    }

    private record OperationRow(
            String id,
            String interfaceGroupId,
            String sourceType,
            String currentDefinitionId,
            String definitionSha256,
            long maxVersion
    ) {
    }

    private static final class MutableStored {

        private int created;

        private int updated;

        private final List<
                GatewayInterfaceDefinitionReportResult.OperationRef> refs =
                new ArrayList<>();

        private StoredReport freeze() {
            return new StoredReport(created, updated, List.copyOf(refs));
        }
    }
}
