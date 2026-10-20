package top.egon.cola.component.tianshu.admin.integration;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TEST-004/005：DDL-201 使用反馈表在真实 PostgreSQL 上的唯一性、租户隔离、评分认领与保留期语义。
 * SQL 与 DdcAiUsageFeedbackMapper.xml 的认领/保留语句保持一致。
 * / TEST-004/005: uniqueness, tenant isolation, judge claims and retention of the DDL-201 feedback table on real
 * PostgreSQL. The SQL mirrors the claim and retention statements of DdcAiUsageFeedbackMapper.xml.
 */
class DdcAiFeedbackPersistenceIT {

    static final String CLAIM_SQL = "SELECT f.id FROM ddc_ai_usage_feedback_t0 f "
            + "WHERE f.tenant_id = ? AND f.deleted_at IS NULL AND f.resource_id = ? "
            + "AND f.explicit_score IS NULL AND f.judge_attempts < 3 "
            + "AND (f.judge_state = 'PENDING' OR (f.judge_state = 'JUDGING' AND f.judge_lease_until < ?)) "
            + "ORDER BY f.occurred_at, f.id LIMIT ? FOR UPDATE";

    @Test
    void usageIdIsUniquePerTenantButReusableAcrossTenants() throws Exception {
        Fixture first = Fixture.create();
        Fixture second = Fixture.create();
        String usageId = usageId();
        try (Connection connection = open()) {
            insertFeedback(connection, first, DdcAiPersistenceIT.nextId(), usageId, "PENDING", Instant.now());
            connection.commit();
            assertThatThrownBy(() -> insertFeedback(connection, first, DdcAiPersistenceIT.nextId(), usageId,
                    "PENDING", Instant.now()))
                    .isInstanceOf(SQLException.class)
                    .satisfies(failure -> assertThat(((SQLException) failure).getSQLState()).isEqualTo("23505"));
            connection.rollback();
            insertFeedback(connection, second, DdcAiPersistenceIT.nextId(), usageId, "PENDING", Instant.now());
            connection.commit();
            assertThat(countLive(connection, first)).isEqualTo(1);
            assertThat(countLive(connection, second)).isEqualTo(1);
        } finally {
            first.cleanup();
            second.cleanup();
        }
    }

    @Test
    void judgeStateChecksRejectInconsistentRows() throws Exception {
        Fixture fixture = Fixture.create();
        try (Connection connection = open()) {
            assertThatThrownBy(() -> insertFeedback(connection, fixture, DdcAiPersistenceIT.nextId(), usageId(),
                    "DONE", Instant.now()))
                    .isInstanceOf(SQLException.class)
                    .satisfies(failure -> assertThat(((SQLException) failure).getSQLState()).isEqualTo("23514"));
            connection.rollback();
            assertThatThrownBy(() -> insertFeedback(connection, fixture, DdcAiPersistenceIT.nextId(), usageId(),
                    "JUDGING", Instant.now()))
                    .isInstanceOf(SQLException.class)
                    .satisfies(failure -> assertThat(((SQLException) failure).getSQLState()).isEqualTo("23514"));
            connection.rollback();
        } finally {
            fixture.cleanup();
        }
    }

    @Test
    void concurrentJudgeClaimsNeverOverlap() throws Exception {
        Fixture fixture = Fixture.create();
        try (Connection connection = open()) {
            Instant base = Instant.now().minus(1, ChronoUnit.HOURS);
            for (int index = 0; index < 40; index++) {
                insertFeedback(connection, fixture, DdcAiPersistenceIT.nextId(), usageId(), "PENDING",
                        base.plusSeconds(index));
            }
            connection.commit();
        }
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
                Future<List<Long>> a = pool.submit(() -> claimAndHold(fixture, 25, ready, start));
                Future<List<Long>> b = pool.submit(() -> claimAndHold(fixture, 25, ready, start));
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                List<Long> first = a.get(60, TimeUnit.SECONDS);
                List<Long> second = b.get(60, TimeUnit.SECONDS);
                Set<Long> union = new HashSet<>(first);
                union.addAll(second);
                assertThat(first).doesNotContainAnyElementsOf(second);
                assertThat(union).hasSize(40);
            }
        } finally {
            fixture.cleanup();
        }
    }

    @Test
    void expiredJudgeLeaseIsReclaimedButLiveLeaseIsNot() throws Exception {
        Fixture fixture = Fixture.create();
        long expired = DdcAiPersistenceIT.nextId();
        long live = DdcAiPersistenceIT.nextId();
        Instant now = Instant.now();
        try (Connection connection = open()) {
            insertJudging(connection, fixture, expired, now.minusSeconds(60));
            insertJudging(connection, fixture, live, now.plusSeconds(600));
            connection.commit();
            List<Long> claimed = claim(connection, fixture, 10, now);
            connection.rollback();
            assertThat(claimed).containsExactly(expired);
        } finally {
            fixture.cleanup();
        }
    }

    @Test
    void retentionSoftDeletesExpiredRowsInBatchesAndKeepsRecentOnes() throws Exception {
        Fixture fixture = Fixture.create();
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        try (Connection connection = open()) {
            for (int index = 0; index < 5; index++) {
                insertFeedback(connection, fixture, DdcAiPersistenceIT.nextId(), usageId(), "NOT_REQUIRED",
                        cutoff.minus(index + 1, ChronoUnit.HOURS));
            }
            long recent = DdcAiPersistenceIT.nextId();
            insertFeedback(connection, fixture, recent, usageId(), "NOT_REQUIRED", Instant.now());
            connection.commit();

            int purged = 0;
            int rounds = 0;
            int batch;
            do {
                batch = purgeBatch(connection, fixture, cutoff, 2);
                connection.commit();
                purged += batch;
                rounds++;
            } while (batch == 2);
            assertThat(purged).isEqualTo(5);
            assertThat(rounds).isEqualTo(3);
            assertThat(countLive(connection, fixture)).isEqualTo(1);
        } finally {
            fixture.cleanup();
        }
    }

    private static List<Long> claimAndHold(Fixture fixture, int limit, CountDownLatch ready,
            CountDownLatch start) throws Exception {
        try (Connection connection = open()) {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("CLAIM_START_TIMEOUT");
            Instant now = Instant.now();
            List<Long> ids = claim(connection, fixture, limit, now);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE ddc_ai_usage_feedback_t0 SET judge_state = 'JUDGING', judge_attempts = judge_attempts + 1, "
                            + "judge_lease_until = ?, version = version + 1 WHERE tenant_id = ? AND id = ?")) {
                for (Long id : ids) {
                    update.setTimestamp(1, Timestamp.from(now.plusSeconds(300)));
                    update.setLong(2, fixture.tenantId());
                    update.setLong(3, id);
                    update.addBatch();
                }
                update.executeBatch();
            }
            connection.commit();
            return ids;
        }
    }

    static List<Long> claim(Connection connection, Fixture fixture, int limit, Instant now) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(CLAIM_SQL)) {
            statement.setQueryTimeout(5);
            statement.setLong(1, fixture.tenantId());
            statement.setLong(2, fixture.resourceId());
            statement.setTimestamp(3, Timestamp.from(now));
            statement.setInt(4, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) ids.add(result.getLong(1));
            }
        }
        return ids;
    }

    private static int purgeBatch(Connection connection, Fixture fixture, Instant before, int limit)
            throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT f.id FROM ddc_ai_usage_feedback_t0 f WHERE f.tenant_id = ? AND f.deleted_at IS NULL "
                        + "AND f.occurred_at < ? ORDER BY f.id LIMIT ? FOR UPDATE")) {
            select.setQueryTimeout(5);
            select.setLong(1, fixture.tenantId());
            select.setTimestamp(2, Timestamp.from(before));
            select.setInt(3, limit);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) ids.add(result.getLong(1));
            }
        }
        if (ids.isEmpty()) return 0;
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE ddc_ai_usage_feedback_t0 SET deleted_at = CURRENT_TIMESTAMP AT TIME ZONE 'UTC', "
                        + "version = version + 1 WHERE tenant_id = ? AND deleted_at IS NULL AND id = ANY (?)")) {
            update.setLong(1, fixture.tenantId());
            update.setArray(2, connection.createArrayOf("bigint", ids.toArray()));
            return update.executeUpdate();
        }
    }

    private static long countLive(Connection connection, Fixture fixture) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count(*) FROM ddc_ai_usage_feedback_t0 WHERE tenant_id = ? AND deleted_at IS NULL")) {
            statement.setLong(1, fixture.tenantId());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    static void insertFeedback(Connection connection, Fixture fixture, long id, String usageId, String judgeState,
                               Instant occurredAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO ddc_ai_usage_feedback_t0 (id, tenant_id, create_user_id, create_time, update_user_id, "
                        + "update_time, deleted_at, version, resource_id, version_id, usage_id, reporter_credential_id, "
                        + "biz_code, env_code, app_code, outcome, explicit_score, judge_score, judge_state, "
                        + "judge_attempts, judge_lease_until, occurred_at) VALUES (?, ?, 'ai-it', clock_timestamp(), "
                        + "'ai-it', clock_timestamp(), NULL, 0, ?, ?, ?, 'it-credential', ?, 'it-env', 'it-app', "
                        + "'FAILURE', NULL, NULL, ?, 0, NULL, ?)")) {
            statement.setLong(1, id);
            statement.setLong(2, fixture.tenantId());
            statement.setLong(3, fixture.resourceId());
            statement.setLong(4, fixture.versionId());
            statement.setString(5, usageId);
            statement.setString(6, fixture.bizCode());
            statement.setString(7, judgeState);
            statement.setTimestamp(8, Timestamp.from(occurredAt));
            statement.executeUpdate();
        }
    }

    private static void insertJudging(Connection connection, Fixture fixture, long id, Instant leaseUntil)
            throws SQLException {
        insertFeedback(connection, fixture, id, usageId(), "PENDING", Instant.now().minusSeconds(3600));
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE ddc_ai_usage_feedback_t0 SET judge_state = 'JUDGING', judge_attempts = 1, "
                        + "judge_lease_until = ? WHERE tenant_id = ? AND id = ?")) {
            statement.setTimestamp(1, Timestamp.from(leaseUntil));
            statement.setLong(2, fixture.tenantId());
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    /** 手动提交的连接；未开启 IT 环境时跳过。 / Manual-commit connection; skipped without the IT environment. */
    static Connection open() throws SQLException {
        Connection connection = DdcAiPersistenceIT.openPostgres();
        connection.setAutoCommit(false);
        return connection;
    }

    static String usageId() {
        return "it" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 每个测试独立的租户、命名空间、资源与已发布版本；清理按外键逆序硬删除本租户的测试行。
     * / Per-test tenant, namespace, resource and version; cleanup hard-deletes this tenant's rows in reverse FK
     * order.
     */
    record Fixture(long tenantId, long namespaceId, long resourceId, long versionId, String bizCode) {

        static Fixture create() throws SQLException {
            try (Connection connection = open()) {
                return createWith(connection);
            }
        }

        /** 在给定连接上创建并提交父行。 / Creates and commits the parent rows on the given connection. */
        static Fixture createWith(Connection connection) throws SQLException {
            String suffix = UUID.randomUUID().toString().replace("-", "");
            Fixture fixture = new Fixture(DdcAiPersistenceIT.nextId(), DdcAiPersistenceIT.nextId(),
                    DdcAiPersistenceIT.nextId(), DdcAiPersistenceIT.nextId(), "it-biz-" + suffix.substring(0, 12));
            DdcAiPersistenceIT.insertNamespace(connection, fixture.tenantId(), fixture.namespaceId(),
                    fixture.bizCode(), "it-space-" + suffix.substring(0, 12));
            DdcAiPersistenceIT.insertResource(connection, fixture.resourceId(), fixture.tenantId(),
                    fixture.namespaceId(), fixture.bizCode(), "it-env", "it-app", "PROMPT",
                    "it-" + suffix, "draft");
            DdcAiPersistenceIT.insertVersion(connection, fixture.versionId(), fixture.tenantId(),
                    fixture.resourceId(), "PROMPT", "published prompt");
            connection.commit();
            return fixture;
        }

        void cleanup() throws SQLException {
            try (Connection connection = open()) {
                cleanupWith(connection);
            }
        }

        void cleanupWith(Connection connection) throws SQLException {
            connection.rollback();
            for (String table : List.of("ddc_ai_optimization_run_t0", "ddc_ai_optimization_policy_t0",
                    "ddc_ai_usage_feedback_t0", "ddc_ai_publication_t0", "ddc_ai_version_t0",
                    "ddc_ai_resource_t0", "ddc_namespace_t0")) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM " + table + " WHERE tenant_id = ?")) {
                    statement.setLong(1, tenantId);
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }
}
