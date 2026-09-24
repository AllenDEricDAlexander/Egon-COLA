package top.egon.cola.component.outbox.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;
import top.egon.cola.component.outbox.common.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Verifies the managed outbox schema using read-only metadata access to its physical PRIMARY. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class OutboxSchemaMetadataValidator {

    private static final String SCHEMA = "egon_outbox";
    private static final String MESSAGE_TABLE = "egon_cola_outbox_message";
    private static final String HISTORY_TABLE = "ddl_history";

    private static final Set<String> BIGINT = Set.of("bigint", "int8");
    private static final Set<String> VARCHAR = Set.of("varchar", "character varying");
    private static final Set<String> CHAR = Set.of("char", "character", "bpchar");
    private static final Set<String> TEXT = Set.of("text");
    private static final Set<String> INTEGER = Set.of("integer", "int4");
    private static final Set<String> TIMESTAMPTZ = Set.of("timestamptz", "timestamp with time zone");
    private static final Set<String> TIMESTAMP = Set.of("timestamp", "timestamp without time zone");

    private static final Map<String, ColumnSpecBO> MESSAGE_COLUMNS = Map.ofEntries(
            Map.entry("id", column(BIGINT, 0, 0, false)),
            Map.entry("message_id", column(VARCHAR, 64, 0, false)),
            Map.entry("idempotency_key", column(VARCHAR, 256, 0, true)),
            Map.entry("message_fingerprint", column(CHAR, 64, 0, false)),
            Map.entry("channel", column(VARCHAR, 64, 0, false)),
            Map.entry("destination", column(VARCHAR, 256, 0, false)),
            Map.entry("payload", column(TEXT, 0, 0, false)),
            Map.entry("content_type", column(VARCHAR, 128, 0, false)),
            Map.entry("schema_version", column(VARCHAR, 32, 0, true)),
            Map.entry("headers_json", column(TEXT, 0, 0, false)),
            Map.entry("trace_id", column(VARCHAR, 128, 0, true)),
            Map.entry("status", column(VARCHAR, 32, 0, false)),
            Map.entry("attempt_count", column(INTEGER, 0, 0, false)),
            Map.entry("max_attempts", column(INTEGER, 0, 0, false)),
            Map.entry("next_attempt_at", column(TIMESTAMPTZ, 0, 6, false)),
            Map.entry("locked_by", column(VARCHAR, 128, 0, true)),
            Map.entry("locked_until", column(TIMESTAMPTZ, 0, 6, true)),
            Map.entry("last_error_code", column(VARCHAR, 64, 0, true)),
            Map.entry("last_error_message", column(TEXT, 0, 0, true)),
            Map.entry("created_at", column(TIMESTAMPTZ, 0, 6, false)),
            Map.entry("updated_at", column(TIMESTAMPTZ, 0, 6, false)),
            Map.entry("completed_at", column(TIMESTAMPTZ, 0, 6, true)),
            Map.entry("tenant_id", column(BIGINT, 0, 0, false)),
            Map.entry("create_user_id", column(VARCHAR, 128, 0, false)),
            Map.entry("create_time", column(TIMESTAMPTZ, 0, 6, false)),
            Map.entry("update_user_id", column(VARCHAR, 128, 0, false)),
            Map.entry("update_time", column(TIMESTAMPTZ, 0, 6, false)),
            Map.entry("deleted_at", column(TIMESTAMP, 0, 6, true)),
            Map.entry("version", column(BIGINT, 0, 0, false))
    );

    private static final Map<String, ColumnSpecBO> HISTORY_COLUMNS = Map.ofEntries(
            Map.entry("tenant_id", column(BIGINT, 0, 0, false)),
            Map.entry("script", column(VARCHAR, 500, 0, false)),
            Map.entry("type", column(VARCHAR, 30, 0, false)),
            Map.entry("version", column(VARCHAR, 30, 0, false)),
            Map.entry("checksum", column(CHAR, 64, 0, false)),
            Map.entry("installed_on", column(TIMESTAMPTZ, 0, 6, false)),
            Map.entry("execution_ms", column(BIGINT, 0, 0, false)),
            Map.entry("route_fingerprint", column(CHAR, 64, 0, false))
    );

    private static final Map<String, IndexSpecBO> MESSAGE_INDEXES = Map.of(
            "pk_outbox_message", new IndexSpecBO(List.of("id"), true),
            "uk_outbox_message_id", new IndexSpecBO(List.of("message_id"), true),
            "uk_outbox_idempotency_key", new IndexSpecBO(List.of("idempotency_key"), true),
            "idx_outbox_claim", new IndexSpecBO(List.of("next_attempt_at", "id"), false),
            "idx_outbox_reclaim", new IndexSpecBO(List.of("locked_until", "id"), false),
            "idx_outbox_cleanup", new IndexSpecBO(List.of("completed_at", "id"), false)
    );

    private static final Map<String, IndexSpecBO> HISTORY_INDEXES = Map.of(
            "pk_ddl_history", new IndexSpecBO(List.of("script", "type"), true),
            "uk_ddl_history_type_version", new IndexSpecBO(List.of("type", "version"), true)
    );

    @Qualifier("outboxManagedDdlInitializer")
    private final OutboxManagedDdlInitializer ddlInitializer;

    @Qualifier("egonColaShardingRouteFingerprint")
    private final String routeFingerprint;

    @Qualifier("outboxMpStorageProperties")
    private final OutboxMpStorageProperties storageProperties;

    @Qualifier("outboxStateMachineObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    private final ResourcePatternResolver resources = new PathMatchingResourcePatternResolver();

    public void validate() {
        DataSource primary = ddlInitializer.physicalMetadataDataSource(routeFingerprint);
        try (Connection connection = primary.getConnection()) {
            connection.setReadOnly(true);
            DatabaseMetaData metadata = connection.getMetaData();
            if (!"PostgreSQL".equals(metadata.getDatabaseProductName())) {
                throw schemaFailure("POSTGRESQL_REQUIRED");
            }
            validateColumns(metadata, MESSAGE_TABLE, MESSAGE_COLUMNS);
            validateColumns(metadata, HISTORY_TABLE, HISTORY_COLUMNS);
            validateIndexes(metadata, MESSAGE_TABLE, MESSAGE_INDEXES);
            validateIndexes(metadata, HISTORY_TABLE, HISTORY_INDEXES);
            validateMessageConstraints(connection);
            validateHistoryConstraints(connection);
            validateHistoryPrefix(connection, loadManifest());
        } catch (SQLException | IOException failure) {
            throw new OutboxConfigurationException("Transactional outbox schema metadata validation failed", failure);
        }
        log.info("outbox schema metadata validated: schema={} routeFingerprint={}", SCHEMA, routeFingerprint);
    }

    private static void validateColumns(DatabaseMetaData metadata, String table, Map<String, ColumnSpecBO> expected)
            throws SQLException {
        Map<String, ColumnSpecBO> actual = new HashMap<>();
        try (ResultSet columns = metadata.getColumns(null, SCHEMA, table, null)) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME").toLowerCase(Locale.ROOT);
                String type = columns.getString("TYPE_NAME").toLowerCase(Locale.ROOT);
                int length = columns.getInt("COLUMN_SIZE");
                int precision = columns.getInt("DECIMAL_DIGITS");
                int nullable = columns.getInt("NULLABLE");
                actual.put(name, new ColumnSpecBO(Set.of(type), length, precision,
                        nullable != DatabaseMetaData.columnNoNulls));
            }
        }
        if (!actual.keySet().equals(expected.keySet())) {
            throw schemaFailure("COLUMN_SET_MISMATCH: " + table);
        }
        expected.forEach((name, definition) -> {
            ColumnSpecBO found = actual.get(name);
            if (!definition.databaseTypes().containsAll(found.databaseTypes())
                    || (definition.maximumLength() > 0 && definition.maximumLength() != found.maximumLength())
                    || (definition.precision() > 0 && definition.precision() != found.precision())
                    || definition.nullable() != found.nullable()) {
                throw schemaFailure("COLUMN_DEFINITION_MISMATCH: " + table + '.' + name);
            }
        });
    }

    private static void validateIndexes(DatabaseMetaData metadata, String table, Map<String, IndexSpecBO> expected)
            throws SQLException {
        Map<String, Map<Short, String>> columns = new HashMap<>();
        Map<String, Boolean> unique = new HashMap<>();
        try (ResultSet indexes = metadata.getIndexInfo(null, SCHEMA, table, false, false)) {
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                String column = indexes.getString("COLUMN_NAME");
                if (name == null || column == null) {
                    continue;
                }
                unique.put(name, !indexes.getBoolean("NON_UNIQUE"));
                columns.computeIfAbsent(name, ignored -> new TreeMap<>())
                        .put(indexes.getShort("ORDINAL_POSITION"), column.toLowerCase(Locale.ROOT));
            }
        }
        expected.forEach((name, definition) -> {
            Map<Short, String> actualColumns = columns.get(name);
            if (actualColumns == null || !List.copyOf(actualColumns.values()).equals(definition.columns())
                    || !Boolean.valueOf(definition.unique()).equals(unique.get(name))) {
                throw schemaFailure("INDEX_DEFINITION_MISMATCH: " + table + '.' + name);
            }
        });
    }

    private void validateMessageConstraints(Connection connection) throws SQLException {
        Map<String, String> definitions = constraintDefinitions(connection, MESSAGE_TABLE);
        requireConstraint(definitions, "pk_outbox_message", "primary key (id)");
        requireConstraint(definitions, "ck_outbox_status", "pending", "processing", "retry_wait", "succeeded", "dead");
        requireConstraint(definitions, "ck_outbox_attempt_count", "attempt_count", "0");
        requireConstraint(definitions, "ck_outbox_max_attempts", "max_attempts", "1");
        requireConstraint(definitions, "ck_outbox_technical_tenant", "tenant_id", "0");
        requireConstraint(definitions, "ck_outbox_not_soft_deleted", "deleted_at", "null");
        requireConstraint(definitions, "ck_outbox_version", "version", "0");
    }

    private void validateHistoryConstraints(Connection connection) throws SQLException {
        Map<String, String> definitions = constraintDefinitions(connection, HISTORY_TABLE);
        requireConstraint(definitions, "pk_ddl_history", "primary key", "script", "type");
        requireConstraint(definitions, "uk_ddl_history_type_version", "unique", "type", "version");
        requireConstraint(definitions, "ddl_history_tenant_id_check", "tenant_id", "0");
        requireConstraint(definitions, "ddl_history_type_check", "type", "sql");
        requireConstraint(definitions, "ddl_history_checksum_check", "checksum", "64");
        requireConstraint(definitions, "ddl_history_execution_ms_check", "execution_ms", "0");
        requireConstraint(definitions, "ddl_history_route_fingerprint_check", "route_fingerprint", "64");
    }

    private void validateHistoryPrefix(Connection connection, EgonColaDdlManifestBO manifest) throws SQLException {
        if (!"component-outbox".equals(manifest.family())) {
            throw schemaFailure("DDL_HISTORY_FAMILY_MISMATCH");
        }
        int index = 0;
        try (var statement = connection.prepareStatement("SELECT script,type,version,checksum,route_fingerprint,tenant_id "
                + "FROM egon_outbox.ddl_history ORDER BY version");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                if (index >= manifest.scripts().size()) {
                    throw schemaFailure("DDL_HISTORY_PREFIX_MISMATCH");
                }
                EgonColaDdlManifestBO.ScriptBO script = manifest.scripts().get(index);
                if (!script.path().equals(rows.getString(1)) || !"SQL".equals(rows.getString(2))
                        || !script.version().equals(rows.getString(3)) || !script.sha256().equals(rows.getString(4))
                        || !routeFingerprint.equals(rows.getString(5)) || rows.getLong(6) != 0) {
                    throw schemaFailure("DDL_HISTORY_ENTRY_MISMATCH: " + script.version());
                }
                index++;
            }
        }
        if (index != manifest.scripts().size()) {
            throw schemaFailure("DDL_HISTORY_PREFIX_INCOMPLETE");
        }
    }

    private Map<String, String> constraintDefinitions(Connection connection, String table) throws SQLException {
        Map<String, String> definitions = new HashMap<>();
        try (var statement = connection.prepareStatement("SELECT c.conname, pg_catalog.pg_get_constraintdef(c.oid) "
                + "FROM pg_catalog.pg_constraint c JOIN pg_catalog.pg_class t ON t.oid = c.conrelid "
                + "JOIN pg_catalog.pg_namespace n ON n.oid = t.relnamespace WHERE n.nspname = ? AND t.relname = ?")) {
            statement.setString(1, SCHEMA);
            statement.setString(2, table);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    definitions.put(rows.getString(1), rows.getString(2).toLowerCase(Locale.ROOT));
                }
            }
        }
        return definitions;
    }

    private void requireConstraint(Map<String, String> definitions, String name, String... requiredParts) {
        String definition = definitions.get(name);
        if (definition == null) {
            throw schemaFailure("CONSTRAINT_MISSING: " + name);
        }
        for (String requiredPart : requiredParts) {
            if (!definition.contains(requiredPart.toLowerCase(Locale.ROOT))) {
                throw schemaFailure("CONSTRAINT_DEFINITION_MISMATCH: " + name);
            }
        }
    }

    private EgonColaDdlManifestBO loadManifest() throws IOException {
        Resource[] matches = resources.getResources("classpath*:" + storageProperties.getManifestResource());
        if (matches.length != 1) {
            throw schemaFailure("DDL_MANIFEST_RESOURCE_AMBIGUOUS");
        }
        try (var input = matches[0].getInputStream()) {
            return validationUtils.validate(objectMapper.readValue(input, EgonColaDdlManifestBO.class));
        }
    }

    private static ColumnSpecBO column(Set<String> types, int maximumLength, int precision, boolean nullable) {
        return new ColumnSpecBO(types, maximumLength, precision, nullable);
    }

    private static OutboxConfigurationException schemaFailure(String reason) {
        return new OutboxConfigurationException("Transactional outbox schema is missing or incompatible: " + reason);
    }

    private record ColumnSpecBO(Set<String> databaseTypes, int maximumLength, int precision, boolean nullable) {
    }

    private record IndexSpecBO(List<String> columns, boolean unique) {
    }
}
