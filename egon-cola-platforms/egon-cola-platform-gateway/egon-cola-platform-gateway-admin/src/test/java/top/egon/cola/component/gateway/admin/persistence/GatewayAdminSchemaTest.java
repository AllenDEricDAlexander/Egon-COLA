package top.egon.cola.component.gateway.admin.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAdminSchemaTest {

    @Test
    void migrationsContainControlPlaneAndObservabilityAggregates()
            throws IOException {
        String root = "db/migration";
        try (java.util.stream.Stream<java.nio.file.Path> migrations =
                     java.nio.file.Files.list(java.nio.file.Path.of(
                             "src/main/resources",
                             root
                     ))) {
            assertEquals(5, migrations.filter(
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

        String observability = new String(
                getClass().getClassLoader().getResourceAsStream(
                        root
                                + "/V2__add_gateway_observability_projection.sql"
                ).readAllBytes(),
                StandardCharsets.UTF_8
        );
        assertTrue(observability.contains(
                "CREATE TABLE gateway_call_event_summary"
        ));
        assertTrue(observability.contains(
                "CREATE TABLE gateway_call_metric_minute"
        ));
        assertTrue(observability.contains(
                "CREATE TABLE gateway_call_event_consume_failure"
        ));

        String lifecycle = new String(
                getClass().getClassLoader().getResourceAsStream(
                        root
                                + "/V3__add_definition_lifecycle_membership.sql"
                ).readAllBytes(),
                StandardCharsets.UTF_8
        );
        assertTrue(lifecycle.contains(
                "CREATE TABLE gateway_definition_set_operation"
        ));
        assertTrue(lifecycle.contains("activated_at"));
        assertTrue(lifecycle.contains("retired_at"));

        String applicationScope = new String(
                getClass().getClassLoader().getResourceAsStream(
                        root + "/V5__add_gateway_application_biz_scope.sql"
                ).readAllBytes(),
                StandardCharsets.UTF_8
        );
        assertTrue(applicationScope.contains("biz_code"));
    }
}
