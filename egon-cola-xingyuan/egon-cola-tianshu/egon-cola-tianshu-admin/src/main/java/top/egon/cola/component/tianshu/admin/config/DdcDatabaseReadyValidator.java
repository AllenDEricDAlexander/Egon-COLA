package top.egon.cola.component.tianshu.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer;
import top.egon.cola.component.outbox.persistence.OutboxSchemaMetadataValidator;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Fails application startup unless the managed Tianshu and Outbox schemas are already ready. */
@Slf4j
@Component("ddcDatabaseReadyValidator")
@RequiredArgsConstructor
public class DdcDatabaseReadyValidator implements SmartInitializingSingleton {

    private static final String TIANSHU_MANIFEST = "db/egon-mp/repository-manifest.json";
    private static final List<String> TIANSHU_TABLES = List.of(
            "ddc_biz_t0", "ddc_env_t0", "ddc_app_t0", "ddc_namespace_t0", "ddc_namespace_env_app_t0",
            "ddc_config_item_t0", "ddc_config_version_t0", "ddc_publish_task_t0", "ddc_publish_ack_t0",
            "ddc_instance_t0", "ddc_operation_log_t0", "ddc_ai_resource_t0", "ddc_ai_version_t0",
            "ddc_ai_draft_node_t0", "ddc_ai_version_node_t0", "ddc_ai_publication_t0", "ddl_history");
    private static final Map<String, Set<String>> TABLE_COLUMNS = Map.ofEntries(
            Map.entry("ddc_biz_t0", Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version", "public_id", "biz_code", "biz_name", "description",
                    "enabled", "created_at", "updated_at")),
            Map.entry("ddc_env_t0", Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version", "public_id", "env_code", "description", "sort_order",
                    "enabled", "created_at", "updated_at")),
            Map.entry("ddc_app_t0", Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version", "public_id", "app_code", "app_name", "owner",
                    "description", "enabled", "created_at", "updated_at", "biz_code")),
            Map.entry("ddc_namespace_t0", Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version", "public_id", "namespace", "description", "enabled",
                    "created_at", "updated_at", "namespace_code", "biz_code")),
            Map.entry("ddc_namespace_env_app_t0", Set.of("id", "tenant_id", "create_user_id", "create_time",
                    "update_user_id", "update_time", "deleted_at", "version", "public_id", "namespace_id",
                    "env_code", "app_id", "enabled", "created_at", "updated_at")),
            Map.entry("ddc_config_item_t0", Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version", "public_id", "app_code", "env", "namespace", "config_key",
                    "config_value", "default_value", "value_type", "current_version", "description", "enabled", "deleted",
                    "created_at", "updated_at", "published_version", "biz_code")),
            Map.entry("ddc_config_version_t0", Set.of("id", "tenant_id", "create_user_id", "create_time",
                    "update_user_id", "update_time", "deleted_at", "version", "public_id", "config_id", "app_code",
                    "env", "namespace", "config_key", "config_revision", "old_value", "new_value", "value_type",
                    "change_type", "change_reason", "operator", "operator_ip", "created_at", "biz_code")),
            Map.entry("ddc_publish_task_t0", Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version", "public_id", "change_id", "config_id", "app_code", "env",
                    "namespace", "config_key", "target_version", "publish_mode", "status", "target_count", "ack_count",
                    "failed_count", "ignored_count", "timeout_count", "timeout_ms", "operator", "error_message",
                    "created_at", "updated_at", "content_checksum", "attempt_count", "dispatched_at", "completed_at",
                    "failure_stage", "biz_code")),
            Map.entry("ddc_publish_ack_t0", Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version", "public_id", "change_id", "instance_id", "app_code", "env",
                    "namespace", "config_key", "target_version", "current_version", "ack_status", "error_message",
                    "ack_at", "lease_id", "content_checksum", "biz_code")),
            Map.entry("ddc_instance_t0", Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version", "public_id", "instance_id", "app_code", "env", "namespace",
                    "host", "port", "pid", "sdk_version", "status", "last_heartbeat_at", "created_at", "updated_at",
                    "lease_id", "lease_expire_at", "runtime_metadata", "biz_code", "resource_server_id", "resource_version",
                    "credential_id", "admission_expires_at")),
            Map.entry("ddc_operation_log_t0", Set.of("id", "tenant_id", "create_user_id", "create_time",
                    "update_user_id", "update_time", "deleted_at", "version", "public_id", "app_code", "env", "namespace",
                    "config_key", "operation_type", "operator", "operator_ip", "operation_content", "created_at", "biz_code")),
            Map.entry("ddc_ai_resource_t0", Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version", "biz_code", "namespace_id", "env_code", "app_code", "kind",
                    "resource_key", "display_name", "description", "draft_prompt", "last_version_no", "current_version_id")),
            Map.entry("ddc_ai_version_t0", Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version", "resource_id", "version_no", "kind", "display_name",
                    "description", "prompt_text", "content_sha256", "content_bytes", "node_count", "source_version_id",
                    "change_note")),
            Map.entry("ddc_ai_draft_node_t0", Set.of("id", "tenant_id", "create_user_id", "create_time",
                    "update_user_id", "update_time", "deleted_at", "version", "resource_id", "parent_path", "path",
                    "node_type", "content", "content_sha256", "content_bytes")),
            Map.entry("ddc_ai_version_node_t0", Set.of("id", "tenant_id", "create_user_id", "create_time",
                    "update_user_id", "update_time", "deleted_at", "version", "version_id", "parent_path", "path",
                    "node_type", "content", "content_sha256", "content_bytes")),
            Map.entry("ddc_ai_publication_t0", Set.of("id", "tenant_id", "create_user_id", "create_time",
                    "update_user_id", "update_time", "deleted_at", "version", "resource_id", "operation", "request_id",
                    "request_sha256", "previous_version_id", "target_version_id", "source_version_id", "result_revision",
                    "target_created")),
            Map.entry("ddl_history", Set.of("tenant_id", "script", "type", "version", "checksum", "installed_on",
                    "execution_ms", "route_fingerprint")));
    private static final Map<String, IndexSpecBO> LIFECYCLE_INDEXES = Map.ofEntries(
            Map.entry("uk_biz_lifecycle", new IndexSpecBO("ddc_biz_t0", List.of("tenant_id", "biz_code", "deleted_at"))),
            Map.entry("uk_biz_live", new IndexSpecBO("ddc_biz_t0", List.of("tenant_id", "biz_code"))),
            Map.entry("uk_env_lifecycle", new IndexSpecBO("ddc_env_t0", List.of("tenant_id", "env_code", "deleted_at"))),
            Map.entry("uk_env_live", new IndexSpecBO("ddc_env_t0", List.of("tenant_id", "env_code"))),
            Map.entry("uk_app_lifecycle", new IndexSpecBO("ddc_app_t0", List.of("tenant_id", "app_code", "deleted_at"))),
            Map.entry("uk_app_live", new IndexSpecBO("ddc_app_t0", List.of("tenant_id", "app_code"))),
            Map.entry("uk_namespace_lifecycle", new IndexSpecBO("ddc_namespace_t0",
                    List.of("tenant_id", "biz_code", "namespace_code", "deleted_at"))),
            Map.entry("uk_namespace_live", new IndexSpecBO("ddc_namespace_t0",
                    List.of("tenant_id", "biz_code", "namespace_code"))),
            Map.entry("uk_namespace_env_app_lifecycle", new IndexSpecBO("ddc_namespace_env_app_t0",
                    List.of("tenant_id", "namespace_id", "env_code", "app_id", "deleted_at"))),
            Map.entry("uk_namespace_env_app_live", new IndexSpecBO("ddc_namespace_env_app_t0",
                    List.of("tenant_id", "namespace_id", "env_code", "app_id"))),
            Map.entry("uk_config_item_lifecycle", new IndexSpecBO("ddc_config_item_t0",
                    List.of("tenant_id", "biz_code", "env", "app_code", "config_key", "deleted_at"))),
            Map.entry("uk_config_item_live", new IndexSpecBO("ddc_config_item_t0",
                    List.of("tenant_id", "biz_code", "env", "app_code", "config_key"))),
            Map.entry("uk_ai_resource_lifecycle", new IndexSpecBO("ddc_ai_resource_t0",
                    List.of("tenant_id", "biz_code", "namespace_id", "env_code", "app_code", "kind", "resource_key",
                            "deleted_at"))),
            Map.entry("uk_ai_resource_live", new IndexSpecBO("ddc_ai_resource_t0",
                    List.of("tenant_id", "biz_code", "namespace_id", "env_code", "app_code", "kind", "resource_key"))),
            Map.entry("uk_ai_draft_node_lifecycle", new IndexSpecBO("ddc_ai_draft_node_t0",
                    List.of("tenant_id", "resource_id", "path", "deleted_at"))),
            Map.entry("uk_ai_draft_node_live", new IndexSpecBO("ddc_ai_draft_node_t0",
                    List.of("tenant_id", "resource_id", "path"))),
            Map.entry("uk_instance_business", new IndexSpecBO("ddc_instance_t0",
                    List.of("tenant_id", "instance_id"))));
    private static final Map<String, Set<String>> TABLE_CONSTRAINTS = Map.ofEntries(
            Map.entry("ddc_biz_t0", Set.of("pk_biz", "uk_biz_tenant_id", "uk_biz_public")),
            Map.entry("ddc_env_t0", Set.of("pk_env", "uk_env_tenant_id", "uk_env_public")),
            Map.entry("ddc_app_t0", Set.of("pk_app", "uk_app_tenant_id", "uk_app_public")),
            Map.entry("ddc_namespace_t0", Set.of("pk_namespace", "uk_namespace_tenant_id", "uk_namespace_public")),
            Map.entry("ddc_namespace_env_app_t0", Set.of("pk_namespace_env_app", "uk_namespace_env_app_tenant_id",
                    "uk_namespace_env_app_public", "fk_namespace_env_app_namespace", "fk_namespace_env_app_app")),
            Map.entry("ddc_config_item_t0", Set.of("pk_config_item", "uk_config_item_tenant_id", "uk_config_item_public")),
            Map.entry("ddc_config_version_t0", Set.of("pk_config_version", "uk_config_version_tenant_id",
                    "uk_config_version_public", "uk_config_version_business", "fk_config_version_item")),
            Map.entry("ddc_publish_task_t0", Set.of("pk_publish_task", "uk_publish_task_tenant_id",
                    "uk_publish_task_public", "uk_publish_task_business", "fk_publish_task_config")),
            Map.entry("ddc_publish_ack_t0", Set.of("pk_publish_ack", "uk_publish_ack_tenant_id",
                    "uk_publish_ack_public", "uk_publish_ack_target")),
            Map.entry("ddc_instance_t0", Set.of("pk_instance", "uk_instance_tenant_id", "uk_instance_public")),
            Map.entry("ddc_operation_log_t0", Set.of("pk_operation_log", "uk_operation_log_tenant_id",
                    "uk_operation_log_public")),
            Map.entry("ddc_ai_resource_t0", Set.of("pk_ai_resource", "uk_ai_resource_tenant_id", "uk_ai_resource_kind",
                    "fk_ai_resource_namespace", "ck_ai_resource_key", "ck_ai_resource_draft_prompt",
                    "fk_ai_resource_current_version")),
            Map.entry("ddc_ai_version_t0", Set.of("pk_ai_version", "uk_ai_version_tenant_id", "uk_ai_version_no",
                    "uk_ai_version_owner", "uk_ai_version_kind", "fk_ai_version_resource", "fk_ai_version_source",
                    "ck_ai_version_content")),
            Map.entry("ddc_ai_draft_node_t0", Set.of("pk_ai_draft_node", "uk_ai_draft_node_tenant_id",
                    "fk_ai_draft_node_resource", "ck_ai_draft_node_path", "ck_ai_draft_node_content")),
            Map.entry("ddc_ai_version_node_t0", Set.of("pk_ai_version_node", "uk_ai_version_node_tenant_id",
                    "fk_ai_version_node_version", "fk_ai_version_node_parent", "ck_ai_version_node_path",
                    "ck_ai_version_node_content")),
            Map.entry("ddc_ai_publication_t0", Set.of("pk_ai_publication", "uk_ai_publication_tenant_id",
                    "uk_ai_publication_request", "fk_ai_publication_resource", "fk_ai_publication_previous",
                    "fk_ai_publication_target", "fk_ai_publication_source", "ck_ai_publication_source")),
            Map.entry("ddl_history", Set.of("pk_ddl_history", "uk_ddl_history_type_version")));

    @Qualifier("outboxManagedDdlInitializer")
    private final OutboxManagedDdlInitializer outboxDdlInitializer;

    @Qualifier("outboxSchemaMetadataValidator")
    private final OutboxSchemaMetadataValidator outboxSchemaMetadataValidator;

    @Qualifier("egonColaShardingRouteFingerprint")
    private final String routeFingerprint;

    @Qualifier("outboxStateMachineObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    private final ResourcePatternResolver resources = new PathMatchingResourcePatternResolver();

    @Override
    public void afterSingletonsInstantiated() {
        DataSource primary = outboxDdlInitializer.physicalMetadataDataSource(routeFingerprint);
        EgonColaDdlManifestBO manifest = loadTianshuManifest();
        validateTianshuSchema(primary, routeFingerprint, manifest);
        outboxSchemaMetadataValidator.validate();
        log.info("Tianshu database readiness validated: tables={} routeFingerprint={}",
                TIANSHU_TABLES.size() - 1, routeFingerprint);
    }

    /** Validates the Tianshu history and relations through the same read-only physical PRIMARY. */
    public static void validateTianshuSchema(DataSource primary, String routeFingerprint,
                                              EgonColaDdlManifestBO manifest) {
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(manifest, "manifest");
        if (routeFingerprint == null || !routeFingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("TIANSHU_DDL_ROUTE_FINGERPRINT_REQUIRED");
        }
        if (!"tianshu".equals(manifest.family()) || manifest.scripts().isEmpty()) {
            throw new IllegalArgumentException("TIANSHU_DDL_MANIFEST_INVALID");
        }
        validateManifestResourceChecksum(manifest);

        try (Connection connection = primary.getConnection()) {
            connection.setSchema("public");
            connection.setReadOnly(true);
            DatabaseMetaData metadata = connection.getMetaData();
            if (!"PostgreSQL".equals(metadata.getDatabaseProductName())) {
                throw new IllegalStateException("TIANSHU_POSTGRESQL_REQUIRED");
            }
            validatePrimaryAndSchema(connection);
            validateTables(metadata);
            validateColumns(metadata);
            validateLifecycleIndexes(metadata);
            validateLifecycleIndexPredicates(connection);
            validateNamedConstraints(connection);
            validateHistory(connection, manifest, routeFingerprint);
        } catch (SQLException failure) {
            throw new IllegalStateException("TIANSHU_DATABASE_SCHEMA_NOT_READY", failure);
        }
    }

    private EgonColaDdlManifestBO loadTianshuManifest() {
        String resourcePath = "classpath*:" + TIANSHU_MANIFEST;
        try {
            var matches = resources.getResources(resourcePath);
            if (matches.length != 1) {
                throw new IllegalStateException("TIANSHU_DDL_MANIFEST_RESOURCE_AMBIGUOUS");
            }
            try (var input = matches[0].getInputStream()) {
                EgonColaDdlManifestBO manifest = objectMapper.readValue(input, EgonColaDdlManifestBO.class);
                validationUtils.validate(manifest);
                return manifest;
            }
        } catch (IOException failure) {
            throw new IllegalStateException("TIANSHU_DDL_MANIFEST_UNREADABLE", failure);
        }
    }

    private static void validatePrimaryAndSchema(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT current_schema(), pg_is_in_recovery()")) {
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || !"public".equals(row.getString(1))) {
                    throw new IllegalStateException("TIANSHU_PUBLIC_SCHEMA_REQUIRED");
                }
                if (row.getBoolean(2)) {
                    throw new IllegalStateException("TIANSHU_PRIMARY_REQUIRED");
                }
            }
        }
    }

    private static void validateTables(DatabaseMetaData metadata) throws SQLException {
        for (String table : TIANSHU_TABLES) {
            try (ResultSet relations = metadata.getTables(null, "public", table, new String[]{"TABLE"})) {
                if (!relations.next() || !table.equals(relations.getString("TABLE_NAME"))) {
                    throw new IllegalStateException("TIANSHU_SCHEMA_TABLE_MISSING: " + table);
                }
            }
        }
    }

    private static void validateColumns(DatabaseMetaData metadata) throws SQLException {
        for (String table : TIANSHU_TABLES) {
            Set<String> actual = new LinkedHashSet<>();
            try (ResultSet columns = metadata.getColumns(null, "public", table, null)) {
                while (columns.next()) {
                    actual.add(columns.getString("COLUMN_NAME").toLowerCase(java.util.Locale.ROOT));
                }
            }
            if (!TABLE_COLUMNS.get(table).equals(actual)) {
                throw new IllegalStateException("TIANSHU_SCHEMA_COLUMN_SET_MISMATCH: " + table);
            }
        }
    }

    private static void validateLifecycleIndexes(DatabaseMetaData metadata) throws SQLException {
        Map<String, Map<String, IndexDefinitionBO>> byTable = new HashMap<>();
        for (String table : LIFECYCLE_INDEXES.values().stream().map(IndexSpecBO::table).distinct().toList()) {
            Map<String, Map<Short, String>> columns = new HashMap<>();
            Map<String, Boolean> unique = new HashMap<>();
            try (ResultSet indexes = metadata.getIndexInfo(null, "public", table, false, false)) {
                while (indexes.next()) {
                    String name = indexes.getString("INDEX_NAME");
                    String column = indexes.getString("COLUMN_NAME");
                    if (name == null || column == null) {
                        continue;
                    }
                    unique.put(name, !indexes.getBoolean("NON_UNIQUE"));
                    columns.computeIfAbsent(name, ignored -> new TreeMap<>())
                            .put(indexes.getShort("ORDINAL_POSITION"), column.toLowerCase(java.util.Locale.ROOT));
                }
            }
            Map<String, IndexDefinitionBO> definitions = new HashMap<>();
            columns.forEach((name, orderedColumns) -> definitions.put(name,
                    new IndexDefinitionBO(List.copyOf(orderedColumns.values()), Boolean.TRUE.equals(unique.get(name)))));
            byTable.put(table, definitions);
        }
        LIFECYCLE_INDEXES.forEach((name, expected) -> {
            IndexDefinitionBO actual = byTable.getOrDefault(expected.table(), Map.of()).get(name);
            if (actual == null || !actual.unique() || !expected.columns().equals(actual.columns())) {
                throw new IllegalStateException("TIANSHU_SCHEMA_INDEX_MISMATCH: " + name);
            }
        });
    }

    private static void validateNamedConstraints(Connection connection) throws SQLException {
        String tables = TIANSHU_TABLES.stream().map(table -> "'" + table + "'")
                .collect(java.util.stream.Collectors.joining(","));
        String sql = "SELECT t.relname,c.conname FROM pg_catalog.pg_constraint c "
                + "JOIN pg_catalog.pg_class t ON t.oid=c.conrelid "
                + "JOIN pg_catalog.pg_namespace n ON n.oid=t.relnamespace "
                + "WHERE n.nspname='public' AND t.relname IN (" + tables + ")";
        Map<String, Set<String>> actual = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                actual.computeIfAbsent(rows.getString(1), ignored -> new LinkedHashSet<>()).add(rows.getString(2));
            }
        }
        TABLE_CONSTRAINTS.forEach((table, expected) -> {
            Set<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(actual.getOrDefault(table, Set.of()));
            if (!missing.isEmpty()) {
                throw new IllegalStateException("TIANSHU_SCHEMA_CONSTRAINT_MISSING: "
                        + table + '.' + missing.iterator().next());
            }
        });
    }

    private static void validateLifecycleIndexPredicates(Connection connection) throws SQLException {
        String names = LIFECYCLE_INDEXES.keySet().stream().filter(name -> name.endsWith("_live"))
                .map(name -> "'" + name + "'").collect(java.util.stream.Collectors.joining(","));
        String sql = "SELECT i.relname, pg_catalog.pg_get_expr(x.indpred, x.indrelid) "
                + "FROM pg_catalog.pg_index x JOIN pg_catalog.pg_class i ON i.oid=x.indexrelid "
                + "JOIN pg_catalog.pg_class t ON t.oid=x.indrelid "
                + "JOIN pg_catalog.pg_namespace n ON n.oid=t.relnamespace "
                + "WHERE n.nspname='public' AND i.relname IN (" + names + ")";
        Map<String, String> actual = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                actual.put(rows.getString(1), rows.getString(2));
            }
        }
        for (String name : LIFECYCLE_INDEXES.keySet().stream().filter(index -> index.endsWith("_live")).toList()) {
            String predicate = actual.get(name);
            String normalized = predicate == null ? "" : predicate.toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("[()\\s]+", "");
            if (!"deleted_atisnull".equals(normalized)) {
                throw new IllegalStateException("TIANSHU_SCHEMA_INDEX_PREDICATE_MISMATCH: " + name);
            }
        }
    }

    private static void validateHistory(Connection connection, EgonColaDdlManifestBO manifest,
                                        String routeFingerprint) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT script,type,version,checksum,route_fingerprint,tenant_id "
                        + "FROM public.ddl_history ORDER BY version");
             ResultSet rows = statement.executeQuery()) {
            List<EgonColaDdlManifestBO.ScriptBO> scripts = manifest.scripts();
            int index = 0;
            boolean first = true;
            while (rows.next()) {
                if (first) {
                    index = manifest.startingIndex(rows.getString(3));
                    first = false;
                }
                if (index >= scripts.size()) {
                    throw new IllegalStateException("TIANSHU_DDL_HISTORY_PREFIX_MISMATCH");
                }
                EgonColaDdlManifestBO.ScriptBO script = scripts.get(index++);
                if (!script.path().equals(rows.getString(1)) || !"SQL".equals(rows.getString(2))
                        || !script.version().equals(rows.getString(3)) || !script.sha256().equals(rows.getString(4))
                        || !routeFingerprint.equals(rows.getString(5)) || rows.getLong(6) != 0) {
                    throw new IllegalStateException("TIANSHU_DDL_HISTORY_PREFIX_MISMATCH");
                }
            }
            if (first || index != scripts.size()) {
                throw new IllegalStateException("TIANSHU_DDL_HISTORY_INCOMPLETE");
            }
        }
    }

    private static void validateManifestResourceChecksum(EgonColaDdlManifestBO manifest) {
        try {
            for (EgonColaDdlManifestBO.ScriptBO script : manifest.scripts()) {
                var matches = DdcDatabaseReadyValidator.class.getClassLoader().getResources(script.path());
                if (!matches.hasMoreElements()) {
                    throw new IllegalStateException("TIANSHU_DDL_MANIFEST_RESOURCE_MISSING");
                }
                var resource = matches.nextElement();
                if (matches.hasMoreElements()) {
                    throw new IllegalStateException("TIANSHU_DDL_MANIFEST_RESOURCE_AMBIGUOUS");
                }
                try (InputStream input = resource.openStream()) {
                    String actual = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                            .digest(input.readAllBytes()));
                    if (!script.sha256().equals(actual)) {
                        throw new IllegalStateException("TIANSHU_DDL_MANIFEST_RESOURCE_CHECKSUM_MISMATCH");
                    }
                }
            }
        } catch (IOException failure) {
            throw new IllegalStateException("TIANSHU_DDL_MANIFEST_RESOURCE_UNREADABLE", failure);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private record IndexSpecBO(String table, List<String> columns) {
    }

    private record IndexDefinitionBO(List<String> columns, boolean unique) {
    }
}
