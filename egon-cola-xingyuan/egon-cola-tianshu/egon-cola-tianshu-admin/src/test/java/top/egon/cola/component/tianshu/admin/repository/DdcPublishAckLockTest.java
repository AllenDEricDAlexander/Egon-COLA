package top.egon.cola.component.tianshu.admin.repository;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class DdcPublishAckLockTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void conflictingAckTransactionsSerializeOnTheTargetRow() throws Exception {
        createAckTable();
        insertTarget("change-lock");

        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondExecuting = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> {
                try (Connection connection = connection()) {
                    connection.setAutoCommit(false);
                    String status = selectForUpdate(connection, "change-lock");
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE ddc_publish_ack SET ack_status = ? WHERE tenant_id = ? AND change_id = ?")) {
                        update.setString(1, "FAILED");
                        update.setLong(2, 101L);
                        update.setString(3, "change-lock");
                        update.executeUpdate();
                    }
                    firstLocked.countDown();
                    await(releaseFirst);
                    connection.commit();
                    return status;
                }
            });

            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                try (Connection connection = connection()) {
                    connection.setAutoCommit(false);
                    secondExecuting.countDown();
                    return selectForUpdate(connection, "change-lock");
                }
            });

            assertThat(secondExecuting.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS)).isNull();
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("FAILED");
        } finally {
            releaseFirst.countDown();
        }
    }

    private void createAckTable() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE ddc_publish_ack (tenant_id BIGINT NOT NULL, "
                    + "change_id TEXT NOT NULL, instance_id TEXT NOT NULL, lease_id TEXT NOT NULL, "
                    + "deleted_at TIMESTAMP NULL, ack_status TEXT NULL, "
                    + "PRIMARY KEY (tenant_id, change_id, instance_id, lease_id))");
        }
    }

    private void insertTarget(String changeId) throws Exception {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ddc_publish_ack (tenant_id, change_id, instance_id, lease_id) VALUES (?, ?, ?, ?)")) {
            statement.setLong(1, 101L);
            statement.setString(2, changeId);
            statement.setString(3, "instance-1");
            statement.setString(4, "lease-1");
            statement.executeUpdate();
        }
    }

    private String selectForUpdate(Connection connection, String changeId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT ack_status FROM ddc_publish_ack "
                + "WHERE tenant_id = ? AND change_id = ? AND instance_id = ? AND lease_id = ? "
                + "AND deleted_at IS NULL FOR UPDATE")) {
            statement.setLong(1, 101L);
            statement.setString(2, changeId);
            statement.setString(3, "instance-1");
            statement.setString(4, "lease-1");
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString("ack_status") : null;
            }
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("timed out waiting for ACK lock test");
        }
    }
}
