package top.egon.cola.component.gateway.admin.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAdminSchemaTest {

    @Test
    void oneMigrationContainsEveryControlPlaneAggregate() throws IOException {
        String root = "db/migration";
        try (java.util.stream.Stream<java.nio.file.Path> migrations =
                     java.nio.file.Files.list(java.nio.file.Path.of(
                             "src/main/resources",
                             root
                     ))) {
            assertEquals(1, migrations.filter(
                    path -> path.getFileName().toString().endsWith(".sql")
            ).count());
        }
        String migration = new String(
                getClass().getClassLoader().getResourceAsStream(
                        root + "/V1__create_gateway_admin_schema.sql"
                ).readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertTrue(migration.contains("CREATE TABLE gateway_group"));
        assertTrue(migration.contains("CREATE TABLE gateway_operation"));
        assertTrue(migration.contains("CREATE TABLE gateway_draft"));
        assertTrue(migration.contains("CREATE TABLE gateway_release"));
        assertTrue(migration.contains("CREATE TABLE gateway_audit_log"));
        assertTrue(migration.contains("JSONB"));
    }
}
