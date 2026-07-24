package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcV2MigrationTest {

    @Test
    void migratesV1AndUsesLeaseIdentityForPublishTargets() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            execute(connection, script("db/sqlite/V1__create_ddc_schema.sql"));
            execute(connection, script("db/sqlite/V2__add_lease_and_sync_publish.sql"));

            assertThatCode(() -> executeStatement(connection, """
                    insert into ddc_instance (
                        id, instance_id, lease_id, lease_expire_at, created_at, updated_at
                    ) values (
                        'i1', 'instance-1', 'lease-1', '2026-07-24 12:00:30',
                        '2026-07-24 12:00:00', '2026-07-24 12:00:00'
                    )
                    """)).doesNotThrowAnyException();
            assertThatCode(() -> executeStatement(connection, """
                    insert into ddc_publish_task (
                        id, change_id, content_checksum, attempt_count,
                        dispatched_at, completed_at, failure_stage, created_at, updated_at
                    ) values (
                        't1', 'change-1', 'checksum-1', 1,
                        '2026-07-24 12:00:01', null, null,
                        '2026-07-24 12:00:00', '2026-07-24 12:00:00'
                    )
                    """)).doesNotThrowAnyException();

            insertAck(connection, "a1", "lease-1");
            assertThatCode(() -> insertAck(connection, "a2", "lease-2"))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> insertAck(connection, "a3", "lease-1"))
                    .isInstanceOf(SQLException.class);
        }
    }

    private void insertAck(Connection connection, String id, String leaseId) throws SQLException {
        executeStatement(connection, """
                insert into ddc_publish_ack (
                    id, change_id, instance_id, lease_id, content_checksum
                ) values (
                    '%s', 'change-1', 'instance-1', '%s', 'checksum-1'
                )
                """.formatted(id, leaseId));
    }

    private void execute(Connection connection, String sql) throws SQLException {
        for (String statement : sql.split(";")) {
            if (!statement.isBlank()) {
                executeStatement(connection, statement);
            }
        }
    }

    private void executeStatement(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String script(String path) throws Exception {
        try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("missing migration: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
