package top.egon.cola.component.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.codegen.ddl.DdlSchemaService;
import top.egon.cola.component.codegen.ddl.PostgreDdlAdapter;
import top.egon.cola.component.codegen.ddl.PostgreDdlAdapter.DdlParseException;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgreDdlAdapterTest {

    private final DdlSchemaService service = new DdlSchemaService(new PostgreDdlAdapter());

    @Test
    void nativeScriptsKeepMasterAndShardRoles() throws Exception {
        Path root = repositoryRoot();
        CodegenSchemaBO light = read(root.resolve(
                "egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql"));
        assertEquals("MASTER_DATA", table(light, "users").getRole());
        assertEquals("SHARD", table(light, "school_classes_0").getRole());
        assertTrue(index(table(light, "users"), "uk_users_tenant_external_active").getPredicate()
                .toLowerCase(java.util.Locale.ROOT).contains("deleted_at is null"));
        assertFalse(light.getTables().stream().anyMatch(table -> "ddl_history".equals(table.getLogicalName())));

        CodegenSchemaBO web = read(root.resolve(
                "egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql"));
        assertEquals("MASTER_DATA", table(web, "users").getRole());
        assertEquals("SHARD", table(web, "school_classes_0").getRole());

        CodegenSchemaBO serviceSchema = read(root.resolve(
                "egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql"));
        assertEquals("MASTER_DATA", table(serviceSchema, "evaluation_course").getRole());
        assertEquals("SHARD", table(serviceSchema, "evaluation_exam_0").getRole());

        CodegenSchemaBO merged = service.read(config(root.resolve(
                "egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql")),
                Map.of("school_classes", List.of("school_classes_0", "school_classes_1")), List.of());
        assertEquals(List.of("school_classes_0", "school_classes_1"), table(merged, "school_classes").getPhysicalNames());
        assertFalse(merged.getTables().stream().anyMatch(table -> "school_classes_0".equals(table.getLogicalName())));
    }

    @Test
    void schemaChangesReplayAddRenameAndNullability() throws Exception {
        Path schema = Path.of("src/test/resources/ddl/schema.sql");
        Path changes = Path.of("src/test/resources/ddl/changes.sql");
        CodegenSchemaBO replayed = service.read(config(schema, changes), Map.of(), List.of());
        CodegenSchemaBO.TableBO orders = table(replayed, "orders");
        assertTrue(orders.getColumns().stream().noneMatch(column -> "code".equals(column.getName())));
        assertEquals(Boolean.FALSE, column(orders, "note").getNullable());
        assertEquals("order_code", column(orders, "order_code").getName());
        assertEquals("业务代码与 Unicode 转义样本", column(orders, "order_code").getComment());
        assertTrue(index(orders, "orders_active").getPredicate().toLowerCase(java.util.Locale.ROOT).contains("deleted_at is null"));
    }

    @Test
    void typeChangeIsAppliedIndependently(@TempDir Path temp) throws Exception {
        Path baseline = temp.resolve("schema.sql");
        Path alter = temp.resolve("type.sql");
        Files.copy(Path.of("src/test/resources/ddl/schema.sql"), baseline);
        Files.writeString(alter, "ALTER TABLE orders ALTER COLUMN code TYPE VARCHAR(80);\n");
        CodegenSchemaBO schema = service.read(config(baseline, alter), Map.of(), List.of());
        assertEquals(80, column(table(schema, "orders"), "code").getLength());
    }

    @Test
    void unknownDynamicExecuteFailsWithSourcePosition() {
        String sql = """
                DO $egon$
                BEGIN
                    EXECUTE 'CREATE TABLE generated(id int)';
                END
                $egon$;
                """;
        DdlParseException exception = assertThrows(DdlParseException.class,
                () -> new PostgreDdlAdapter().parseStatements(sql, "dynamic.sql"));
        assertEquals(PostgreDdlAdapter.UNSUPPORTED_DDL, exception.getCode());
        assertEquals("dynamic.sql", exception.getFile());
        assertTrue(exception.getLine() >= 1);
    }

    @Test
    void historyChecksumDriftFailsBeforeRendering(@TempDir Path temp) throws Exception {
        Path script = temp.resolve("db/egon-mp/V20260922_001__orders.sql");
        Files.createDirectories(script.getParent());
        Files.writeString(script, "EXECUTE 'not parsed';\n");
        String hash = sha256(Files.readAllBytes(script));
        Path manifest = temp.resolve("db/egon-mp/repository-manifest.json");
        Files.writeString(manifest, manifestJson(hash));
        CodegenConfigBO config = CodegenConfigBO.builder()
                .input(CodegenConfigBO.InputBO.builder()
                        .mode("manifest")
                        .resourceRoot(temp.toString())
                        .manifest("db/egon-mp/repository-manifest.json")
                        .build())
                .build();
        Files.writeString(script, "CREATE TABLE drifted(id INT);\n");
        DdlParseException drifted = assertThrows(DdlParseException.class,
                () -> service.read(config, Map.of(), List.of()));
        assertEquals(DdlSchemaService.CHECKSUM_DRIFT, drifted.getCode());

        Files.writeString(script, "EXECUTE 'not parsed';\n");
        DdlParseException prefix = assertThrows(DdlParseException.class,
                () -> service.read(config, Map.of(), List.of("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")));
        assertEquals(DdlSchemaService.CHECKSUM_DRIFT, prefix.getCode());
    }

    @Test
    void cannotMergeDifferentPhysicalSchemas() throws Exception {
        String sql = """
                CREATE TABLE orders_0 (id BIGINT PRIMARY KEY, code VARCHAR(64));
                CREATE TABLE orders_1 (id BIGINT PRIMARY KEY, code VARCHAR(32));
                """;
        DdlParseException exception = assertThrows(DdlParseException.class, () -> service.read(
                configSql(sql), Map.of("orders", List.of("orders_0", "orders_1")), List.of()));
        assertEquals(DdlSchemaService.PHYSICAL_SCHEMA_MISMATCH, exception.getCode());
    }

    @Test
    void alterWithoutBaselineFailsAndNoRunnerIsInvoked() throws Exception {
        DdlParseException exception = assertThrows(DdlParseException.class,
                () -> service.read(configSql("ALTER TABLE orders ADD COLUMN note VARCHAR(10);"), Map.of(), List.of()));
        assertEquals(DdlSchemaService.MISSING_SCHEMA_BASELINE, exception.getCode());
        String adapter = Files.readString(Path.of(
                "src/main/java/top/egon/cola/component/codegen/ddl/PostgreDdlAdapter.java"));
        String schemaService = Files.readString(Path.of(
                "src/main/java/top/egon/cola/component/codegen/ddl/DdlSchemaService.java"));
        assertFalse(adapter.contains("EgonColaPostgreDdlRunner"));
        assertFalse(adapter.contains("DriverManager"));
        assertFalse(schemaService.contains("EgonColaPostgreDdlRunner"));
        assertFalse(schemaService.contains("getConnection"));
    }

    private CodegenSchemaBO read(Path path) {
        return service.read(config(path), Map.of(), List.of());
    }

    private static CodegenConfigBO config(Path... files) {
        return CodegenConfigBO.builder()
                .input(CodegenConfigBO.InputBO.builder()
                        .mode("schema")
                        .schemaFiles(java.util.Arrays.stream(files).map(Path::toString).toList())
                        .build())
                .build();
    }

    private static CodegenConfigBO configSql(String sql) throws Exception {
        Path path = Files.createTempFile("egon-ddl", ".sql");
        Files.writeString(path, sql);
        return config(path);
    }

    private static CodegenSchemaBO.TableBO table(CodegenSchemaBO schema, String name) {
        return schema.getTables().stream()
                .filter(table -> name.equals(table.getLogicalName()))
                .findFirst()
                .orElseThrow();
    }

    private static CodegenSchemaBO.ColumnBO column(CodegenSchemaBO.TableBO table, String name) {
        return table.getColumns().stream().filter(column -> name.equals(column.getName())).findFirst().orElseThrow();
    }

    private static CodegenSchemaBO.IndexBO index(CodegenSchemaBO.TableBO table, String name) {
        return table.getIndexes().stream().filter(index -> name.equals(index.getName())).findFirst().orElseThrow();
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isDirectory(current.resolve("egon-cola-archetypes"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("repository root not found");
        }
        return current;
    }

    private static String manifestJson(String hash) {
        return """
                {
                  "family": "web",
                  "scripts": [
                    {
                      "version": "20260922_001",
                      "path": "db/egon-mp/V20260922_001__orders.sql",
                      "sha256": "%s"
                    }
                  ]
                }
                """.formatted(hash);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
