package top.egon.cola.component.tianshu.admin.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcLegacyImportContractTest {

    @Test
    void migrationMappingMustVerifyAgainstTheTrustedEd25519Key() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] payload = "{\"sourceFingerprint\":\"v9-source\",\"tenantRules\":{}}"
                .getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keyPair.getPrivate());
        signer.update(payload);
        byte[] signature = signer.sign();
        String trustedPublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        Class<?> serviceType = Class.forName(
                "top.egon.cola.component.tianshu.admin.migration.DdcLegacyImportService");
        Method verify = serviceType.getDeclaredMethod(
                "verifyMappingSignature", byte[].class, byte[].class, String.class);
        verify.setAccessible(true);

        assertThat(verify.invoke(null, payload, signature, trustedPublicKey)).isEqualTo(true);
        assertThat(verify.invoke(null,
                "{\"sourceFingerprint\":\"changed\",\"tenantRules\":{}}".getBytes(StandardCharsets.UTF_8),
                signature, trustedPublicKey)).isEqualTo(false);
    }

    @Test
    void cliRejectsSourceAndTargetConnectionsToTheSamePostgresDatabase() throws Exception {
        Connection source = postgresDatabase("tianshu", "10.0.0.12", 5432);
        Connection targetAlias = postgresDatabase("tianshu", "10.0.0.12", 5432);
        Connection separateTarget = postgresDatabase("tianshu_target", "10.0.0.12", 5432);

        assertThat(DdcPersistenceMigrationCli.sameDatabase(source, targetAlias)).isTrue();
        assertThat(DdcPersistenceMigrationCli.sameDatabase(source, separateTarget)).isFalse();
    }

    @Test
    void legacyRowBuilderCopiesTargetValuesOnInput() {
        Map<String, Object> sourceValues = new java.util.LinkedHashMap<>();
        sourceValues.put("biz_code", "retail");
        DdcLegacyRowDTO row = DdcLegacyRowDTO.builder()
                .sourceTable("ddc_biz")
                .targetTable("ddc_biz_t0")
                .legacyId("legacy-biz-A")
                .tenantId(101L)
                .publicId("legacy-biz-A")
                .sourceHash("a".repeat(64))
                .targetValues(sourceValues)
                .technicalVersion(0L)
                .build();
        sourceValues.put("biz_code", "mutated");

        assertThat(row.getTargetValues()).containsEntry("biz_code", "retail");
        assertThatThrownBy(() -> row.getTargetValues().put("biz_code", "mutated"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(DdcLegacyRowDTO.builder().build().getTargetValues()).isEmpty();
    }

    @Test
    void minimalMigrationFailureReportSerializesWithAnEmptyPerTableMap() throws Exception {
        DdcMigrationReportResult report = DdcMigrationReportResult.builder()
                .mode("IMPORT")
                .jobId("overlap-contract-test")
                .status("FAILED")
                .errorCode("SOURCE_TARGET_DATABASE_OVERLAP")
                .build();

        assertThat(new ObjectMapper().writeValueAsString(report))
                .contains("\"rowsByTable\":{}");
    }

    @Test
    void cliVerifiesTheSignedMappingEnvelopeAgainstTargetConfigTrustBeforeDatabaseSetup(@TempDir Path tempDir)
            throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String payload = "{\"jobId\":\"signed-job\",\"sourceFingerprint\":\""
                + "a".repeat(64) + "\"}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keyPair.getPrivate());
        signer.update(payloadBytes);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode envelope = mapper.createObjectNode()
                .put("keyId", "migration-test-key")
                .put("payload", Base64.getEncoder().encodeToString(payloadBytes))
                .put("signature", Base64.getEncoder().encodeToString(signer.sign()));
        Path mappingFile = tempDir.resolve("mapping.json");
        Files.writeString(mappingFile, mapper.writeValueAsString(envelope), StandardCharsets.UTF_8);
        StandardEnvironment targetEnvironment = new StandardEnvironment();
        targetEnvironment.getPropertySources().addFirst(new MapPropertySource("test-migration-trust",
                Map.of("egon.cola.component.tianshu.migration.trusted-mapping-public-keys.migration-test-key",
                        Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()))));
        Class<?> cliType = Class.forName(
                "top.egon.cola.component.tianshu.admin.migration.DdcPersistenceMigrationCli");
        Method readMapping = cliType.getDeclaredMethod("readSignedMapping", Path.class,
                StandardEnvironment.class, ObjectMapper.class);
        readMapping.setAccessible(true);

        JsonNode verified = (JsonNode) readMapping.invoke(null, mappingFile, targetEnvironment, mapper);
        assertThat(verified.path("jobId").asText()).isEqualTo("signed-job");

        envelope.put("payload", Base64.getEncoder().encodeToString(
                "{\"jobId\":\"tampered\"}".getBytes(StandardCharsets.UTF_8)));
        Files.writeString(mappingFile, mapper.writeValueAsString(envelope), StandardCharsets.UTF_8);
        assertThatThrownBy(() -> readMapping.invoke(null, mappingFile, targetEnvironment, mapper))
                .rootCause().hasMessage("TIANSHU_MIGRATION_MAPPING_SIGNATURE_INVALID");
    }

    @Test
    void validatesImportsAndVerifiesTheFrozenV9RowsWithTenantScopedEnvironmentCopies() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DdcLegacyTableStrategy strategy = new DdcLegacyTableStrategy();
        DdcLegacyImportService service = new DdcLegacyImportService();
        try (Connection writableSource = DriverManager.getConnection("jdbc:sqlite::memory:");
             Connection target = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            migrateSourceThroughV9(writableSource);
            seedSourceRows(writableSource);
            createTargetSchema(target, strategy);
            Connection source = readOnlyView(writableSource);
            String routeFingerprint = "a".repeat(64);
        ObjectNode mapping = migrationMapping(objectMapper, strategy, writableSource,
                    routeFingerprint, "0".repeat(64));

            DdcMigrationReportResult fingerprintProbe = service.validateDatabase(
                    source, target, mapping, routeFingerprint, strategy);
            assertThat(fingerprintProbe.getStatus()).isEqualTo("FAILED");
            assertThat(fingerprintProbe.getErrorCode()).isEqualTo("TIANSHU_MIGRATION_SOURCE_FINGERPRINT_MISMATCH");
            assertThat(fingerprintProbe.getSourceFingerprint()).matches("[0-9a-f]{64}");
            mapping.put("sourceFingerprint", fingerprintProbe.getSourceFingerprint());

            DdcMigrationReportResult preflight = service.validateDatabase(source, target, mapping,
                    routeFingerprint, strategy);
            assertThat(preflight.getStatus()).isEqualTo("SUCCESS");
            long sourceBizRows = queryLong(writableSource, "select count(*) from ddc_biz");
            long sourceEnvRows = queryLong(writableSource, "select count(*) from ddc_env");
            assertThat(preflight.getRowsRead()).isEqualTo(sourceBizRows + sourceEnvRows);
            assertThat(service.verifyDatabase(source, target, mapping, routeFingerprint, strategy).getStatus())
                    .isEqualTo("FAILED");

            Path reportDirectory = Files.createTempDirectory("tianshu-migration-contract");
            Files.setPosixFilePermissions(reportDirectory, Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
            DdcMigrationReportResult imported = service.importDatabase(source, target, mapping,
                    routeFingerprint, "import-contract-test", strategy,
                    new AtomicLong(9000)::incrementAndGet,
                    Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC), reportDirectory);

            assertThat(imported.getStatus()).isEqualTo("SUCCESS");
            assertThat(imported.getRowsRead()).isEqualTo(sourceBizRows + sourceEnvRows);
            assertThat(imported.getRowsInserted()).isEqualTo(sourceBizRows + (sourceEnvRows * 2));
            assertThat(imported.getCheckpoint()).isEqualTo("import-contract-test.checkpoint.json");
            assertThat(imported.getRowsByTable()).containsEntry("ddc_biz", sourceBizRows)
                    .containsEntry("ddc_env", sourceEnvRows);
            assertThat(queryLong(target, "select count(*) from ddc_env_t0 where public_id='env-dev'"))
                    .isEqualTo(2L);
            assertThat(queryLong(writableSource, "select count(*) from ddc_env")).isEqualTo(sourceEnvRows);
            assertThat(service.verifyDatabase(source, target, mapping, routeFingerprint, strategy).getStatus())
                    .isEqualTo("SUCCESS");
            assertThat(Files.exists(reportDirectory.resolve("import-contract-test.checkpoint.json"))).isTrue();
            DdcMigrationReportResult resumed = service.importDatabase(source, target, mapping,
                    routeFingerprint, "import-contract-test", strategy, new AtomicLong(12000)::incrementAndGet,
                    Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC), reportDirectory);
            assertThat(resumed.getStatus()).isEqualTo("SUCCESS");
            assertThat(resumed.getRowsInserted()).isZero();
            assertThat(resumed.getRowsSkipped()).isEqualTo(sourceBizRows + (sourceEnvRows * 2));
            assertThat(queryLong(target, "select count(*) from ddc_biz_t0")).isEqualTo(sourceBizRows);
        }
    }

    @Test
    void legacyTableStrategyKeepsTheVersionedParentFirstV9TableSet() throws Exception {
        List<String> tables = orderedLegacyTableNames();

        assertThat(tables).containsExactly(
                "ddc_biz",
                "ddc_env",
                "ddc_app",
                "ddc_namespace",
                "ddc_namespace_env_app",
                "ddc_config_item",
                "ddc_config_version",
                "ddc_publish_task",
                "ddc_publish_ack",
                "ddc_instance",
                "ddc_operation_log");
    }

    @Test
    void everyLegacyMappingMatchesTheManagedV001BusinessColumnSet() throws Exception {
        DdcLegacyTableStrategy strategy = new DdcLegacyTableStrategy();
        String ddl = resourceText("db/egon-mp/V20260922_001__tianshu_platform.sql");
        Set<String> inherited = Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id",
                "update_time", "deleted_at", "version", "public_id");

        for (String sourceTable : strategy.orderedLegacyTableNames()) {
            String targetTable = strategy.targetTableName(sourceTable);
            List<String> declared = createTableColumns(ddl, targetTable).stream()
                    .filter(column -> !inherited.contains(column)).toList();
            assertThat(strategy.targetColumnNames(sourceTable)).containsExactlyElementsOf(declared);
        }
    }

    @Test
    void mappingPreservesStringPublicIdsDuplicatesGlobalEnvByTenantAndKeepsLocalTime() throws Exception {
        Object strategy = newStrategy();
        Object biz = mapSourceRow(strategy, "ddc_biz", bizRow("legacy-biz-A", "retail", "Retail"),
                101L, Map.of());
        Object envTenant101 = mapSourceRow(strategy, "ddc_env", Map.of(
                "id", "legacy-env-global",
                "env_code", "dev",
                "description", "Development",
                "sort_order", 0,
                "enabled", true,
                "created_at", LocalDateTime.parse("2026-08-18T10:11:12"),
                "updated_at", LocalDateTime.parse("2026-08-18T10:11:12")), 101L, Map.of());
        Object envTenant202 = mapSourceRow(strategy, "ddc_env", Map.of(
                "id", "legacy-env-global",
                "env_code", "dev",
                "description", "Development",
                "sort_order", 0,
                "enabled", true,
                "created_at", LocalDateTime.parse("2026-08-18T10:11:12"),
                "updated_at", LocalDateTime.parse("2026-08-18T10:11:12")), 202L, Map.of());
        Object configVersion = mapSourceRow(strategy, "ddc_config_version", Map.ofEntries(
                Map.entry("id", "legacy-version-A"),
                Map.entry("config_id", "legacy-config-A"),
                Map.entry("app_code", "assistant"),
                Map.entry("env", "dev"),
                Map.entry("namespace", "main"),
                Map.entry("config_key", "prompt"),
                Map.entry("version", 7L),
                Map.entry("old_value", "old"),
                Map.entry("new_value", "new"),
                Map.entry("value_type", "STRING"),
                Map.entry("change_type", "UPDATE"),
                Map.entry("change_reason", "migration fixture"),
                Map.entry("operator", "system:test"),
                Map.entry("operator_ip", "127.0.0.1"),
                Map.entry("biz_code", "retail"),
                Map.entry("created_at", LocalDateTime.parse("2026-08-18T10:11:12"))),
                101L, Map.of("ddc_config_item:legacy-config-A", 9001L));

        assertThat(value(biz, "getPublicId")).isEqualTo("legacy-biz-A");
        assertThat(value(biz, "getTenantId")).isEqualTo(101L);
        assertThat(value(envTenant101, "getPublicId")).isEqualTo(value(envTenant202, "getPublicId"));
        assertThat(value(envTenant101, "getTenantId")).isEqualTo(101L);
        assertThat(value(envTenant202, "getTenantId")).isEqualTo(202L);
        @SuppressWarnings("unchecked")
        Map<String, Object> versionColumns = (Map<String, Object>) value(configVersion, "getTargetValues");
        assertThat(versionColumns)
                .containsEntry("config_id", 9001L)
                .containsEntry("config_revision", 7L)
                .containsEntry("created_at", LocalDateTime.parse("2026-08-18T10:11:12"));
        assertThat(versionColumns).doesNotContainKey("version");
    }

    @Test
    void mappingRejectsMissingTenantAndUnresolvedParentInsteadOfGuessing() throws Exception {
        Object strategy = newStrategy();
        Map<String, Object> biz = Map.of("id", "legacy-biz-A", "biz_code", "retail", "biz_name", "Retail");
        Map<String, Object> binding = Map.of(
                "id", "legacy-binding-A", "namespace_id", "legacy-namespace-A", "app_id", "legacy-app-A",
                "env_code", "dev");

        assertThatThrownBy(() -> mapSourceRow(strategy, "ddc_biz", biz, null, Map.of()))
                .hasMessage("TIANSHU_LEGACY_TENANT_REQUIRED");
        assertThatThrownBy(() -> mapSourceRow(strategy, "ddc_namespace_env_app", binding, 101L, Map.of()))
                .hasMessage("TIANSHU_LEGACY_PARENT_MAPPING_REQUIRED");
    }

    @Test
    void importRowsSkipsAnExactExistingRowAndStopsOnSamePublicIdWithDifferentFields() throws Exception {
        Object strategy = newStrategy();
        Object first = mapSourceRow(strategy, "ddc_biz", bizRow("legacy-biz-A", "retail", "Retail"),
                101L, Map.of());
        Object changed = mapSourceRow(strategy, "ddc_biz", bizRow("legacy-biz-A", "retail", "Changed"),
                101L, Map.of());
        LongSupplier ids = new AtomicLong(5000)::incrementAndGet;
        Object service = newService();

        try (Connection target = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            createBizTarget(target);
            Object firstReport = importRows(service, target, List.of(first), strategy, ids);
            Object repeatReport = importRows(service, target, List.of(first), strategy, ids);
            Object conflictReport = importRows(service, target, List.of(changed), strategy, ids);

            assertThat(value(firstReport, "getRowsInserted")).isEqualTo(1L);
            assertThat(value(repeatReport, "getRowsSkipped")).isEqualTo(1L);
            assertThat(value(conflictReport, "getStatus").toString()).isEqualTo("FAILED");
            assertThat(value(conflictReport, "getErrorCode").toString()).isEqualTo("TARGET_ROW_MISMATCH");
            assertThat(value(conflictReport, "getErrorTable")).isEqualTo("ddc_biz");
            assertThat(value(conflictReport, "getErrorLegacyId")).isEqualTo("legacy-biz-A");
            assertThat(value(conflictReport, "getErrorHash").toString()).matches("[0-9a-f]{64}");
            assertThat(queryString(target,
                    "select biz_name from ddc_biz_t0 where tenant_id=101 and public_id='legacy-biz-A'"))
                    .isEqualTo("Retail");
        }
    }

    @Test
    void importRowsValidatesLegacyDtoBeforeWriting() throws Exception {
        DdcLegacyTableStrategy strategy = new DdcLegacyTableStrategy();
        DdcLegacyImportService service = new DdcLegacyImportService();
        DdcLegacyRowDTO invalid = strategy.mapSourceRow("ddc_biz",
                bizRow("legacy-biz-invalid", "retail", "Retail"), 101L, Map.of())
                .setPublicId("");

        try (Connection target = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            createBizTarget(target);
            DdcMigrationReportResult report = service.importRows(target, List.of(invalid),
                    "validation-contract-test", strategy, new AtomicLong(9400)::incrementAndGet,
                    Clock.systemUTC());

            assertThat(report.getStatus()).isEqualTo("FAILED");
            assertThat(report.getErrorCode()).isEqualTo("LEGACY_ROW_VALIDATION_FAILED");
            assertThat(queryLong(target, "select count(*) from ddc_biz_t0")).isZero();
        }
    }

    @Test
    void importRowsCommitsAtMostOneThousandRowsPerBatchAndLeavesPriorBatchOnConflict() throws Exception {
        Object strategy = newStrategy();
        LongSupplier ids = new AtomicLong(7000)::incrementAndGet;
        Object service = newService();
        List<Object> rows = new ArrayList<>();
        for (int index = 0; index < 1000; index++) {
            rows.add(mapSourceRow(strategy, "ddc_biz",
                    bizRow("legacy-biz-" + index, "retail-" + index, "Retail " + index), 101L, Map.of()));
        }
        rows.add(mapSourceRow(strategy, "ddc_biz",
                bizRow("legacy-biz-conflict", "retail-conflict", "Incoming"), 101L, Map.of()));

        try (Connection target = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            createBizTarget(target);
            execute(target, "insert into ddc_biz_t0 (id,tenant_id,public_id,biz_code,biz_name,"
                    + "create_user_id,create_time,update_user_id,update_time,version,enabled,created_at,updated_at) "
                    + "values (8000,101,'legacy-biz-conflict','retail-conflict','Existing','system:test',"
                    + "'2026-08-18T10:11:12','system:test','2026-08-18T10:11:12',0,1,"
                    + "'2026-08-18T10:11:12','2026-08-18T10:11:12')");

            Object report = importRows(service, target, rows, strategy, ids);

            assertThat(value(report, "getStatus").toString()).isEqualTo("FAILED");
            assertThat(value(report, "getRowsInserted")).isEqualTo(1000L);
            assertThat(value(report, "getLastSourceId")).isEqualTo("legacy-biz-999");
            assertThat(value(report, "getErrorTable")).isEqualTo("ddc_biz");
            assertThat(value(report, "getErrorLegacyId")).isEqualTo("legacy-biz-conflict");
            assertThat(queryLong(target, "select count(*) from ddc_biz_t0")).isEqualTo(1001L);
        }
    }

    @Test
    void failedRollbackDoesNotRestoreAutoCommitAndRiskImplicitCommit() throws Exception {
        DdcLegacyTableStrategy strategy = new DdcLegacyTableStrategy();
        DdcLegacyImportService service = new DdcLegacyImportService();
        DdcLegacyRowDTO first = strategy.mapSourceRow("ddc_biz",
                bizRow("legacy-biz-A", "retail", "Retail"), 101L, Map.of());
        DdcLegacyRowDTO invalidTarget = strategy.mapSourceRow("ddc_biz",
                bizRow("legacy-biz-B", "assistant", "Assistant"), 101L, Map.of())
                .setTargetTable("ddc_env_t0");
        AtomicLong autoCommitRestores = new AtomicLong();

        try (Connection target = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            createBizTarget(target);
            Connection rollbackFailure = rollbackFailureView(target, autoCommitRestores);

            assertThatThrownBy(() -> service.importRows(rollbackFailure, List.of(first, invalidTarget),
                    "rollback-contract-test", strategy, new AtomicLong(9500)::incrementAndGet,
                    Clock.systemUTC()))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("rollback failed");
            assertThat(autoCommitRestores).hasValue(0);
        }
    }

    @Test
    void validatesImportsAndVerifiesFrozenV9RowsWithTenantScopedEnvironmentCopies(@TempDir Path tempDir)
            throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DdcLegacyTableStrategy strategy = new DdcLegacyTableStrategy();
        DdcLegacyImportService service = new DdcLegacyImportService();
        try (Connection writableSource = DriverManager.getConnection("jdbc:sqlite::memory:");
             Connection target = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            migrateSourceThroughV9(writableSource);
            seedSourceRows(writableSource);
            createTargetSchema(target, strategy);
            Connection source = readOnlyView(writableSource);
            String routeFingerprint = "a".repeat(64);
            ObjectNode mapping = migrationMapping(objectMapper, strategy, writableSource,
                    routeFingerprint, "0".repeat(64));

            DdcMigrationReportResult fingerprintProbe = service.validateDatabase(
                    source, target, mapping, routeFingerprint, strategy);
            assertThat(fingerprintProbe.getStatus()).isEqualTo("FAILED");
            assertThat(fingerprintProbe.getErrorCode()).isEqualTo("TIANSHU_MIGRATION_SOURCE_FINGERPRINT_MISMATCH");
            assertThat(fingerprintProbe.getSourceFingerprint()).matches("[0-9a-f]{64}");
            mapping.put("sourceFingerprint", fingerprintProbe.getSourceFingerprint());

            DdcMigrationReportResult preflight = service.validateDatabase(
                    source, target, mapping, routeFingerprint, strategy);
            long sourceBizRows = queryLong(writableSource, "select count(*) from ddc_biz");
            long sourceEnvRows = queryLong(writableSource, "select count(*) from ddc_env");
            assertThat(preflight.getStatus()).isEqualTo("SUCCESS");
            assertThat(preflight.getRowsRead()).isEqualTo(sourceBizRows + sourceEnvRows);
            assertThat(service.verifyDatabase(source, target, mapping, routeFingerprint, strategy).getStatus())
                    .isEqualTo("FAILED");

            Path reportDirectory = tempDir.resolve("reports");
            Files.createDirectory(reportDirectory);
            Files.setPosixFilePermissions(reportDirectory, Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
            DdcMigrationReportResult imported = service.importDatabase(source, target, mapping,
                    routeFingerprint, "import-contract-test", strategy, new AtomicLong(9000)::incrementAndGet,
                    Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC), reportDirectory);

            assertThat(imported.getStatus()).isEqualTo("SUCCESS");
            assertThat(imported.getRowsRead()).isEqualTo(sourceBizRows + sourceEnvRows);
            assertThat(imported.getRowsInserted()).isEqualTo(sourceBizRows + (sourceEnvRows * 2));
            assertThat(queryLong(target,
                    "select count(*) from ddc_env_t0 where public_id='env-dev'")).isEqualTo(2L);
            assertThat(queryLong(writableSource, "select count(*) from ddc_env")).isEqualTo(sourceEnvRows);
            assertThat(service.verifyDatabase(source, target, mapping, routeFingerprint, strategy).getStatus())
                    .isEqualTo("SUCCESS");
            assertThat(Files.exists(reportDirectory.resolve("import-contract-test.checkpoint.json"))).isTrue();
        }
    }

    private static List<String> orderedLegacyTableNames() throws Exception {
        Object strategy = newStrategy();
        @SuppressWarnings("unchecked")
        List<String> tables = (List<String>) strategy.getClass().getMethod("orderedLegacyTableNames").invoke(strategy);
        return tables;
    }

    private static Object newStrategy() throws Exception {
        Class<?> type = Class.forName("top.egon.cola.component.tianshu.admin.migration.DdcLegacyTableStrategy");
        return type.getConstructor().newInstance();
    }

    private static Object mapSourceRow(Object strategy, String table, Map<String, Object> row,
                                       Long tenantId, Map<String, Long> parentIds) throws Exception {
        try {
            return strategy.getClass()
                    .getMethod("mapSourceRow", String.class, Map.class, Long.class, Map.class)
                    .invoke(strategy, table, row, tenantId, parentIds);
        } catch (InvocationTargetException failure) {
            throw asException(failure.getCause());
        }
    }

    private static Object newService() throws Exception {
        Class<?> type = Class.forName("top.egon.cola.component.tianshu.admin.migration.DdcLegacyImportService");
        return type.getConstructor().newInstance();
    }

    private static Object importRows(Object service, Connection target, List<?> rows,
                                     Object strategy, LongSupplier ids) throws Exception {
        try {
            return service.getClass().getMethod("importRows", Connection.class, List.class, String.class,
                            strategy.getClass(), LongSupplier.class, Clock.class)
                    .invoke(service, target, rows, "job-contract-test", strategy, ids,
                            Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC));
        } catch (InvocationTargetException failure) {
            throw asException(failure.getCause());
        }
    }

    private static Connection postgresDatabase(String database, String serverAddress, int serverPort)
            throws SQLException {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getString(1)).thenReturn(database);
        when(result.getString(2)).thenReturn(serverAddress);
        when(result.getInt(3)).thenReturn(serverPort);
        return connection;
    }

    private static Connection rollbackFailureView(Connection delegate, AtomicLong autoCommitRestores) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    if ("rollback".equals(method.getName())) throw new SQLException("rollback failed");
                    if ("setAutoCommit".equals(method.getName()) && args != null && Boolean.TRUE.equals(args[0])) {
                        autoCommitRestores.incrementAndGet();
                    }
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
    }

    private static Object value(Object target, String getter) throws Exception {
        return target.getClass().getMethod(getter).invoke(target);
    }

    private static Exception asException(Throwable failure) {
        return failure instanceof Exception exception ? exception : new RuntimeException(failure);
    }

    private static Map<String, Object> bizRow(String id, String code, String name) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", id);
        row.put("biz_code", code);
        row.put("biz_name", name);
        row.put("description", null);
        row.put("enabled", true);
        row.put("created_at", LocalDateTime.parse("2026-08-18T10:11:12"));
        row.put("updated_at", LocalDateTime.parse("2026-08-18T10:11:12"));
        return row;
    }

    private static void createBizTarget(Connection target) throws Exception {
        execute(target, "create table ddc_biz_t0 (id integer not null, tenant_id integer not null, "
                + "create_user_id text not null, create_time text not null, update_user_id text not null, "
                + "update_time text not null, deleted_at text, version integer not null, public_id text not null, "
                + "biz_code text not null, biz_name text not null, description text, enabled integer not null, "
                + "created_at text not null, updated_at text not null, unique(tenant_id, public_id), "
                + "unique(tenant_id, biz_code, deleted_at))");
    }

    private static String queryString(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getString(1) : null;
        }
    }

    private static long queryLong(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getLong(1) : -1L;
        }
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void migrateSourceThroughV9(Connection connection) throws Exception {
        for (int version = 1; version <= 9; version++) {
            String suffix = switch (version) {
                case 1 -> "create_ddc_schema";
                case 2 -> "add_lease_and_sync_publish";
                case 3 -> "add_instance_runtime_metadata";
                case 4 -> "add_published_config_pointer";
                case 5 -> "add_biz_env_and_detach_namespace_env";
                case 6 -> "add_namespace_code";
                case 7 -> "add_namespace_env_app_visibility";
                case 8 -> "add_resource_admission_audit";
                case 9 -> "enforce_global_biz_app_codes";
                default -> throw new IllegalArgumentException("unsupported V9 fixture step");
            };
            executeScript(connection, "db/sqlite/V" + version + "__" + suffix + ".sql");
        }
        execute(connection, "create table flyway_schema_history (installed_rank integer, version text, "
                + "checksum text, success integer)");
        for (int version = 1; version <= 9; version++) {
            execute(connection, "insert into flyway_schema_history values (" + version + ", '" + version
                    + "', 'source-checksum-" + version + "', 1)");
        }
    }

    private static void executeScript(Connection connection, String resource) throws Exception {
        String script = resourceText(resource);
        for (String sql : script.split(";")) {
            if (!sql.isBlank()) execute(connection, sql);
        }
    }

    private static String resourceText(String name) throws Exception {
        try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            if (input == null) throw new IllegalStateException("missing test resource: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> createTableColumns(String ddl, String table) {
        Pattern pattern = Pattern.compile("(?is)CREATE\\s+TABLE\\s+" + Pattern.quote(table)
                + "\\s*\\((.*?)\\n\\);");
        Matcher matcher = pattern.matcher(ddl);
        if (!matcher.find()) throw new IllegalStateException("missing target table DDL: " + table);
        List<String> columns = new ArrayList<>();
        for (String line : matcher.group(1).split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String name = trimmed.split("\\s+", 2)[0].replace(",", "");
            if (name.matches("[a-z][a-z0-9_]*") && !Set.of(
                    "constraint", "primary", "foreign", "unique", "check").contains(name)) {
                columns.add(name);
            }
        }
        return columns;
    }

    private static void seedSourceRows(Connection source) throws Exception {
        try (PreparedStatement biz = source.prepareStatement("insert into ddc_biz "
                + "(id,biz_code,biz_name,description,enabled,created_at,updated_at) values(?,?,?,?,?,?,?)")) {
            biz.setString(1, "legacy-biz-A");
            biz.setString(2, "retail");
            biz.setString(3, "Retail");
            biz.setString(4, "source fixture");
            biz.setInt(5, 1);
            biz.setString(6, "2026-08-18 10:11:12");
            biz.setString(7, "2026-08-18 10:11:12");
            biz.executeUpdate();
        }
    }

    private static void createTargetSchema(Connection target, DdcLegacyTableStrategy strategy) throws Exception {
        for (String sourceTable : strategy.orderedLegacyTableNames()) {
            List<String> columns = new ArrayList<>(List.of(
                    "id INTEGER NOT NULL", "tenant_id INTEGER NOT NULL", "create_user_id TEXT NOT NULL",
                    "create_time TEXT NOT NULL", "update_user_id TEXT NOT NULL", "update_time TEXT NOT NULL",
                    "deleted_at TEXT", "version INTEGER NOT NULL", "public_id TEXT NOT NULL"));
            for (String column : strategy.targetColumnNames(sourceTable)) {
                columns.add(column + " " + sqliteType(column));
            }
            columns.add("UNIQUE (tenant_id, public_id)");
            execute(target, "CREATE TABLE " + strategy.targetTableName(sourceTable)
                    + " (" + String.join(",", columns) + ")");
        }
    }

    private static String sqliteType(String column) {
        if (Set.of("enabled", "deleted").contains(column)) return "INTEGER";
        if (Set.of("sort_order", "port", "target_count", "ack_count", "failed_count", "ignored_count",
                "timeout_count", "timeout_ms", "attempt_count", "current_version", "published_version",
                "target_version", "config_revision", "namespace_id", "app_id", "resource_version").contains(column)) {
            return "INTEGER";
        }
        return "TEXT";
    }

    private static ObjectNode migrationMapping(ObjectMapper mapper, DdcLegacyTableStrategy strategy,
                                               Connection source, String targetRouteFingerprint,
                                               String sourceFingerprint) throws Exception {
        ObjectNode mapping = mapper.createObjectNode();
        mapping.put("jobId", "import-contract-test");
        mapping.put("sourceFingerprint", sourceFingerprint);
        mapping.put("targetRouteFingerprint", targetRouteFingerprint);
        ObjectNode checksums = mapping.putObject("sourceHistoryChecksums");
        for (int version = 1; version <= 9; version++) {
            checksums.put(Integer.toString(version), "source-checksum-" + version);
        }
        ArrayNode tenants = mapping.putArray("registeredTenants");
        tenants.addObject().put("identityTenantId", "identity-a").put("tenantId", 101);
        tenants.addObject().put("identityTenantId", "identity-b").put("tenantId", 202);
        ObjectNode tenantMappings = mapping.putObject("tenantMappings");
        for (String table : strategy.orderedLegacyTableNames()) tenantMappings.putObject(table);
        ObjectNode bizMappings = (ObjectNode) tenantMappings.get("ddc_biz");
        bizMappings.put("biz-default", 101);
        bizMappings.put("legacy-biz-A", 101);
        ObjectNode envMappings = (ObjectNode) tenantMappings.get("ddc_env");
        try (Statement statement = source.createStatement();
             ResultSet result = statement.executeQuery("select id from ddc_env order by id")) {
            while (result.next()) envMappings.putArray(result.getString(1)).add(101).add(202);
        }
        return mapping;
    }

    private static Connection readOnlyView(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("isReadOnly".equals(method.getName())) return true;
                    if ("setReadOnly".equals(method.getName())) return null;
                    if ("prepareStatement".equals(method.getName()) && args != null && args.length > 0
                            && !isReadQuery(args[0].toString())) {
                        throw new SQLException("test source connection is read-only");
                    }
                    try {
                        Object result = method.invoke(delegate, args);
                        if (result instanceof Statement statement && "createStatement".equals(method.getName())) {
                            return readOnlyStatement(statement, false);
                        }
                        if (result instanceof PreparedStatement prepared && "prepareStatement".equals(method.getName())) {
                            return readOnlyStatement(prepared, true);
                        }
                        return result;
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
    }

    private static Object readOnlyStatement(Statement delegate, boolean prepared) {
        Class<?> contract = prepared ? PreparedStatement.class : Statement.class;
        return Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[]{contract}, (proxy, method, args) -> {
            String name = method.getName();
            if (Set.of("executeUpdate", "executeLargeUpdate", "addBatch", "executeBatch").contains(name)) {
                throw new SQLException("test source connection is read-only");
            }
            if (!prepared && "execute".equals(name) && args != null && args.length > 0
                    && !isReadQuery(args[0].toString())) {
                throw new SQLException("test source connection is read-only");
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException failure) {
                throw failure.getCause();
            }
        });
    }

    private static boolean isReadQuery(String sql) {
        return sql.stripLeading().toLowerCase(java.util.Locale.ROOT).startsWith("select");
    }
}
