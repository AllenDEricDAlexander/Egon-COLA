package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.task.McpTask;
import top.egon.cola.component.gateway.mcp.task.McpTaskExecutor;
import top.egon.cola.component.gateway.mcp.task.McpTaskService;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpTaskRecoveryPostgresqlIT {

    private final String jdbcUrl = environment(
            "GATEWAY_MCP_TEST_POSTGRES_URL"
    );

    private final String user = environment(
            "GATEWAY_MCP_TEST_POSTGRES_USER"
    );

    private final String password = environment(
            "GATEWAY_MCP_TEST_POSTGRES_PASSWORD"
    );

    private final String schema = "gateway_mcp_task_"
            + UUID.randomUUID().toString().replace("-", "");

    private DriverManagerDataSource dataSource;

    @BeforeAll
    void createSchema() throws Exception {
        assumeTrue(jdbcUrl != null && user != null && password != null);
        execute("CREATE SCHEMA " + schema);
        String separator = jdbcUrl.contains("?") ? "&" : "?";
        dataSource = new DriverManagerDataSource(
                jdbcUrl + separator + "currentSchema=" + schema,
                user,
                password
        );
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE gateway_mcp_task_instance (
                        id VARCHAR(64) PRIMARY KEY,
                        principal_fingerprint VARCHAR(128) NOT NULL,
                        subject_id VARCHAR(128) NOT NULL,
                        tenant_id VARCHAR(128) NOT NULL,
                        client_id VARCHAR(128) NOT NULL,
                        server_code VARCHAR(128) NOT NULL,
                        tool_name VARCHAR(256) NOT NULL,
                        request_digest VARCHAR(64) NOT NULL,
                        state VARCHAR(32) NOT NULL,
                        input_payload JSONB,
                        result_payload JSONB,
                        error_payload JSONB,
                        worker_owner VARCHAR(256),
                        lease_until TIMESTAMPTZ,
                        execution_deadline TIMESTAMPTZ NOT NULL,
                        expires_at TIMESTAMPTZ NOT NULL,
                        attempt_count INTEGER NOT NULL DEFAULT 0,
                        max_attempts INTEGER NOT NULL,
                        revision BIGINT NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ NOT NULL,
                        updated_at TIMESTAMPTZ NOT NULL
                    )
                    """);
        }
    }

    @AfterAll
    void dropSchema() throws Exception {
        if (jdbcUrl != null && user != null && password != null) {
            execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void taskCreatedOnNodeAIsLeasedAndCompletedByNodeBAfterCrash() {
        Instant initial = Instant.parse("2026-08-02T08:00:00Z");
        JdbcMcpRuntimeTaskStore store = new JdbcMcpRuntimeTaskStore(
                dataSource,
                new ObjectMapper()
        );
        McpTaskService.Owner owner = new McpTaskService.Owner(
                "subject-1",
                "tenant-1",
                "client-1"
        );
        McpTaskService nodeA = service(store, initial);
        McpTask created = Mono.from(nodeA.create(
                new McpTaskService.CreateRequest(
                        "billing",
                        "export_invoice",
                        "a".repeat(64),
                        Map.of(
                                "operationId", "operation-1",
                                "arguments", Map.of("invoiceId", "invoice-1")
                        ),
                        Duration.ofMinutes(10),
                        Duration.ofHours(1),
                        3
                ),
                owner
        )).block();
        Mono.from(store.leaseNext(
                "node-a",
                initial,
                initial.plusSeconds(30)
        )).block();

        McpTaskService nodeB = service(store, initial.plusSeconds(31));
        McpTaskWorker worker = new McpTaskWorker(
                nodeB,
                task -> Mono.just(McpTaskExecutor.Outcome.completed(
                        Map.of("exportId", "export-1")
                )),
                "node-b",
                Duration.ofSeconds(30),
                Duration.ofSeconds(1)
        );
        Mono.from(worker.runOnce()).block();

        McpTask recovered = Mono.from(nodeB.get(created.id(), owner)).block();
        assertEquals(McpTask.State.COMPLETED, recovered.state());
        assertEquals("export-1", recovered.resultPayload().get("exportId"));
    }

    @Test
    void taskOwnerInputKeyAndCooperativeCancellationAreEnforced() {
        Instant now = Instant.parse("2026-08-02T09:00:00Z");
        JdbcMcpRuntimeTaskStore store = new JdbcMcpRuntimeTaskStore(
                dataSource,
                new ObjectMapper()
        );
        McpTaskService service = service(store, now);
        McpTaskService.Owner owner = new McpTaskService.Owner(
                "subject-2",
                "tenant-1",
                "client-1"
        );
        McpTask task = Mono.from(service.create(
                new McpTaskService.CreateRequest(
                        "billing",
                        "confirm_invoice",
                        "b".repeat(64),
                        Map.of("operationId", "operation-2"),
                        Duration.ofMinutes(10),
                        Duration.ofHours(1),
                        3
                ),
                owner
        )).block();
        assertEquals(43, task.id().length());

        McpProtocolException hidden = assertThrows(
                McpProtocolException.class,
                () -> Mono.from(service.get(
                        task.id(),
                        new McpTaskService.Owner(
                                "subject-other",
                                "tenant-1",
                                "client-1"
                        )
                )).block()
        );
        assertEquals(McpErrorCode.MCP_TASK_NOT_FOUND, hidden.code());

        Mono.from(service.executeNext(
                "node-input",
                leased -> Mono.just(
                        McpTaskExecutor.Outcome.inputRequired(
                                "confirm-1",
                                Map.of("question", "Confirm invoice?")
                        )
                )
        )).block();
        McpTask waiting = Mono.from(service.get(task.id(), owner)).block();
        assertEquals(McpTask.State.INPUT_REQUIRED, waiting.state());

        assertThrows(McpProtocolException.class, () -> Mono.from(
                service.provideInput(
                        task.id(),
                        "wrong-key",
                        Map.of("confirmed", true),
                        owner
                )
        ).block());
        McpTask resumed = Mono.from(service.provideInput(
                task.id(),
                "confirm-1",
                Map.of("confirmed", true),
                owner
        )).block();
        assertEquals(McpTask.State.WORKING, resumed.state());

        McpTask cancelled = Mono.from(service.cancel(
                task.id(),
                owner
        )).block();
        assertEquals(McpTask.State.CANCELLED, cancelled.state());
        assertTrue(cancelled.terminal());
    }

    private McpTaskService service(
            JdbcMcpRuntimeTaskStore store,
            Instant now) {
        return new McpTaskService(
                store,
                new ObjectMapper(),
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofSeconds(30)
        );
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl,
                user,
                password
        ); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String environment(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value;
    }
}
