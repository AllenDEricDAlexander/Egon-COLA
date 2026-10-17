package top.egon.cola.component.tianshu.admin.integration;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.admin.integration.DdcAiFeedbackPersistenceIT.Fixture;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TEST-011：每个资源同一时刻最多一个 PENDING/RUNNING 运行，请求幂等键租户内唯一，认领 CAS 互斥。
 * / TEST-011: at most one PENDING/RUNNING run per resource, tenant-unique request keys and exclusive claim CAS.
 */
class DdcAiOptimizationConcurrencyIT {

    @Test
    void onlyOneActiveRunPerResourceUnderConcurrentInserts() throws Exception {
        Fixture fixture = Fixture.create();
        long policyId = DdcAiPersistenceIT.nextId();
        try (Connection connection = DdcAiFeedbackPersistenceIT.open()) {
            insertPolicy(connection, fixture, policyId);
            connection.commit();
        }
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
                Future<String> first = pool.submit(() -> insertAtBarrier(ready, start, fixture, policyId, 1));
                Future<String> second = pool.submit(() -> insertAtBarrier(ready, start, fixture, policyId, 2));
                assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                assertThat(List.of(first.get(60, TimeUnit.SECONDS), second.get(60, TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder("INSERTED", "UNIQUE_CONFLICT");
            }
        } finally {
            fixture.cleanup();
        }
    }

    @Test
    void finishedRunsDoNotBlockANewActiveRunButRequestIdsStayUnique() throws Exception {
        Fixture fixture = Fixture.create();
        long policyId = DdcAiPersistenceIT.nextId();
        String requestId = "it-req-" + DdcAiFeedbackPersistenceIT.usageId().substring(0, 20);
        try (Connection connection = DdcAiFeedbackPersistenceIT.open()) {
            insertPolicy(connection, fixture, policyId);
            long finished = insertRun(connection, fixture, policyId, 1, requestId);
            finish(connection, fixture, finished);
            connection.commit();

            insertRun(connection, fixture, policyId, 2, null);
            connection.commit();

            assertThatThrownBy(() -> insertRun(connection, fixture, policyId, 3, requestId))
                    .isInstanceOf(SQLException.class)
                    .satisfies(failure -> assertThat(((SQLException) failure).getSQLState()).isEqualTo("23505"));
            connection.rollback();
        } finally {
            fixture.cleanup();
        }
    }

    @Test
    void pendingRunClaimIsExclusiveAcrossConnections() throws Exception {
        Fixture fixture = Fixture.create();
        long policyId = DdcAiPersistenceIT.nextId();
        long runId;
        try (Connection connection = DdcAiFeedbackPersistenceIT.open()) {
            insertPolicy(connection, fixture, policyId);
            runId = insertRun(connection, fixture, policyId, 1, null);
            connection.commit();
        }
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
                Future<Integer> first = pool.submit(() -> claimAtBarrier(ready, start, fixture, runId, "node-a"));
                Future<Integer> second = pool.submit(() -> claimAtBarrier(ready, start, fixture, runId, "node-b"));
                assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                assertThat(first.get(60, TimeUnit.SECONDS) + second.get(60, TimeUnit.SECONDS)).isEqualTo(1);
            }
        } finally {
            fixture.cleanup();
        }
    }

    private static String insertAtBarrier(CountDownLatch ready, CountDownLatch start, Fixture fixture,
                                          long policyId, long runNo) throws Exception {
        try (Connection connection = DdcAiFeedbackPersistenceIT.open()) {
            ready.countDown();
            start.await();
            insertRun(connection, fixture, policyId, runNo, null);
            connection.commit();
            return "INSERTED";
        } catch (SQLException failure) {
            if ("23505".equals(failure.getSQLState())) return "UNIQUE_CONFLICT";
            throw failure;
        }
    }

    private static int claimAtBarrier(CountDownLatch ready, CountDownLatch start, Fixture fixture, long runId,
                                      String owner) throws Exception {
        try (Connection connection = DdcAiFeedbackPersistenceIT.open()) {
            ready.countDown();
            start.await();
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE ddc_ai_optimization_run_t0 SET state = 'RUNNING', lease_owner = ?, lease_until = ?, "
                            + "started_at = clock_timestamp(), version = version + 1 "
                            + "WHERE tenant_id = ? AND id = ? AND state = 'PENDING' AND version = 0 "
                            + "AND deleted_at IS NULL")) {
                statement.setString(1, owner);
                statement.setTimestamp(2, Timestamp.from(Instant.now().plusSeconds(600)));
                statement.setLong(3, fixture.tenantId());
                statement.setLong(4, runId);
                int updated = statement.executeUpdate();
                connection.commit();
                return updated;
            }
        }
    }

    static void insertPolicy(Connection connection, Fixture fixture, long policyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ddc_ai_optimization_policy_t0 (id, tenant_id, create_user_id, create_time, "
                        + "update_user_id, update_time, deleted_at, version, resource_id, enabled, auto_publish, "
                        + "optimizer_model_alias, judge_model_alias) VALUES (?, ?, 'ai-it', clock_timestamp(), "
                        + "'ai-it', clock_timestamp(), NULL, 0, ?, TRUE, TRUE, 'gpt-it', 'gpt-it')")) {
            statement.setLong(1, policyId);
            statement.setLong(2, fixture.tenantId());
            statement.setLong(3, fixture.resourceId());
            statement.executeUpdate();
        }
    }

    static long insertRun(Connection connection, Fixture fixture, long policyId, long runNo, String requestId)
            throws SQLException {
        long runId = DdcAiPersistenceIT.nextId();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ddc_ai_optimization_run_t0 (id, tenant_id, create_user_id, create_time, update_user_id, "
                        + "update_time, deleted_at, version, resource_id, policy_id, run_no, iteration_no, \"trigger\", "
                        + "request_id, request_sha256, state, base_version_id, sample_count, threshold_snapshot, "
                        + "window_start, window_end, model_alias) VALUES (?, ?, 'ai-it', clock_timestamp(), 'ai-it', "
                        + "clock_timestamp(), NULL, 0, ?, ?, ?, 1, ?, ?, ?, 'PENDING', ?, 0, 80, ?, ?, 'gpt-it')")) {
            Instant now = Instant.now();
            statement.setLong(1, runId);
            statement.setLong(2, fixture.tenantId());
            statement.setLong(3, fixture.resourceId());
            statement.setLong(4, policyId);
            statement.setLong(5, runNo);
            statement.setString(6, requestId == null ? "SCHEDULED" : "MANUAL");
            statement.setString(7, requestId);
            statement.setString(8, requestId == null ? null : "b".repeat(64));
            statement.setLong(9, fixture.versionId());
            statement.setTimestamp(10, Timestamp.from(now.minusSeconds(86_400)));
            statement.setTimestamp(11, Timestamp.from(now));
            statement.executeUpdate();
        }
        return runId;
    }

    static void finish(Connection connection, Fixture fixture, long runId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE ddc_ai_optimization_run_t0 SET state = 'SKIPPED', error_code = 'BASE_CHANGED', "
                        + "finished_at = clock_timestamp(), version = version + 1 WHERE tenant_id = ? AND id = ?")) {
            statement.setLong(1, fixture.tenantId());
            statement.setLong(2, runId);
            statement.executeUpdate();
        }
    }
}
