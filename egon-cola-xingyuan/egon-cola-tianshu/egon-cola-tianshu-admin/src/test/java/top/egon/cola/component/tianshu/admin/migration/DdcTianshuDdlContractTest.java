package top.egon.cola.component.tianshu.admin.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.tianshu.admin.config.DdcDatabaseReadyValidator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcTianshuDdlContractTest {

    private static final String TIANSHU_MANIFEST = "db/egon-mp/repository-manifest.json";
    private static final String OUTBOX_MANIFEST = "db/egon-outbox-mp/manifest.json";
    private static final String TIANSHU_SQL = "db/egon-mp/V20260922_001__tianshu_platform.sql";
    private static final String TIANSHU_INSTANCE_KEY_SQL =
            "db/egon-mp/V20260928_001__tianshu_instance_business_key.sql";
    private static final String TIANSHU_AI_OPTIMIZATION_SQL =
            "db/egon-mp/V20261017_001__tianshu_ai_feedback_optimization.sql";
    private static final Set<String> TIANSHU_AI_OPTIMIZATION_TABLES = Set.of(
            "ddc_ai_usage_feedback_t0", "ddc_ai_optimization_policy_t0", "ddc_ai_optimization_run_t0");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?im)^\\s*CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:public\\.)?([a-z0-9_]+)");
    private static final Set<String> TIANSHU_TABLES = Set.of(
            "ddc_biz_t0", "ddc_env_t0", "ddc_app_t0", "ddc_namespace_t0", "ddc_namespace_env_app_t0",
            "ddc_config_item_t0", "ddc_config_version_t0", "ddc_publish_task_t0", "ddc_publish_ack_t0",
            "ddc_instance_t0", "ddc_operation_log_t0", "ddc_ai_resource_t0", "ddc_ai_version_t0",
            "ddc_ai_draft_node_t0", "ddc_ai_version_node_t0", "ddc_ai_publication_t0", "ddl_history");
    private static final Map<String, String> LIFECYCLE_INDEX_TABLES = Map.ofEntries(
            Map.entry("uk_biz_lifecycle", "ddc_biz_t0"),
            Map.entry("uk_env_lifecycle", "ddc_env_t0"),
            Map.entry("uk_app_lifecycle", "ddc_app_t0"),
            Map.entry("uk_namespace_lifecycle", "ddc_namespace_t0"),
            Map.entry("uk_namespace_env_app_lifecycle", "ddc_namespace_env_app_t0"),
            Map.entry("uk_config_item_lifecycle", "ddc_config_item_t0"),
            Map.entry("uk_ai_resource_lifecycle", "ddc_ai_resource_t0"),
            Map.entry("uk_ai_draft_node_lifecycle", "ddc_ai_draft_node_t0"));
    private static final Set<String> LEGACY_PUBLIC_ID_TABLES = Set.of(
            "ddc_biz_t0", "ddc_env_t0", "ddc_app_t0", "ddc_namespace_t0", "ddc_namespace_env_app_t0",
            "ddc_config_item_t0", "ddc_config_version_t0", "ddc_publish_task_t0", "ddc_publish_ack_t0",
            "ddc_instance_t0", "ddc_operation_log_t0");
    private static final Map<String, String> LEGACY_SCRIPT_SHA256 = Map.ofEntries(
            Map.entry("db/postgresql/V1__create_ddc_schema.sql", "fb4363236774caf9b61d04d4f99072cca9eeee84a57a946269eb79345186fdec"),
            Map.entry("db/postgresql/V2__add_lease_and_sync_publish.sql", "979b11f97306b54a1b4b7e6fb20b390f468e1ae9b6ae043c5cee786db5ac335d"),
            Map.entry("db/postgresql/V3__add_instance_runtime_metadata.sql", "93f21b47822f78e2271a5746cdafbd71557360d401ba1017034b501a39b73e8f"),
            Map.entry("db/postgresql/V4__add_published_config_pointer.sql", "bba1d16d0a95dbfb13a0a235eca3a238167b5100f71010f638d82cbc7f8c4deb"),
            Map.entry("db/postgresql/V5__add_biz_env_and_detach_namespace_env.sql", "e1a15b8db7f281329f6a4369c8e8eeaa024bb100a5ada5fda50de7b97e157d75"),
            Map.entry("db/postgresql/V6__add_namespace_code.sql", "eaca99b4047aaee02d8ef96c63fc1aecd4fe5e86097452c129e9b00763b9ade8"),
            Map.entry("db/postgresql/V7__add_namespace_env_app_visibility.sql", "b291e74e373d6742f550f9516c9510f99ca022e09fd8a8e7eac3df20975185c9"),
            Map.entry("db/postgresql/V8__add_resource_admission_audit.sql", "2403954eff1814df65269735511fb2e611c9c113f1dc32917b7491b54c06f28f"),
            Map.entry("db/postgresql/V9__enforce_global_biz_app_codes.sql", "4201410e91f7886c36a3f2de209bcbdff22222695ace0fc0203661fdf16a654c"),
            Map.entry("db/sqlite/V1__create_ddc_schema.sql", "69a1b6d8096da10a6ce9c1ac7e567fa3d449481debce85fd30ca4869e866205d"),
            Map.entry("db/sqlite/V2__add_lease_and_sync_publish.sql", "a68dfc7925ecdca9aff590e3d859066f6d80eefa726f9120a07a90e583c15894"),
            Map.entry("db/sqlite/V3__add_instance_runtime_metadata.sql", "93f21b47822f78e2271a5746cdafbd71557360d401ba1017034b501a39b73e8f"),
            Map.entry("db/sqlite/V4__add_published_config_pointer.sql", "9a27ad7038678660728d11c92b91a0e6df5372b7e2855ea3198934256644374f"),
            Map.entry("db/sqlite/V5__add_biz_env_and_detach_namespace_env.sql", "b85ead9469677bdff9a7f0e8506b2930fa65ec20a25ddbbfb7cc047c6d1c3ddf"),
            Map.entry("db/sqlite/V6__add_namespace_code.sql", "e1ecbcfb28c6490306128c0e0f45f1a4721ab01dff921b270cd06bfccce8b01f"),
            Map.entry("db/sqlite/V7__add_namespace_env_app_visibility.sql", "035133c0985082e9df15d8fa2a59e5e3cf394268d4491093139b91514d535ab9"),
            Map.entry("db/sqlite/V8__add_resource_admission_audit.sql", "2403954eff1814df65269735511fb2e611c9c113f1dc32917b7491b54c06f28f"),
            Map.entry("db/sqlite/V9__enforce_global_biz_app_codes.sql", "4201410e91f7886c36a3f2de209bcbdff22222695ace0fc0203661fdf16a654c"));

    @Test
    void tianshuManifestKeepsV001AndAppendsV002WithExactChecksums() throws Exception {
        JsonNode manifest = OBJECT_MAPPER.readTree(requiredResource(TIANSHU_MANIFEST));
        EgonColaDdlManifestBO typedManifest = OBJECT_MAPPER.treeToValue(manifest, EgonColaDdlManifestBO.class);

        assertThat(manifest.path("family").asText()).isEqualTo("tianshu");
        assertThat(typedManifest.family()).isEqualTo("tianshu");
        assertThat(manifest.path("scripts").size()).isEqualTo(4);
        JsonNode v001 = manifest.path("scripts").get(0);
        JsonNode v002 = manifest.path("scripts").get(1);
        JsonNode v003 = manifest.path("scripts").get(2);
        assertThat(v001.path("version").asText()).isEqualTo("20260922_001");
        assertThat(v001.path("path").asText()).isEqualTo(TIANSHU_SQL);
        assertThat(v001.path("sha256").asText()).isEqualTo(sha256(requiredResource(TIANSHU_SQL)));
        assertThat(v002.path("version").asText()).isEqualTo("20260928_001");
        assertThat(v002.path("path").asText()).isEqualTo(TIANSHU_INSTANCE_KEY_SQL);
        assertThat(v002.path("sha256").asText()).isEqualTo(sha256(requiredResource(TIANSHU_INSTANCE_KEY_SQL)));
        assertThat(v003.path("version").asText()).isEqualTo("20261017_001");
        assertThat(v003.path("path").asText()).isEqualTo(TIANSHU_AI_OPTIMIZATION_SQL);
        assertThat(v003.path("sha256").asText()).isEqualTo(sha256(requiredResource(TIANSHU_AI_OPTIMIZATION_SQL)));
        JsonNode v004 = manifest.path("scripts").get(3);
        assertThat(v004.path("version").asText()).isEqualTo("20261018_001");
        assertThat(v004.path("sha256").asText()).isEqualTo(sha256(requiredResource(v004.path("path").asText())));
        assertThat(typedManifest.freshBaselineVersion()).isEqualTo("20261018_001");
        assertThat(typedManifest.startingIndex(null)).isEqualTo(3);
    }

    @Test
    void aiOptimizationDdlAddsThreeTenantScopedTablesOnly() throws Exception {
        String sql = new String(requiredResource(TIANSHU_AI_OPTIMIZATION_SQL), java.nio.charset.StandardCharsets.UTF_8);
        Matcher tables = CREATE_TABLE.matcher(sql);
        List<String> actualTables = new java.util.ArrayList<>();
        while (tables.find()) {
            actualTables.add(tables.group(1).toLowerCase(java.util.Locale.ROOT));
        }
        assertThat(actualTables).containsExactlyInAnyOrderElementsOf(TIANSHU_AI_OPTIMIZATION_TABLES);
        String normalizedSql = sql.toLowerCase(java.util.Locale.ROOT);
        assertThat(Pattern.compile("create\\s+unique\\s+index\\s+uk_ai_optimization_policy_lifecycle\\s+on\\s+"
                + "ddc_ai_optimization_policy_t0\\s*\\([^;]*\\bdeleted_at\\b[^;]*\\)\\s*;", Pattern.DOTALL)
                .matcher(normalizedSql).find()).isTrue();
        assertThat(Pattern.compile("create\\s+unique\\s+index\\s+uk_ai_optimization_policy_live\\s+on\\s+"
                + "ddc_ai_optimization_policy_t0\\s*\\([^;]*\\)\\s+where\\s+deleted_at\\s+is\\s+null\\s*;", Pattern.DOTALL)
                .matcher(normalizedSql).find()).isTrue();
        assertThat(normalizedSql).doesNotContain("egon_outbox", "ddl_history");
    }

    @Test
    void forwardDdlPreservesTenantLocalInstanceBusinessUniqueness() throws Exception {
        String sql = new String(requiredResource(TIANSHU_INSTANCE_KEY_SQL),
                java.nio.charset.StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);
        assertThat(sql).contains("create unique index uk_instance_business on ddc_instance_t0 (tenant_id, instance_id)");
    }

    @Test
    void tianshuSqlOwnsSixteenBusinessTablesAndPublicHistoryOnly() throws Exception {
        String sql = new String(requiredResource(TIANSHU_SQL), java.nio.charset.StandardCharsets.UTF_8);
        Matcher tables = CREATE_TABLE.matcher(sql);
        List<String> actualTables = new java.util.ArrayList<>();
        while (tables.find()) {
            actualTables.add(tables.group(1).toLowerCase(java.util.Locale.ROOT));
        }

        assertThat(actualTables).hasSize(TIANSHU_TABLES.size()).containsExactlyInAnyOrderElementsOf(TIANSHU_TABLES);
        assertThat(sql.toLowerCase(java.util.Locale.ROOT)).contains("create schema if not exists egon_outbox");
        assertThat(sql.toLowerCase(java.util.Locale.ROOT)).doesNotContain("egon_cola_outbox_message");
        assertThat(sql.toLowerCase(java.util.Locale.ROOT)).doesNotContain("egon_outbox.ddl_history");
        String normalizedSql = sql.toLowerCase(java.util.Locale.ROOT);
        LIFECYCLE_INDEX_TABLES.forEach((index, table) -> {
            assertThat(Pattern.compile("create\\s+unique\\s+index\\s+" + index + "\\s+on\\s+" + table
                    + "\\s*\\([^;]*\\bdeleted_at\\b[^;]*\\)\\s*;", Pattern.DOTALL)
                    .matcher(normalizedSql).find()).as(index + " lifecycle index includes deleted_at").isTrue();
            String liveIndex = index.replace("_lifecycle", "_live");
            assertThat(Pattern.compile("create\\s+unique\\s+index\\s+" + liveIndex + "\\s+on\\s+" + table
                    + "\\s*\\([^;]*\\)\\s+where\\s+deleted_at\\s+is\\s+null\\s*;", Pattern.DOTALL)
                    .matcher(normalizedSql).find()).as(liveIndex + " enforces one live row").isTrue();
        });
        assertThat(Pattern.compile("foreign\\s+key\\s*\\(\\s*tenant_id\\s*,\\s*resource_id\\s*,\\s*parent_path\\s*\\)"
                + "\\s*references\\s+(?:public\\.)?ddc_ai_draft_node_t0", Pattern.DOTALL)
                .matcher(normalizedSql).find()).as("draft parent_path has no foreign key").isFalse();
        assertThat(normalizedSql).contains("create table ddl_history",
                "constraint pk_ddl_history primary key (script, type)",
                "constraint uk_ddl_history_type_version unique (type, version)",
                "route_fingerprint char(64) not null");
    }

    @Test
    void allBusinessTablesUseTenantScopedMpBaseColumnsAndLegacyPublicIds() throws Exception {
        String sql = new String(requiredResource(TIANSHU_SQL), java.nio.charset.StandardCharsets.UTF_8);
        for (String table : TIANSHU_TABLES) {
            if ("ddl_history".equals(table)) {
                continue;
            }
            Matcher definition = Pattern.compile("(?is)CREATE\\s+TABLE\\s+" + table + "\\s*\\((.*?)\\n\\);")
                    .matcher(sql);
            assertThat(definition.find()).as("table definition " + table).isTrue();
            String body = definition.group(1).toLowerCase(java.util.Locale.ROOT);
            for (String column : List.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                    "update_time", "deleted_at", "version")) {
                assertThat(Pattern.compile("(?m)^\\s*" + column + "\\s+").matcher(body).find())
                        .as(table + " inherits " + column).isTrue();
            }
            assertThat(body).contains("check (id > 0)", "check (tenant_id > 0)", "check (version >= 0)");
            assertThat(body.contains("public_id varchar(64) not null"))
                    .as(table + " public_id compatibility column").isEqualTo(LEGACY_PUBLIC_ID_TABLES.contains(table));
        }
    }

    @Test
    void aiTablesEnforceTheVersionTreeAndPublicationContracts() throws Exception {
        String sql = new String(requiredResource(TIANSHU_SQL), java.nio.charset.StandardCharsets.UTF_8)
                .toLowerCase(java.util.Locale.ROOT);

        assertThat(sql).contains(
                "resource_key varchar(128) collate \"c\" not null",
                "constraint ck_ai_resource_draft_prompt",
                "constraint ck_ai_version_content",
                "constraint uk_ai_version_no unique (tenant_id, resource_id, version_no)",
                "constraint uk_ai_version_owner unique (tenant_id, resource_id, id)",
                "constraint fk_ai_resource_current_version",
                "constraint fk_ai_version_source",
                "path varchar(1024) collate \"c\" not null",
                "content_bytes = octet_length(content)",
                "content_bytes between 0 and 262144",
                "constraint fk_ai_version_node_parent",
                "constraint uk_ai_publication_request unique (tenant_id, request_id)",
                "constraint ck_ai_publication_source");
    }

    @Test
    void outboxMessageAndHistoryRemainOwnedByOneExistingComponentManifest() throws Exception {
        ClassLoader classLoader = DdcTianshuDdlContractTest.class.getClassLoader();
        Enumeration<URL> manifests = classLoader.getResources(OUTBOX_MANIFEST);
        assertThat(Collections.list(manifests)).hasSize(1);

        JsonNode manifest = OBJECT_MAPPER.readTree(requiredResource(OUTBOX_MANIFEST));
        assertThat(manifest.path("family").asText()).isEqualTo("component-outbox");
        assertThat(manifest.path("scripts").size()).isEqualTo(1);
        JsonNode script = manifest.path("scripts").get(0);
        assertThat(script.path("version").asText()).isEqualTo("20260924_001");
        String path = script.path("path").asText();
        assertThat(path).isEqualTo("db/egon-outbox-mp/V20260924_001__initialize_outbox_mp_schema.sql");
        byte[] sqlBytes = requiredResource(path);
        String sql = new String(sqlBytes, java.nio.charset.StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);
        assertThat(script.path("sha256").asText()).isEqualTo(sha256(sqlBytes));
        assertThat(sql).contains("create table egon_cola_outbox_message", "create table ddl_history")
                .contains("ck_outbox_technical_tenant", "ck_outbox_not_soft_deleted", "ck_outbox_version");
    }

    @Test
    void runtimePersistenceConfigurationSeparatesPublicAndOutboxSchemas() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("tianshu-test-secrets", Map.of(
                "TIANSHU_JDBC_URL", "jdbc:postgresql://localhost:5432/tianshu",
                "TIANSHU_DB_USERNAME", "test-user",
                "TIANSHU_DB_PASSWORD", "test-password")));
        new YamlPropertySourceLoader().load("tianshu-persistence", new ClassPathResource("egon-tianshu-persistence.yml"))
                .forEach(environment.getPropertySources()::addLast);

        EgonColaShardingProperties sharding = Binder.get(environment)
                .bind(EgonColaShardingProperties.PREFIX, Bindable.of(EgonColaShardingProperties.class))
                .orElseThrow(() -> new IllegalStateException("Tianshu sharding configuration is missing"));
        OutboxMpStorageProperties outbox = Binder.get(environment)
                .bind(OutboxMpStorageProperties.PREFIX, Bindable.of(OutboxMpStorageProperties.class))
                .orElseThrow(() -> new IllegalStateException("Tianshu Outbox storage configuration is missing"));
        EgonColaMybatisPlusProperties mybatis = Binder.get(environment)
                .bind(EgonColaMybatisPlusProperties.PREFIX, Bindable.of(EgonColaMybatisPlusProperties.class))
                .orElseThrow(() -> new IllegalStateException("Tianshu MyBatis-Plus configuration is missing"));
        TransactionalOutboxProperties transactionalOutbox = Binder.get(environment)
                .bind("egon.cola.component.transactional-outbox", Bindable.of(TransactionalOutboxProperties.class))
                .orElseThrow(() -> new IllegalStateException("Tianshu Outbox configuration is missing"));

        assertThat(sharding.getConfigStyle()).isEqualTo(EgonColaShardingProperties.ConfigStyleEnum.STRATEGY);
        assertThat(sharding.getTransactionDefaultType()).isEqualTo("LOCAL");
        assertThat(sharding.getDataSources()).singleElement()
                .satisfies(source -> assertThat(source.role()).isEqualTo(EgonColaShardingProperties.DataSourceRoleEnum.PRIMARY));
        assertThat(sharding.getTables()).hasSize(21);
        assertThat(sharding.getTables()).containsKeys(TIANSHU_TABLES.stream()
                .filter(table -> !"ddl_history".equals(table))
                .map(table -> table.replaceFirst("_t0$", ""))
                .toArray(String[]::new));
        assertThat(sharding.getTables().get("ddc_ai_resource").type()).isEqualTo("STANDARD_TENANT_ID");
        assertThat(sharding.getTables().get("egon_cola_outbox_message"))
                .satisfies(table -> {
                    assertThat(table.type()).isEqualTo("SINGLE");
                    assertThat(table.dataSource()).isEqualTo("tianshu_0");
                    assertThat(table.schema()).isEqualTo("egon_outbox");
                });
        assertThat(sharding.getTables().get("ddl_history").schema()).isEqualTo("egon_outbox");
        assertThat(outbox.isAutoInitializeDdl()).isFalse();
        assertThat(mybatis.getDdl().isEnabled()).isFalse();
        assertThat(mybatis.getTenantId().getMdcKey()).isEqualTo("tenantId");
        assertThat(mybatis.getTenantId().getIgnoredTables())
                .contains("egon_cola_outbox_message", "ddl_history");
        assertThat(transactionalOutbox.getStorage().isValidateSchema()).isTrue();

        String yaml = new String(requiredResource("egon-tianshu-persistence.yml"), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(yaml).contains("enabled: false", "validate-schema: true",
                "history-read-statements: [top.egon.cola.component.tianshu.admin.dao.DdcAiResourceMapper.selectIncludingDeletedById]",
                "restore-statements: []").doesNotContain("tenant-id: 1", "password: postgres");
    }

    @Test
    void readinessValidatorChecksTianshuHistoryUsingReadOnlyPublicPrimary() throws Exception {
        EgonColaDdlManifestBO manifest = OBJECT_MAPPER.readValue(requiredResource(TIANSHU_MANIFEST),
                EgonColaDdlManifestBO.class);
        String routeFingerprint = "a".repeat(64);
        DataSource primary = readinessDataSource(manifest.scripts().getFirst(), routeFingerprint, true);

        assertThatCode(() -> DdcDatabaseReadyValidator.validateTianshuSchema(primary, routeFingerprint, manifest))
                .doesNotThrowAnyException();

        verify(primary.getConnection()).setSchema("public");
        verify(primary.getConnection()).setReadOnly(true);
    }

    @Test
    void readinessValidatorFailsClosedWhenTianshuHistoryIsIncomplete() throws Exception {
        EgonColaDdlManifestBO manifest = OBJECT_MAPPER.readValue(requiredResource(TIANSHU_MANIFEST),
                EgonColaDdlManifestBO.class);
        String routeFingerprint = "b".repeat(64);
        DataSource primary = readinessDataSource(manifest.scripts().getFirst(), routeFingerprint, false);

        assertThatThrownBy(() -> DdcDatabaseReadyValidator.validateTianshuSchema(primary, routeFingerprint, manifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANSHU_DDL_HISTORY_INCOMPLETE");
    }

    @Test
    void readinessValidatorRejectsStandbyPrimaryAndMissingBusinessTables() throws Exception {
        EgonColaDdlManifestBO manifest = OBJECT_MAPPER.readValue(requiredResource(TIANSHU_MANIFEST),
                EgonColaDdlManifestBO.class);
        String routeFingerprint = "c".repeat(64);
        EgonColaDdlManifestBO.ScriptBO script = manifest.scripts().getFirst();

        assertThatThrownBy(() -> DdcDatabaseReadyValidator.validateTianshuSchema(
                readinessDataSource(script, routeFingerprint, true, true, null), routeFingerprint, manifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANSHU_PRIMARY_REQUIRED");
        assertThatThrownBy(() -> DdcDatabaseReadyValidator.validateTianshuSchema(
                readinessDataSource(script, routeFingerprint, true, false, "ddc_ai_resource_t0"),
                routeFingerprint, manifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANSHU_SCHEMA_TABLE_MISSING: ddc_ai_resource_t0");
    }

    @Test
    void readinessValidatorFailsClosedWhenManagedTableColumnsDrift() throws Exception {
        EgonColaDdlManifestBO manifest = OBJECT_MAPPER.readValue(requiredResource(TIANSHU_MANIFEST),
                EgonColaDdlManifestBO.class);
        String routeFingerprint = "d".repeat(64);
        DataSource primary = readinessDataSource(manifest.scripts().getFirst(), routeFingerprint, true,
                false, null, "ddc_biz_t0", "biz_code", null);

        assertThatThrownBy(() -> DdcDatabaseReadyValidator.validateTianshuSchema(primary, routeFingerprint, manifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANSHU_SCHEMA_COLUMN_SET_MISMATCH: ddc_biz_t0");
    }

    @Test
    void readinessValidatorFailsClosedWhenLifecycleIndexDrifts() throws Exception {
        EgonColaDdlManifestBO manifest = OBJECT_MAPPER.readValue(requiredResource(TIANSHU_MANIFEST),
                EgonColaDdlManifestBO.class);
        String routeFingerprint = "e".repeat(64);
        DataSource primary = readinessDataSource(manifest.scripts().getFirst(), routeFingerprint, true,
                false, null, null, null, "uk_ai_resource_live");

        assertThatThrownBy(() -> DdcDatabaseReadyValidator.validateTianshuSchema(primary, routeFingerprint, manifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANSHU_SCHEMA_INDEX_MISMATCH: uk_ai_resource_live");

        assertThatThrownBy(() -> DdcDatabaseReadyValidator.validateTianshuSchema(
                readinessDataSource(manifest.scripts().getFirst(), routeFingerprint, true,
                        false, null, null, null, "uk_instance_business"), routeFingerprint, manifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANSHU_SCHEMA_INDEX_MISMATCH: uk_instance_business");
    }

    @Test
    void readinessValidatorFailsClosedWhenTenantConstraintDrifts() throws Exception {
        EgonColaDdlManifestBO manifest = OBJECT_MAPPER.readValue(requiredResource(TIANSHU_MANIFEST),
                EgonColaDdlManifestBO.class);
        String routeFingerprint = "f".repeat(64);
        DataSource primary = readinessDataSource(manifest.scripts().getFirst(), routeFingerprint, true,
                false, null, null, null, null, "fk_ai_resource_namespace");

        assertThatThrownBy(() -> DdcDatabaseReadyValidator.validateTianshuSchema(primary, routeFingerprint, manifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANSHU_SCHEMA_CONSTRAINT_MISSING: ddc_ai_resource_t0.fk_ai_resource_namespace");
    }

    @Test
    void readinessValidatorFailsClosedWhenLiveIndexPredicateDrifts() throws Exception {
        EgonColaDdlManifestBO manifest = OBJECT_MAPPER.readValue(requiredResource(TIANSHU_MANIFEST),
                EgonColaDdlManifestBO.class);
        String routeFingerprint = "1".repeat(64);
        DataSource primary = readinessDataSource(manifest.scripts().getFirst(), routeFingerprint, true,
                false, null, null, null, null, null, "uk_ai_resource_live");

        assertThatThrownBy(() -> DdcDatabaseReadyValidator.validateTianshuSchema(primary, routeFingerprint, manifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANSHU_SCHEMA_INDEX_PREDICATE_MISMATCH: uk_ai_resource_live");
    }

    @Test
    void readinessValidatorRejectsManifestWhenClasspathSqlChecksumDrifts() throws Exception {
        EgonColaDdlManifestBO manifest = OBJECT_MAPPER.readValue(requiredResource(TIANSHU_MANIFEST),
                EgonColaDdlManifestBO.class);
        EgonColaDdlManifestBO.ScriptBO script = manifest.scripts().getFirst();
        EgonColaDdlManifestBO.ScriptBO invalidScript =
                new EgonColaDdlManifestBO.ScriptBO(script.version(), script.path(), "0".repeat(64));
        EgonColaDdlManifestBO invalidManifest = new EgonColaDdlManifestBO("tianshu", List.of(invalidScript));
        DataSource primary = readinessDataSource(invalidScript, "a".repeat(64), true);

        assertThatThrownBy(() -> DdcDatabaseReadyValidator.validateTianshuSchema(
                primary, "a".repeat(64), invalidManifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANSHU_DDL_MANIFEST_RESOURCE_CHECKSUM_MISMATCH");
        verify(primary, org.mockito.Mockito.never()).getConnection();
    }

    @Test
    void readinessValidatorChecksTheChecksumOfEveryForwardScript() throws Exception {
        EgonColaDdlManifestBO manifest = OBJECT_MAPPER.readValue(requiredResource(TIANSHU_MANIFEST),
                EgonColaDdlManifestBO.class);
        EgonColaDdlManifestBO.ScriptBO v001 = manifest.scripts().getFirst();
        EgonColaDdlManifestBO.ScriptBO v002 = manifest.scripts().get(1);
        EgonColaDdlManifestBO invalidManifest = new EgonColaDdlManifestBO("tianshu", List.of(
                v001, new EgonColaDdlManifestBO.ScriptBO(v002.version(), v002.path(), "0".repeat(64))));
        DataSource primary = readinessDataSource(v001, "a".repeat(64), true);

        assertThatThrownBy(() -> DdcDatabaseReadyValidator.validateTianshuSchema(
                primary, "a".repeat(64), invalidManifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TIANSHU_DDL_MANIFEST_RESOURCE_CHECKSUM_MISMATCH");
        verify(primary, org.mockito.Mockito.never()).getConnection();
    }

    @Test
    void persistenceMigrationCliKeepsDdlModesAndSupportsExplicitLegacyImportModes() throws Exception {
        Class<?> cli = Class.forName("top.egon.cola.component.tianshu.admin.migration.DdcPersistenceMigrationCli");
        Method parseMode = cli.getMethod("parseMode", String[].class);

        assertThat(parseMode.invoke(null, (Object) new String[]{"validate"}).toString()).isEqualTo("VALIDATE");
        assertThat(parseMode.invoke(null, (Object) new String[]{"initialize"}).toString()).isEqualTo("INITIALIZE");
        String[] migrationOptions = {
                "--source-config", "/protected/source.yml",
                "--target-config", "/protected/target.yml",
                "--mapping", "/protected/tenant-mapping.json",
                "--report-dir", "/var/tmp/tianshu-migration"
        };
        assertThat(parseMode.invoke(null, (Object) withMode("validate", migrationOptions)).toString())
                .isEqualTo("VALIDATE");
        assertThat(parseMode.invoke(null, (Object) withMode("import", migrationOptions)).toString())
                .isEqualTo("IMPORT");
        assertThat(parseMode.invoke(null, (Object) withMode("verify", migrationOptions)).toString())
                .isEqualTo("VERIFY");
        String[] overlappingConfigs = migrationOptions.clone();
        overlappingConfigs[3] = overlappingConfigs[1];
        assertThatThrownBy(() -> parseMode.invoke(null, (Object) withMode("import", overlappingConfigs)))
                .rootCause()
                .hasMessageStartingWith("USAGE: DdcPersistenceMigrationCli");
        assertThatThrownBy(() -> parseMode.invoke(null, (Object) new String[]{"import", "--mapping", "map.json"}))
                .rootCause()
                .hasMessageStartingWith("USAGE: DdcPersistenceMigrationCli");
        assertThatThrownBy(() -> parseMode.invoke(null, (Object) new String[]{"automatic"}))
                .rootCause()
                .hasMessageStartingWith("USAGE: DdcPersistenceMigrationCli");
    }

    private static String[] withMode(String mode, String[] options) {
        String[] result = new String[options.length + 1];
        result[0] = mode;
        System.arraycopy(options, 0, result, 1, options.length);
        return result;
    }

    @Test
    void legacyFlywayScriptsRemainByteIdentical() throws Exception {
        for (Map.Entry<String, String> script : LEGACY_SCRIPT_SHA256.entrySet()) {
            assertThat(sha256(requiredResource(script.getKey())))
                    .as(script.getKey()).isEqualTo(script.getValue());
        }
    }

    private static byte[] requiredResource(String path) throws IOException {
        try (InputStream input = DdcTianshuDdlContractTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as("classpath resource " + path).isNotNull();
            return input.readAllBytes();
        }
    }

    private static DataSource readinessDataSource(EgonColaDdlManifestBO.ScriptBO script, String routeFingerprint,
                                                  boolean includeHistory) throws SQLException {
        return readinessDataSource(script, routeFingerprint, includeHistory, false, null, null, null, null, null);
    }

    private static DataSource readinessDataSource(EgonColaDdlManifestBO.ScriptBO script, String routeFingerprint,
                                                  boolean includeHistory, boolean standby, String missingTable)
            throws SQLException {
        return readinessDataSource(script, routeFingerprint, includeHistory, standby, missingTable,
                null, null, null, null, null);
    }

    private static DataSource readinessDataSource(EgonColaDdlManifestBO.ScriptBO script, String routeFingerprint,
                                                  boolean includeHistory, boolean standby, String missingTable,
                                                  String driftTable, String driftColumn, String missingIndex)
            throws SQLException {
        return readinessDataSource(script, routeFingerprint, includeHistory, standby, missingTable,
                driftTable, driftColumn, missingIndex, null, null);
    }

    private static DataSource readinessDataSource(EgonColaDdlManifestBO.ScriptBO script, String routeFingerprint,
                                                  boolean includeHistory, boolean standby, String missingTable,
                                                  String driftTable, String driftColumn, String missingIndex,
                                                  String missingConstraint)
            throws SQLException {
        return readinessDataSource(script, routeFingerprint, includeHistory, standby, missingTable,
                driftTable, driftColumn, missingIndex, missingConstraint, null);
    }

    private static DataSource readinessDataSource(EgonColaDdlManifestBO.ScriptBO script, String routeFingerprint,
                                                  boolean includeHistory, boolean standby, String missingTable,
                                                  String driftTable, String driftColumn, String missingIndex,
                                                  String missingConstraint, String wrongPredicateIndex)
            throws SQLException {
        DataSource primary = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(primary.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metadata.getTables(any(), eq("public"), anyString(), any(String[].class))).thenAnswer(invocation -> {
            String table = invocation.getArgument(2);
            ResultSet relation = mock(ResultSet.class);
            when(relation.next()).thenReturn(!table.equals(missingTable));
            when(relation.getString("TABLE_NAME")).thenReturn(table);
            return relation;
        });
        when(metadata.getColumns(any(), eq("public"), anyString(), any())).thenAnswer(invocation -> {
            String table = invocation.getArgument(2);
            Set<String> columns = sqlColumns(table);
            if (table.equals(driftTable)) {
                columns.remove(driftColumn);
            }
            return stringRows("COLUMN_NAME", columns);
        });
        when(metadata.getIndexInfo(any(), eq("public"), anyString(), eq(false), eq(false))).thenAnswer(invocation -> {
            String table = invocation.getArgument(2);
            return indexRows(sqlLifecycleIndexes(table).stream()
                    .filter(index -> !index.name().equals(missingIndex))
                    .toList());
        });
        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            PreparedStatement statement = mock(PreparedStatement.class);
            ResultSet rows = mock(ResultSet.class);
            when(statement.executeQuery()).thenReturn(rows);
            if (sql.contains("pg_is_in_recovery")) {
                when(rows.next()).thenReturn(true);
                when(rows.getString(1)).thenReturn("public");
                when(rows.getBoolean(2)).thenReturn(standby);
            } else if (sql.contains("pg_catalog.pg_index")) {
                List<IndexPredicateBO> predicates = sqlLiveIndexPredicates().stream()
                        .map(predicate -> predicate.name().equals(wrongPredicateIndex)
                                ? new IndexPredicateBO(predicate.name(), "(1 = 1)") : predicate)
                        .toList();
                int[] index = {0};
                when(rows.next()).thenAnswer(row -> index[0]++ < predicates.size());
                when(rows.getString(1)).thenAnswer(row -> predicates.get(index[0] - 1).name());
                when(rows.getString(2)).thenAnswer(row -> predicates.get(index[0] - 1).predicate());
            } else if (sql.contains("pg_catalog.pg_constraint")) {
                List<String[]> constraints = new java.util.ArrayList<>();
                for (String table : TIANSHU_TABLES) {
                    for (String constraint : sqlConstraints(table)) {
                        if (!constraint.equals(missingConstraint)) {
                            constraints.add(new String[]{table, constraint});
                        }
                    }
                }
                int[] index = {0};
                when(rows.next()).thenAnswer(row -> index[0]++ < constraints.size());
                when(rows.getString(1)).thenAnswer(row -> constraints.get(index[0] - 1)[0]);
                when(rows.getString(2)).thenAnswer(row -> constraints.get(index[0] - 1)[1]);
            } else if (includeHistory) {
                List<EgonColaDdlManifestBO.ScriptBO> history = expectedManifestScripts(script);
                int[] historyIndex = {-1};
                when(rows.next()).thenAnswer(row -> ++historyIndex[0] < history.size());
                when(rows.getString(any(Integer.class))).thenAnswer(row -> {
                    EgonColaDdlManifestBO.ScriptBO current = history.get(historyIndex[0]);
                    return switch ((int) row.getArgument(0)) {
                        case 1 -> current.path();
                        case 2 -> "SQL";
                        case 3 -> current.version();
                        case 4 -> current.sha256();
                        case 5 -> routeFingerprint;
                        default -> throw new IllegalArgumentException("Unexpected history column");
                    };
                });
                when(rows.getLong(6)).thenReturn(0L);
            } else {
                when(rows.next()).thenReturn(false);
            }
            return statement;
        });
        return primary;
    }

    private static Set<String> sqlColumns(String table) throws SQLException {
        try {
            String sql = new String(requiredResource(TIANSHU_SQL), java.nio.charset.StandardCharsets.UTF_8);
            Matcher definition = Pattern.compile("(?is)CREATE\\s+TABLE\\s+" + Pattern.quote(table)
                    + "\\s*\\((.*?)\\n\\);").matcher(sql);
            if (!definition.find()) {
                throw new SQLException("Tianshu SQL table definition missing: " + table);
            }
            Set<String> columns = new java.util.LinkedHashSet<>();
            Matcher column = Pattern.compile("(?im)^\\s*([a-z_][a-z0-9_]*)\\s+(?:bigint|varchar|text|integer|"
                    + "boolean|timestamptz|timestamp|char)\\b").matcher(definition.group(1));
            while (column.find()) {
                columns.add(column.group(1));
            }
            return columns;
        } catch (IOException failure) {
            throw new SQLException("Tianshu SQL resource unreadable", failure);
        }
    }

    private static List<IndexRow> sqlLifecycleIndexes(String table) throws SQLException {
        try {
            List<IndexRow> indexes = new java.util.ArrayList<>();
            for (String resource : List.of(TIANSHU_SQL, TIANSHU_INSTANCE_KEY_SQL)) {
                String sql = new String(requiredResource(resource), java.nio.charset.StandardCharsets.UTF_8);
                Matcher index = Pattern.compile("(?im)^CREATE\\s+(UNIQUE\\s+)?INDEX\\s+([a-z0-9_]+)\\s+ON\\s+"
                        + Pattern.quote(table) + "\\s*\\(([^;]+?)\\)\\s*(?:WHERE\\s+[^;]+)?;").matcher(sql);
                while (index.find()) {
                    String name = index.group(2);
                    if (!name.endsWith("_lifecycle") && !name.endsWith("_live")
                            && !"uk_instance_business".equals(name)) {
                        continue;
                    }
                    String[] columns = index.group(3).split(",");
                    for (int position = 0; position < columns.length; position++) {
                        indexes.add(new IndexRow(name, columns[position].trim(), position + 1,
                                index.group(1) == null));
                    }
                }
            }
            return indexes;
        } catch (IOException failure) {
            throw new SQLException("Tianshu SQL resource unreadable", failure);
        }
    }

    private static List<EgonColaDdlManifestBO.ScriptBO> expectedManifestScripts(
            EgonColaDdlManifestBO.ScriptBO firstScript) throws SQLException {
        try {
            EgonColaDdlManifestBO manifest = OBJECT_MAPPER.readValue(
                    requiredResource(TIANSHU_MANIFEST), EgonColaDdlManifestBO.class);
            if (manifest.scripts().getFirst().path().equals(firstScript.path())) return manifest.scripts();
            return List.of(firstScript);
        } catch (IOException failure) {
            throw new SQLException("Tianshu manifest unreadable", failure);
        }
    }

    private static Set<String> sqlConstraints(String table) throws SQLException {
        try {
            String sql = new String(requiredResource(TIANSHU_SQL), java.nio.charset.StandardCharsets.UTF_8);
            Matcher definition = Pattern.compile("(?is)CREATE\\s+TABLE\\s+" + Pattern.quote(table)
                    + "\\s*\\((.*?)\\n\\);").matcher(sql);
            Set<String> constraints = new java.util.LinkedHashSet<>();
            if (definition.find()) {
                Matcher names = Pattern.compile("(?i)CONSTRAINT\\s+([a-z0-9_]+)").matcher(definition.group(1));
                while (names.find()) {
                    constraints.add(names.group(1));
                }
            }
            Matcher alter = Pattern.compile("(?im)^ALTER\\s+TABLE\\s+" + Pattern.quote(table)
                    + "\\s+ADD\\s+CONSTRAINT\\s+([a-z0-9_]+)").matcher(sql);
            while (alter.find()) {
                constraints.add(alter.group(1));
            }
            return constraints;
        } catch (IOException failure) {
            throw new SQLException("Tianshu SQL resource unreadable", failure);
        }
    }

    private static List<IndexPredicateBO> sqlLiveIndexPredicates() throws SQLException {
        try {
            String sql = new String(requiredResource(TIANSHU_SQL), java.nio.charset.StandardCharsets.UTF_8);
            Matcher index = Pattern.compile("(?im)^CREATE\\s+UNIQUE\\s+INDEX\\s+([a-z0-9_]+)\\s+ON\\s+"
                    + "[a-z0-9_]+\\s*\\(([^;]+?)\\)\\s*WHERE\\s+([^;]+);").matcher(sql);
            List<IndexPredicateBO> predicates = new java.util.ArrayList<>();
            while (index.find()) {
                String name = index.group(1);
                if (name.endsWith("_live")) {
                    predicates.add(new IndexPredicateBO(name, "(" + index.group(3).trim() + ")"));
                }
            }
            return predicates;
        } catch (IOException failure) {
            throw new SQLException("Tianshu SQL resource unreadable", failure);
        }
    }

    private static ResultSet stringRows(String columnName, Set<String> values) throws SQLException {
        ResultSet rows = mock(ResultSet.class);
        List<String> ordered = List.copyOf(values);
        int[] index = {0};
        when(rows.next()).thenAnswer(invocation -> index[0]++ < ordered.size());
        when(rows.getString(columnName)).thenAnswer(invocation -> ordered.get(index[0] - 1));
        return rows;
    }

    private static ResultSet indexRows(List<IndexRow> values) throws SQLException {
        ResultSet rows = mock(ResultSet.class);
        int[] index = {0};
        when(rows.next()).thenAnswer(invocation -> index[0]++ < values.size());
        when(rows.getString("INDEX_NAME")).thenAnswer(invocation -> values.get(index[0] - 1).name());
        when(rows.getString("COLUMN_NAME")).thenAnswer(invocation -> values.get(index[0] - 1).column());
        when(rows.getShort("ORDINAL_POSITION")).thenAnswer(invocation -> (short) values.get(index[0] - 1).position());
        when(rows.getBoolean("NON_UNIQUE")).thenAnswer(invocation -> values.get(index[0] - 1).nonUnique());
        return rows;
    }

    private record IndexRow(String name, String column, int position, boolean nonUnique) {
    }

    private record IndexPredicateBO(String name, String predicate) {
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
