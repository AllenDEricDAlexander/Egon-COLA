package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GatewayMcpFlywayPostgresqlIT {

    private static final Set<String> MCP_TABLES = Set.of(
            "gateway_mcp_server",
            "gateway_mcp_managed_tool_override",
            "gateway_mcp_remote_tool_draft",
            "gateway_mcp_resource_draft",
            "gateway_mcp_resource_template_draft",
            "gateway_mcp_prompt_draft",
            "gateway_mcp_task_policy_draft",
            "gateway_mcp_app_artifact",
            "gateway_mcp_app_binding_draft",
            "gateway_mcp_remote_provider",
            "gateway_mcp_remote_capability",
            "gateway_mcp_remote_mount_draft",
            "gateway_mcp_approval",
            "gateway_mcp_task_instance"
    );

    private final String jdbcUrl = requiredEnvironment(
            "GATEWAY_MCP_TEST_POSTGRES_URL"
    );

    private final String user = requiredEnvironment(
            "GATEWAY_MCP_TEST_POSTGRES_USER"
    );

    private final String password = requiredEnvironment(
            "GATEWAY_MCP_TEST_POSTGRES_PASSWORD"
    );

    private final String upgradeSchema = schema("upgrade");

    private final String emptySchema = schema("empty");

    private final String storeSchema = schema("store");

    private final String toolMigrationSchema = schema("tool_migration");

    @BeforeAll
    void createSchemas() throws SQLException {
        execute("CREATE SCHEMA " + upgradeSchema);
        execute("CREATE SCHEMA " + emptySchema);
        execute("CREATE SCHEMA " + storeSchema);
        execute("CREATE SCHEMA " + toolMigrationSchema);
    }

    @AfterAll
    void dropSchemas() throws SQLException {
        execute("DROP SCHEMA IF EXISTS " + upgradeSchema + " CASCADE");
        execute("DROP SCHEMA IF EXISTS " + emptySchema + " CASCADE");
        execute("DROP SCHEMA IF EXISTS " + storeSchema + " CASCADE");
        execute("DROP SCHEMA IF EXISTS " + toolMigrationSchema + " CASCADE");
    }

    @Test
    void latestMigrationsCreateAllMcpTablesAndUpgradeV1ThroughV6()
            throws SQLException {
        Flyway firstSix = flyway(upgradeSchema, "6");
        firstSix.migrate();
        assertFalse(tableNames(upgradeSchema).contains(
                "gateway_mcp_server"
        ));

        Flyway latest = flyway(upgradeSchema, null);
        latest.migrate();

        assertTrue(tableNames(upgradeSchema).containsAll(MCP_TABLES));
        assertEquals("11", latest.info().current().getVersion().getVersion());
    }

    @Test
    void v10MigratesOnlyRemoteToolsAndPreservesTheirIds() throws SQLException {
        Flyway throughNine = flyway(toolMigrationSchema, "9");
        throughNine.migrate();
        JdbcTemplate jdbc = jdbc(toolMigrationSchema);
        Instant now = Instant.parse("2026-08-02T00:00:00Z");
        Timestamp timestamp = Timestamp.from(now);
        jdbc.update("""
                INSERT INTO gateway_group(
                    id, gateway_group_code, display_name, env, namespace,
                    enabled, revision, deleted, created_at, created_by,
                    updated_at, updated_by
                ) VALUES (
                    'group-1', 'group-1', 'Group 1', 'DEV', 'default',
                    TRUE, 0, FALSE, ?, 'admin', ?, 'admin'
                )
                """, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO gateway_application(
                    id, biz_code, application_code, display_name, env,
                    namespace, revision, deleted, created_at, created_by,
                    updated_at, updated_by
                ) VALUES (
                    'app-1', 'biz', 'orders', 'Orders', 'DEV', 'default',
                    0, FALSE, ?, 'admin', ?, 'admin'
                )
                """, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO gateway_business_domain(
                    id, application_id, code, display_name, deleted,
                    created_at, updated_at
                ) VALUES (
                    'business-1', 'app-1', 'orders', 'Orders', FALSE, ?, ?
                )
                """, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO gateway_entity_domain(
                    id, business_domain_id, code, display_name, deleted,
                    created_at, updated_at
                ) VALUES (
                    'entity-1', 'business-1', 'order', 'Order', FALSE, ?, ?
                )
                """, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO gateway_interface_group(
                    id, entity_domain_id, code, display_name, source_type,
                    deleted, created_at, updated_at
                ) VALUES (
                    'interface-1', 'entity-1', 'orders', 'Orders', 'STARTER',
                    FALSE, ?, ?
                )
                """, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO gateway_operation(
                    id, application_id, interface_group_id, operation_key,
                    protocol, method_identity, external_accessible,
                    provider_service_identity, source_type, lifecycle_status,
                    revision, created_at, updated_at
                ) VALUES (
                    'operation-1', 'app-1', 'interface-1', 'orders.get',
                    'HTTP', 'GET /orders/{id}', FALSE, '{}'::jsonb,
                    'STARTER', 'ACTIVE', 0, ?, ?
                )
                """, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO gateway_mcp_server(
                    id, gateway_group_id, server_code, display_name,
                    dialects, resource_uri, enabled, revision, deleted,
                    created_at, created_by, updated_at, updated_by
                ) VALUES (
                    'server-1', 'group-1', 'orders', 'Orders',
                    '["STABLE_2025_11_25"]'::jsonb,
                    'https://resource.egon.top/gateway-mcp', TRUE,
                    0, FALSE, ?, 'admin', ?, 'admin'
                )
                """, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO gateway_mcp_remote_provider(
                    id, gateway_group_id, provider_code, display_name,
                    dialect, transport_type, endpoint_reference, status,
                    enabled, revision, deleted, created_at, created_by,
                    updated_at, updated_by
                ) VALUES (
                    'provider-1', 'group-1', 'remote', 'Remote',
                    'STABLE_2025_11_25', 'STREAMABLE_HTTP', 'remote:orders',
                    'CONFIGURED', TRUE, 0, FALSE, ?, 'admin', ?, 'admin'
                )
                """, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO gateway_mcp_remote_mount_draft(
                    id, gateway_group_id, server_id, provider_id, namespace,
                    capability_fingerprint, content, enabled, revision,
                    deleted, created_at, created_by, updated_at, updated_by
                ) VALUES (
                    'mount-1', 'group-1', 'server-1', 'provider-1', 'remote',
                    'fingerprint', '{}'::jsonb, TRUE, 0, FALSE,
                    ?, 'admin', ?, 'admin'
                )
                """, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO gateway_mcp_tool_draft(
                    id, gateway_group_id, server_id, tool_name, source_type,
                    operation_id, remote_mount_id, content, enabled, revision,
                    deleted, created_at, created_by, updated_at, updated_by
                ) VALUES
                    ('local-tool', 'group-1', 'server-1', 'orders.get',
                     'LOCAL_OPERATION', 'operation-1', NULL,
                     '{"sourceType":"LOCAL_OPERATION"}'::jsonb,
                     TRUE, 0, FALSE, ?, 'admin', ?, 'admin'),
                    ('remote-tool', 'group-1', 'server-1', 'remote.get',
                     'REMOTE_MCP', NULL, 'mount-1',
                     '{"sourceType":"REMOTE_MCP","riskLevel":"HIGH"}'::jsonb,
                     TRUE, 4, FALSE, ?, 'admin', ?, 'admin')
                """,
                timestamp, timestamp,
                timestamp, timestamp
        );

        Flyway latest = flyway(toolMigrationSchema, null);
        latest.migrate();

        assertFalse(tableNames(toolMigrationSchema).contains(
                "gateway_mcp_tool_draft"
        ));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM gateway_mcp_remote_tool_draft",
                Integer.class
        ));
        assertEquals("remote-tool", jdbc.queryForObject(
                "SELECT id FROM gateway_mcp_remote_tool_draft",
                String.class
        ));
        assertEquals(4L, jdbc.queryForObject(
                "SELECT revision FROM gateway_mcp_remote_tool_draft",
                Long.class
        ));
        assertFalse(Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT content ? 'sourceType' "
                        + "FROM gateway_mcp_remote_tool_draft",
                Boolean.class
        )));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM gateway_mcp_managed_tool_override",
                Integer.class
        ));
    }

    @Test
    void emptyDatabaseMigrationCreatesSameMcpSchemaAndConstraints()
            throws SQLException {
        Flyway latest = flyway(emptySchema, null);
        latest.migrate();

        assertTrue(tableNames(emptySchema).containsAll(MCP_TABLES));
        assertTrue(constraintNames(emptySchema).containsAll(Set.of(
                "ck_gateway_mcp_approval_status",
                "ck_gateway_mcp_task_state",
                "ck_gateway_mcp_provider_transport"
        )));
    }

    @Test
    void storesEnforceRevisionOneTimeApprovalAndLeaseClaim() {
        flyway(storeSchema, null).migrate();
        JdbcTemplate jdbc = jdbc(storeSchema);
        Instant now = Instant.parse("2026-08-02T00:00:00Z");
        AdminActor actor = new AdminActor(
                "admin-1",
                AdminActor.ActorType.USER,
                Set.of(),
                Set.of()
        );
        jdbc.update("""
                INSERT INTO gateway_group(
                    id, gateway_group_code, display_name, env, namespace,
                    enabled, revision, deleted, created_at, created_by,
                    updated_at, updated_by
                ) VALUES (
                    'group-1', 'group-1', 'Group 1', 'DEV', 'default',
                    TRUE, 0, FALSE, ?, 'admin-1', ?, 'admin-1'
                )
                """, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                INSERT INTO gateway_mcp_server(
                    id, gateway_group_id, server_code, display_name,
                    dialects, resource_uri, list_cache_ttl_seconds,
                    enabled, revision, deleted, created_at, created_by,
                    updated_at, updated_by
                ) VALUES (
                    'server-1', 'group-1', 'billing', 'Billing',
                    '["STABLE_2025_11_25"]'::jsonb,
                    'https://resource.egon.top/gateway-mcp', 30,
                    TRUE, 0, FALSE, ?, 'admin-1', ?, 'admin-1'
                )
                """, Timestamp.from(now), Timestamp.from(now));

        JdbcMcpCapabilityDraftStore capabilities =
                new JdbcMcpCapabilityDraftStore(jdbc, new ObjectMapper());
        var policy = new JdbcMcpCapabilityDraftStore.CapabilityDraft(
                JdbcMcpCapabilityDraftStore.CapabilityKind.TASK_POLICY,
                "policy-1",
                "group-1",
                "server-1",
                "invoice.export",
                Map.of("maxAttempts", 3),
                true,
                0
        );
        assertEquals(0, capabilities.save(
                policy, 0, actor, now
        ).revision());
        assertEquals(1, capabilities.save(
                policy, 0, actor, now.plusSeconds(1)
        ).revision());
        assertThrows(
                GatewayAdminRevisionConflictException.class,
                () -> capabilities.save(
                        policy, 0, actor, now.plusSeconds(2)
                )
        );

        JdbcMcpApprovalStore approvals = new JdbcMcpApprovalStore(jdbc);
        approvals.issue(new JdbcMcpApprovalStore.Approval(
                "approval-1",
                "a".repeat(64),
                "subject-1",
                "tenant-1",
                "client-1",
                "billing",
                "invoice.approve",
                "b".repeat(64),
                now,
                now.plusSeconds(60)
        ));
        assertTrue(approvals.consume(
                "a".repeat(64), "subject-1", "tenant-1", "client-1",
                "billing", "invoice.approve", "b".repeat(64), now
        ));
        assertFalse(approvals.consume(
                "a".repeat(64), "subject-1", "tenant-1", "client-1",
                "billing", "invoice.approve", "b".repeat(64), now
        ));

        JdbcMcpTaskStore tasks = new JdbcMcpTaskStore(
                jdbc,
                new ObjectMapper()
        );
        tasks.create(new JdbcMcpTaskStore.TaskRecord(
                "task-1", "principal-1", "subject-1", "tenant-1",
                "client-1", "billing", "invoice.export",
                "c".repeat(64), "WORKING", Map.of("format", "csv"),
                null, null, null, null, now.plusSeconds(300),
                now.plusSeconds(600), 0, 3, 0, now, now
        ));
        assertTrue(tasks.claim(
                "task-1", "worker-1", now, now.plusSeconds(30), 0
        ));
        assertFalse(tasks.claim(
                "task-1", "worker-2", now, now.plusSeconds(30), 0
        ));
    }

    private Flyway flyway(String schema, String target) {
        var configuration = Flyway.configure()
                .dataSource(jdbcUrl, user, password)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private JdbcTemplate jdbc(String schema) {
        String separator = jdbcUrl.contains("?") ? "&" : "?";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                jdbcUrl + separator + "currentSchema=" + schema,
                user,
                password
        );
        return new JdbcTemplate(dataSource);
    }

    private Set<String> tableNames(String schema) throws SQLException {
        return values("""
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = '%s'
                """.formatted(schema));
    }

    private Set<String> constraintNames(String schema) throws SQLException {
        return values("""
                SELECT constraint_name
                  FROM information_schema.table_constraints
                 WHERE table_schema = '%s'
                """.formatted(schema));
    }

    private Set<String> values(String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            Set<String> values = new HashSet<>();
            while (result.next()) {
                values.add(result.getString(1));
            }
            return Set.copyOf(values);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    private String schema(String purpose) {
        return "gateway_mcp_"
                + purpose
                + "_"
                + UUID.randomUUID().toString().replace("-", "");
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
