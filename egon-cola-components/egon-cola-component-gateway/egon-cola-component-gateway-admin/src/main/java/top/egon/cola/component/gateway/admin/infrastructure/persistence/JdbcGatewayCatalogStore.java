package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

@Repository
public class JdbcGatewayCatalogStore implements GatewayCatalogStore {

    private final JdbcTemplate jdbc;

    private final ObjectMapper objectMapper;

    public JdbcGatewayCatalogStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public CatalogTree loadCatalog(String applicationId) {
        Map<String, MutableBusiness> businesses = new LinkedHashMap<>();
        jdbc.query("""
                SELECT b.id AS business_id,
                       b.code AS business_code,
                       b.display_name AS business_name,
                       e.id AS entity_id,
                       e.code AS entity_code,
                       e.display_name AS entity_name,
                       g.id AS group_id,
                       g.code AS group_code,
                       g.display_name AS group_name,
                       g.source_type,
                       g.class_name,
                       o.id AS operation_id,
                       o.operation_key,
                       o.protocol,
                       o.method_identity,
                       o.external_accessible,
                       o.lifecycle_status,
                       o.source_type AS operation_source,
                       o.revision
                  FROM gateway_business_domain b
                  LEFT JOIN gateway_entity_domain e
                    ON e.business_domain_id = b.id AND e.deleted = FALSE
                  LEFT JOIN gateway_interface_group g
                    ON g.entity_domain_id = e.id AND g.deleted = FALSE
                  LEFT JOIN gateway_operation o
                    ON o.interface_group_id = g.id
                 WHERE b.application_id = ? AND b.deleted = FALSE
                 ORDER BY b.code, e.code, g.code, o.operation_key
                """, result -> {
            collect(result, businesses);
            return null;
        }, applicationId);
        return new CatalogTree(
                applicationId,
                businesses.values().stream()
                        .map(MutableBusiness::freeze)
                        .toList()
        );
    }

    @Override
    public String createManualHierarchy(
            String applicationId,
            ManualHierarchy hierarchy,
            Instant now) {
        requireApplication(applicationId);
        String businessId = findOrCreateBusiness(
                applicationId,
                hierarchy,
                now
        );
        String entityId = findOrCreateEntity(
                businessId,
                hierarchy,
                now
        );
        String interfaceGroupId = UuidV7.simpleString();
        jdbc.update("""
                INSERT INTO gateway_interface_group(
                    id, entity_domain_id, code, display_name, source_type,
                    class_name, description, deleted, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'MANUAL', ?, ?, FALSE, ?, ?)
                """,
                interfaceGroupId,
                entityId,
                hierarchy.interfaceGroupCode(),
                hierarchy.interfaceGroupName(),
                hierarchy.className(),
                hierarchy.description(),
                timestamp(now),
                timestamp(now)
        );
        return interfaceGroupId;
    }

    @Override
    public Optional<InterfaceGroupScope> findInterfaceGroup(String id) {
        return jdbc.query("""
                SELECT g.id, a.id AS application_id, a.application_code,
                       a.env, a.namespace
                  FROM gateway_interface_group g
                  JOIN gateway_entity_domain e ON e.id = g.entity_domain_id
                  JOIN gateway_business_domain b
                    ON b.id = e.business_domain_id
                  JOIN gateway_application a ON a.id = b.application_id
                 WHERE g.id = ? AND g.deleted = FALSE
                   AND e.deleted = FALSE AND b.deleted = FALSE
                   AND a.deleted = FALSE
                """, (result, row) -> new InterfaceGroupScope(
                result.getString("id"),
                result.getString("application_id"),
                result.getString("application_code"),
                result.getString("env"),
                result.getString("namespace")
        ), id).stream().findFirst();
    }

    @Override
    public Optional<OperationRecord> findOperation(String operationId) {
        return queryOperation("WHERE o.id = ?", operationId);
    }

    @Override
    public Optional<OperationRecord> findOperation(
            String applicationId,
            String operationKey) {
        return queryOperation(
                "WHERE o.application_id = ? AND o.operation_key = ?",
                applicationId,
                operationKey
        );
    }

    @Override
    public List<OperationDefinition> loadDefinitions(String operationId) {
        return jdbc.query("""
                SELECT id, operation_id, definition_version,
                       definition_sha256, summary, tags::text AS tags,
                       request_schema::text AS request_schema,
                       response_schema::text AS response_schema,
                       error_schema::text AS error_schema,
                       descriptor_snapshot::text AS descriptor_snapshot,
                       attributes::text AS attributes,
                       external_accessible, created_at, created_by
                  FROM gateway_operation_definition
                 WHERE operation_id = ?
                 ORDER BY definition_version DESC
                """, (result, row) -> new OperationDefinition(
                result.getString("id"),
                result.getString("operation_id"),
                result.getLong("definition_version"),
                result.getString("definition_sha256"),
                result.getString("summary"),
                list(result.getString("tags")),
                map(result.getString("request_schema")),
                map(result.getString("response_schema")),
                mapList(result.getString("error_schema")),
                result.getString("descriptor_snapshot") == null
                        ? null
                        : map(result.getString("descriptor_snapshot")),
                map(result.getString("attributes")),
                result.getBoolean("external_accessible"),
                result.getTimestamp("created_at").toInstant(),
                result.getString("created_by")
        ), operationId);
    }

    @Override
    public void insertOperation(OperationRecord operation) {
        jdbc.update("""
                INSERT INTO gateway_operation(
                    id, application_id, interface_group_id, operation_key,
                    protocol, method_identity, external_accessible,
                    provider_service_identity, source_type, lifecycle_status,
                    current_definition_id, revision, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, NULL, 0, ?, ?)
                """,
                operation.id(),
                operation.applicationId(),
                operation.interfaceGroupId(),
                operation.operationKey(),
                operation.protocol(),
                operation.methodIdentity(),
                operation.externalAccessible(),
                json(operation.providerServiceIdentity()),
                operation.sourceType(),
                operation.lifecycleStatus(),
                timestamp(operation.createdAt()),
                timestamp(operation.updatedAt())
        );
    }

    @Override
    public void appendDefinition(OperationDefinition definition) {
        jdbc.update("""
                INSERT INTO gateway_operation_definition(
                    id, operation_id, definition_set_id, definition_version,
                    definition_sha256, summary, tags, request_schema,
                    response_schema, error_schema, descriptor_snapshot,
                    attributes, external_accessible, created_at, created_by
                ) VALUES (?, ?, NULL, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb,
                          ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?)
                """,
                definition.id(),
                definition.operationId(),
                definition.definitionVersion(),
                definition.definitionSha256(),
                definition.summary(),
                json(definition.tags()),
                json(definition.requestSchema()),
                json(definition.responseSchema()),
                json(definition.errorSchema()),
                definition.descriptorSnapshot() == null
                        ? null
                        : json(definition.descriptorSnapshot()),
                json(definition.attributes()),
                definition.externalAccessible(),
                timestamp(definition.createdAt()),
                definition.createdBy()
        );
    }

    @Override
    public void pointToDefinition(
            String operationId,
            String definitionId,
            boolean externalAccessible,
            Instant now) {
        int updated = jdbc.update("""
                UPDATE gateway_operation
                   SET current_definition_id = ?,
                       external_accessible = ?,
                       revision = revision + 1,
                       lifecycle_status = 'ACTIVE',
                       updated_at = ?
                 WHERE id = ?
                """, definitionId, externalAccessible, timestamp(now),
                operationId);
        if (updated == 0) {
            throw new GatewayAdminNotFoundException(
                    "gateway operation " + operationId + " was not found"
            );
        }
    }

    @Override
    public void deprecate(String operationId, Instant now) {
        int updated = jdbc.update("""
                UPDATE gateway_operation
                   SET lifecycle_status = 'DEPRECATED',
                       deprecated_at = ?,
                       revision = revision + 1,
                       updated_at = ?
                 WHERE id = ?
                """, timestamp(now), timestamp(now), operationId);
        if (updated == 0) {
            throw new GatewayAdminNotFoundException(
                    "gateway operation " + operationId + " was not found"
            );
        }
    }

    private Optional<OperationRecord> queryOperation(
            String where,
            Object... arguments) {
        return jdbc.query("""
                SELECT o.id, o.application_id, o.interface_group_id,
                       o.operation_key, o.protocol, o.method_identity,
                       o.external_accessible,
                       o.provider_service_identity::text AS provider_identity,
                       o.source_type, o.lifecycle_status,
                       o.current_definition_id, o.revision,
                       o.created_at, o.updated_at
                  FROM gateway_operation o
                """ + where, (result, row) -> operation(result), arguments)
                .stream()
                .findFirst();
    }

    private OperationRecord operation(ResultSet result) throws SQLException {
        return new OperationRecord(
                result.getString("id"),
                result.getString("application_id"),
                result.getString("interface_group_id"),
                result.getString("operation_key"),
                result.getString("protocol"),
                result.getString("method_identity"),
                result.getBoolean("external_accessible"),
                map(result.getString("provider_identity")),
                result.getString("source_type"),
                result.getString("lifecycle_status"),
                result.getString("current_definition_id"),
                result.getLong("revision"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()
        );
    }

    private void collect(
            ResultSet result,
            Map<String, MutableBusiness> businesses) throws SQLException {
        while (result.next()) {
            String businessId = result.getString("business_id");
            if (businessId == null) {
                continue;
            }
            MutableBusiness business = businesses.computeIfAbsent(
                    businessId,
                    ignored -> new MutableBusiness(
                            businessId,
                            get(result, "business_code"),
                            get(result, "business_name")
                    )
            );
            String entityId = result.getString("entity_id");
            if (entityId == null) {
                continue;
            }
            MutableEntity entity = business.entities.computeIfAbsent(
                    entityId,
                    ignored -> new MutableEntity(
                            entityId,
                            get(result, "entity_code"),
                            get(result, "entity_name")
                    )
            );
            String groupId = result.getString("group_id");
            if (groupId == null) {
                continue;
            }
            MutableGroup group = entity.groups.computeIfAbsent(
                    groupId,
                    ignored -> new MutableGroup(
                            groupId,
                            get(result, "group_code"),
                            get(result, "group_name"),
                            get(result, "source_type"),
                            get(result, "class_name")
                    )
            );
            String operationId = result.getString("operation_id");
            if (operationId != null) {
                group.operations.add(new OperationNode(
                        operationId,
                        get(result, "operation_key"),
                        get(result, "protocol"),
                        get(result, "method_identity"),
                        result.getBoolean("external_accessible"),
                        get(result, "lifecycle_status"),
                        get(result, "operation_source"),
                        result.getLong("revision")
                ));
            }
        }
    }

    private String findOrCreateBusiness(
            String applicationId,
            ManualHierarchy hierarchy,
            Instant now) {
        List<String> existing = jdbc.queryForList("""
                SELECT id FROM gateway_business_domain
                 WHERE application_id = ? AND code = ? AND deleted = FALSE
                """, String.class, applicationId, hierarchy.businessCode());
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        String id = UuidV7.simpleString();
        jdbc.update("""
                INSERT INTO gateway_business_domain(
                    id, application_id, code, display_name, description,
                    deleted, created_at, updated_at
                ) VALUES (?, ?, ?, ?, NULL, FALSE, ?, ?)
                """, id, applicationId, hierarchy.businessCode(),
                hierarchy.businessName(), timestamp(now), timestamp(now));
        return id;
    }

    private String findOrCreateEntity(
            String businessId,
            ManualHierarchy hierarchy,
            Instant now) {
        List<String> existing = jdbc.queryForList("""
                SELECT id FROM gateway_entity_domain
                 WHERE business_domain_id = ? AND code = ?
                   AND deleted = FALSE
                """, String.class, businessId, hierarchy.entityCode());
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        String id = UuidV7.simpleString();
        jdbc.update("""
                INSERT INTO gateway_entity_domain(
                    id, business_domain_id, code, display_name, description,
                    deleted, created_at, updated_at
                ) VALUES (?, ?, ?, ?, NULL, FALSE, ?, ?)
                """, id, businessId, hierarchy.entityCode(),
                hierarchy.entityName(), timestamp(now), timestamp(now));
        return id;
    }

    private void requireApplication(String applicationId) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM gateway_application
                 WHERE id = ? AND deleted = FALSE
                """, Integer.class, applicationId);
        if (count == null || count == 0) {
            throw new GatewayAdminNotFoundException(
                    "gateway application " + applicationId + " was not found"
            );
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "catalog value cannot be serialized",
                    failure
            );
        }
    }

    private Map<String, Object> map(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored provider identity is invalid",
                    failure
            );
        }
    }

    private List<String> list(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<List<String>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("stored tags are invalid", failure);
        }
    }

    private List<Map<String, Object>> mapList(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<List<Map<String, Object>>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored error schema is invalid",
                    failure
            );
        }
    }

    private static String get(ResultSet result, String column) {
        try {
            return result.getString(column);
        } catch (SQLException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static final class MutableBusiness {

        private final String id;

        private final String code;

        private final String displayName;

        private final Map<String, MutableEntity> entities =
                new LinkedHashMap<>();

        private MutableBusiness(
                String id,
                String code,
                String displayName) {
            this.id = id;
            this.code = code;
            this.displayName = displayName;
        }

        private BusinessNode freeze() {
            return new BusinessNode(
                    id,
                    code,
                    displayName,
                    entities.values().stream()
                            .map(MutableEntity::freeze)
                            .toList()
            );
        }
    }

    private static final class MutableEntity {

        private final String id;

        private final String code;

        private final String displayName;

        private final Map<String, MutableGroup> groups =
                new LinkedHashMap<>();

        private MutableEntity(
                String id,
                String code,
                String displayName) {
            this.id = id;
            this.code = code;
            this.displayName = displayName;
        }

        private EntityNode freeze() {
            return new EntityNode(
                    id,
                    code,
                    displayName,
                    groups.values().stream()
                            .map(MutableGroup::freeze)
                            .toList()
            );
        }
    }

    private static final class MutableGroup {

        private final String id;

        private final String code;

        private final String displayName;

        private final String sourceType;

        private final String className;

        private final List<OperationNode> operations = new ArrayList<>();

        private MutableGroup(
                String id,
                String code,
                String displayName,
                String sourceType,
                String className) {
            this.id = id;
            this.code = code;
            this.displayName = displayName;
            this.sourceType = sourceType;
            this.className = className;
        }

        private InterfaceGroupNode freeze() {
            return new InterfaceGroupNode(
                    id,
                    code,
                    displayName,
                    sourceType,
                    className,
                    List.copyOf(operations)
            );
        }
    }
}
