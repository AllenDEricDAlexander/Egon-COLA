package top.egon.cola.component.tianshu.admin.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class DdcLegacyMigrationIT {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void frozenV9FixtureImportsTenantScopedIdsAndHistoryIntoAnIsolatedPostgresTarget(@TempDir Path tempDir)
            throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TIANSHU_AI_PERSISTENCE_IT_ENABLED")),
                "requires an explicitly configured isolated legacy-migration PostgreSQL target");
        String jdbcUrl = requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_LEGACY_JDBC_URL");
        String username = requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_LEGACY_DB_USERNAME");
        String password = requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_LEGACY_DB_PASSWORD");
        String jobId = "step24-it-" + java.util.UUID.randomUUID().toString().replace("-", "");
        DdcLegacyTableStrategy strategy = new DdcLegacyTableStrategy();
        DdcLegacyImportService service = new DdcLegacyImportService();

        try (Connection source = DriverManager.getConnection("jdbc:sqlite::memory:");
             Connection target = DriverManager.getConnection(jdbcUrl, username, password)) {
            migrateSourceThroughV9(source);
            seedSourceRows(source);
            Connection readOnlySource = readOnlyView(source);
            String sourceFingerprint = fingerprintProbe(service, readOnlySource, target, strategy, jobId);
            ObjectNode mapping = mapping(strategy, readOnlySource, target, jobId, sourceFingerprint);
            var preflight = service.validateDatabase(readOnlySource, target, mapping,
                    mapping.path("targetRouteFingerprint").asText(),
                    strategy);
            assertThat(preflight.getStatus()).isEqualTo("SUCCESS");
            assertThat(preflight.getSourceFingerprint()).isEqualTo(sourceFingerprint);

            Path reportDirectory = tempDir.resolve("reports");
            Files.createDirectory(reportDirectory);
            Files.setPosixFilePermissions(reportDirectory, Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
            long sourceBizRows = queryLong(source, "SELECT count(*) FROM ddc_biz");
            long sourceEnvRows = queryLong(source, "SELECT count(*) FROM ddc_env");
            var imported = service.importDatabase(readOnlySource, target, mapping,
                    mapping.path("targetRouteFingerprint").asText(), jobId, strategy,
                    new AtomicLong(System.currentTimeMillis() * 1000)::incrementAndGet,
                    Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC), reportDirectory);

            assertThat(imported.getStatus()).isEqualTo("SUCCESS");
            assertThat(imported.getRowsRead()).isEqualTo(sourceBizRows + sourceEnvRows);
            assertThat(imported.getRowsByTable()).containsEntry("ddc_biz", sourceBizRows)
                    .containsEntry("ddc_env", sourceEnvRows);
            assertThat(queryLong(target, "SELECT count(*) FROM ddc_biz_t0 WHERE tenant_id = 101 "
                    + "AND public_id = 'legacy-biz-A' AND biz_code = 'step24-retail'"))
                    .isEqualTo(1L);
            assertThat(queryLong(target, "SELECT count(*) FROM ddc_env_t0 WHERE public_id = 'legacy-env-global' "
                    + "AND tenant_id IN (101, 202)"))
                    .isEqualTo(2L);
            assertThat(queryLong(target, "SELECT count(DISTINCT id) FROM ddc_env_t0 "
                    + "WHERE public_id = 'legacy-env-global' AND tenant_id IN (101, 202)"))
                    .isEqualTo(2L);
            assertThat(service.verifyDatabase(readOnlySource, target, mapping,
                    mapping.path("targetRouteFingerprint").asText(), strategy).getStatus()).isEqualTo("SUCCESS");
            assertThat(Files.exists(reportDirectory.resolve(jobId + ".checkpoint.json"))).isTrue();
            assertThat(queryLong(source, "SELECT count(*) FROM ddc_biz")).isEqualTo(sourceBizRows);
            assertThat(queryLong(source, "SELECT count(*) FROM ddc_env")).isEqualTo(sourceEnvRows);
        }
    }

    private static String fingerprintProbe(DdcLegacyImportService service, Connection source, Connection target,
                                           DdcLegacyTableStrategy strategy, String jobId) throws Exception {
        ObjectNode mapping = mapping(strategy, source, target, jobId, "0".repeat(64));
        var probe = service.validateDatabase(source, target, mapping,
                mapping.path("targetRouteFingerprint").asText(), strategy);
        assertThat(probe.getStatus()).isEqualTo("FAILED");
        assertThat(probe.getErrorCode()).isEqualTo("TIANSHU_MIGRATION_SOURCE_FINGERPRINT_MISMATCH");
        assertThat(probe.getSourceFingerprint()).matches("[0-9a-f]{64}");
        return probe.getSourceFingerprint();
    }

    private static ObjectNode mapping(DdcLegacyTableStrategy strategy, Connection source, Connection target,
                                     String jobId, String sourceFingerprint) throws Exception {
        ObjectNode mapping = OBJECT_MAPPER.createObjectNode();
        mapping.put("jobId", jobId);
        mapping.put("sourceFingerprint", sourceFingerprint);
        mapping.put("targetRouteFingerprint", targetRouteFingerprint(target));
        ObjectNode checksums = mapping.putObject("sourceHistoryChecksums");
        for (int version = 1; version <= 9; version++) checksums.put(Integer.toString(version), "source-checksum-" + version);
        ArrayNode tenants = mapping.putArray("registeredTenants");
        tenants.addObject().put("identityTenantId", "identity-a").put("tenantId", 101);
        tenants.addObject().put("identityTenantId", "identity-b").put("tenantId", 202);
        ObjectNode tenantMappings = mapping.putObject("tenantMappings");
        for (String table : strategy.orderedLegacyTableNames()) tenantMappings.putObject(table);
        tenantMappings.with("ddc_biz").put("biz-default", 101).put("legacy-biz-A", 101);
        try (Statement statement = source.createStatement();
             ResultSet result = statement.executeQuery("SELECT id FROM ddc_env ORDER BY id")) {
            while (result.next()) tenantMappings.with("ddc_env").putArray(result.getString(1)).add(101).add(202);
        }
        return mapping;
    }

    private static String targetRouteFingerprint(Connection target) throws Exception {
        try (PreparedStatement statement = target.prepareStatement(
                "SELECT route_fingerprint FROM public.ddl_history WHERE script = ? AND type = 'SQL'")) {
            statement.setString(1, "db/egon-mp/V20260922_001__tianshu_platform.sql");
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("Legacy target must be preinitialized");
                String fingerprint = result.getString(1);
                if (!fingerprint.matches("[0-9a-f]{64}")) {
                    throw new IllegalStateException("Legacy target route fingerprint is invalid");
                }
                return fingerprint;
            }
        }
    }

    private static void migrateSourceThroughV9(Connection connection) throws Exception {
        List<String> suffixes = List.of("create_ddc_schema", "add_lease_and_sync_publish",
                "add_instance_runtime_metadata", "add_published_config_pointer",
                "add_biz_env_and_detach_namespace_env", "add_namespace_code",
                "add_namespace_env_app_visibility", "add_resource_admission_audit",
                "enforce_global_biz_app_codes");
        for (int index = 0; index < suffixes.size(); index++) {
            String resource = "db/sqlite/V" + (index + 1) + "__" + suffixes.get(index) + ".sql";
            try (var input = DdcLegacyMigrationIT.class.getClassLoader().getResourceAsStream(resource)) {
                if (input == null) throw new IllegalStateException("Missing frozen V9 fixture " + resource);
                String script = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                for (String sql : script.split(";")) execute(connection, sql);
            }
        }
        execute(connection, "CREATE TABLE flyway_schema_history (installed_rank integer, version text, "
                + "checksum text, success integer)");
        for (int version = 1; version <= 9; version++) {
            execute(connection, "INSERT INTO flyway_schema_history VALUES (" + version + ", '" + version
                    + "', 'source-checksum-" + version + "', 1)");
        }
    }

    private static void seedSourceRows(Connection source) throws Exception {
        try (PreparedStatement statement = source.prepareStatement("INSERT INTO ddc_biz "
                + "(id, biz_code, biz_name, description, enabled, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, "legacy-biz-A");
            statement.setString(2, "step24-retail");
            statement.setString(3, "Retail");
            statement.setString(4, "V9 integration fixture");
            statement.setInt(5, 1);
            statement.setString(6, "2026-08-18 10:11:12");
            statement.setString(7, "2026-08-18 10:11:12");
            statement.executeUpdate();
        }
        try (PreparedStatement statement = source.prepareStatement("INSERT INTO ddc_env "
                + "(id, env_code, description, sort_order, enabled, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, "legacy-env-global");
            statement.setString(2, "dev");
            statement.setString(3, "Development");
            statement.setInt(4, 1);
            statement.setInt(5, 1);
            statement.setString(6, "2026-08-18 10:11:12");
            statement.setString(7, "2026-08-18 10:11:12");
            statement.executeUpdate();
        }
    }

    private static void execute(Connection connection, String sql) throws Exception {
        if (sql.isBlank()) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static Connection readOnlyView(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("isReadOnly".equals(method.getName())) return true;
                    if ("setReadOnly".equals(method.getName())) return null;
                    if ("prepareStatement".equals(method.getName()) && args != null && args.length > 0
                            && !isReadQuery(args[0].toString())) {
                        throw new SQLException("V9 fixture source is read-only");
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
                throw new SQLException("V9 fixture source is read-only");
            }
            if (!prepared && "execute".equals(name) && args != null && args.length > 0
                    && !isReadQuery(args[0].toString())) {
                throw new SQLException("V9 fixture source is read-only");
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

    private static long queryLong(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) throw new IllegalStateException("Query returned no rows");
            return result.getLong(1);
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required for this IT");
        return value;
    }
}
