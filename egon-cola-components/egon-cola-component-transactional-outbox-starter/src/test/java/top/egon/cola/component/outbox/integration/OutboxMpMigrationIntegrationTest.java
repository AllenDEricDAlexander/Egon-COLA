package top.egon.cola.component.outbox.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;
import top.egon.cola.component.outbox.common.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.common.exception.OutboxIdempotencyConflictException;
import top.egon.cola.component.outbox.common.exception.OutboxStorageException;
import top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer;
import top.egon.cola.component.outbox.migration.OutboxLegacyMigrationService;
import top.egon.cola.component.outbox.migration.OutboxMigrationResult;
import top.egon.cola.component.outbox.persistence.OutboxTechnicalContextExecutor;
import top.egon.cola.component.outbox.persistence.converter.OutboxHeadersConverter;
import top.egon.cola.component.outbox.persistence.converter.OutboxMessageConverter;
import top.egon.cola.component.outbox.persistence.repository.OutboxMessageRepository;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxMpMigrationIntegrationTest extends PostgresqlOutboxTestSupport {

    private static final String SOURCE_SCHEMA = "legacy_outbox";

    @BeforeEach
    void prepareLegacySource() {
        physicalJdbcTemplate.execute("DROP SCHEMA IF EXISTS " + SOURCE_SCHEMA + " CASCADE");
        physicalJdbcTemplate.execute("CREATE SCHEMA " + SOURCE_SCHEMA);
        physicalJdbcTemplate.execute("""
                CREATE TABLE legacy_outbox.egon_cola_outbox_message (
                    id bigint PRIMARY KEY,
                    message_id varchar(64) NOT NULL,
                    idempotency_key varchar(256),
                    message_fingerprint char(64) NOT NULL,
                    channel varchar(64) NOT NULL,
                    destination varchar(256) NOT NULL,
                    payload text NOT NULL,
                    content_type varchar(128) NOT NULL,
                    schema_version varchar(32),
                    headers_json text NOT NULL DEFAULT '{}',
                    trace_id varchar(128),
                    status varchar(32) NOT NULL CHECK (status IN (
                        'PENDING', 'PROCESSING', 'RETRY_WAIT', 'SUCCEEDED', 'DEAD')),
                    attempt_count integer NOT NULL CHECK (attempt_count >= 0),
                    max_attempts integer NOT NULL CHECK (max_attempts >= 1),
                    next_attempt_at timestamptz(6) NOT NULL,
                    locked_by varchar(128),
                    locked_until timestamptz(6),
                    last_error_code varchar(64),
                    last_error_message text,
                    created_at timestamptz(6) NOT NULL,
                    updated_at timestamptz(6) NOT NULL,
                    completed_at timestamptz(6)
                )
                """);
        physicalJdbcTemplate.execute("CREATE UNIQUE INDEX uk_legacy_message_id ON legacy_outbox.egon_cola_outbox_message(message_id)");
        physicalJdbcTemplate.execute("CREATE UNIQUE INDEX uk_legacy_idempotency_key ON legacy_outbox.egon_cola_outbox_message(idempotency_key) WHERE idempotency_key IS NOT NULL");
        migrationProperties().setMigrationMode(true);
    }

    @Test
    void shouldCopyEveryLegacyStateAndRawColumnAndResumeWithoutDuplicateRows() {
        insertLegacy(1, "legacy-pending", "key-1", "PENDING", 0, null,
                "{\"name\": \"café\"}", "application/json", "{ \"x-region\": \"東京\" }");
        insertLegacy(2, "legacy-processing", "key-2", "PROCESSING", 1, "worker:lease-1",
                "plain text\n雪", "text/plain", "{\"trace\":\"t-2\"}");
        insertLegacy(3, "legacy-retry", null, "RETRY_WAIT", 2, null,
                "{\"nested\": [3, 2, 1]}", "application/json", "{}");
        insertLegacy(4, "legacy-succeeded", "key-4", "SUCCEEDED", 1, null,
                "{\"done\":true}", "application/json", "{\"nullable\":\"value\"}");
        insertLegacy(5, "legacy-dead", "key-5", "DEAD", 3, null,
                "opaque payload", "application/octet-stream", "{}");

        OutboxMigrationResult first = migrationService().migrate(SOURCE_SCHEMA, 2, false);

        assertThat(first.isVerified()).isTrue();
        assertThat(first.getSourceCount()).isEqualTo(5L);
        assertThat(first.getTargetCount()).isEqualTo(5L);
        assertThat(first.getCopiedCount()).isEqualTo(5L);
        assertThat(first.getMatchedCount()).isZero();
        assertThat(legacyTargetDifferences()).isZero();
        assertThat(physicalJdbcTemplate.queryForObject("""
                SELECT count(*) FROM egon_outbox.egon_cola_outbox_message
                WHERE tenant_id <> 0 OR version <> 0 OR deleted_at IS NOT NULL
                   OR create_user_id <> 'system:outbox:migration'
                   OR update_user_id <> 'system:outbox:migration'
                   OR create_time IS NULL OR update_time IS NULL
                """, Long.class)).isZero();

        OutboxMigrationResult resumed = migrationService().migrate(SOURCE_SCHEMA, 2, false);

        assertThat(resumed.isVerified()).isTrue();
        assertThat(resumed.getCopiedCount()).isZero();
        assertThat(resumed.getMatchedCount()).isEqualTo(5L);
        assertThat(resumed.getTargetCount()).isEqualTo(5L);
    }

    @Test
    void verifyOnlyShouldReportDifferencesWithoutWritingTheTarget() {
        insertLegacy(1, "legacy-pending", "key-1", "PENDING", 0, null,
                "payload", "application/json", "{}");

        OutboxMigrationResult result = migrationService().migrate(SOURCE_SCHEMA, 10, true);

        assertThat(result.isVerified()).isFalse();
        assertThat(result.getSourceCount()).isEqualTo(1L);
        assertThat(result.getTargetCount()).isZero();
        assertThat(result.getDifferenceCount()).isEqualTo(1L);
        assertThat(result.getFirstDifferenceId()).isEqualTo(1L);
        assertThat(targetCount()).isZero();
    }

    @Test
    void shouldRejectAStartedTargetBeforeCopying() {
        insertLegacy(1, "legacy-pending", "key-1", "PENDING", 0, null,
                "payload", "application/json", "{}");
        migrationService().migrate(SOURCE_SCHEMA, 10, false);
        physicalJdbcTemplate.update("""
                UPDATE egon_outbox.egon_cola_outbox_message SET version = 1 WHERE message_id = 'legacy-pending'
                """);

        assertThatThrownBy(() -> migrationService().migrate(SOURCE_SCHEMA, 10, false))
                .isInstanceOf(OutboxConfigurationException.class);
        assertThat(targetCount()).isEqualTo(1L);
    }

    @Test
    void shouldResumeAfterTheFirstBatchCommittedButItsAcknowledgementWasLost() {
        insertLegacy(1, "legacy-pending-1", "key-1", "PENDING", 0, null,
                "payload-1", "application/json", "{}");
        insertLegacy(2, "legacy-pending-2", "key-2", "PENDING", 0, null,
                "payload-2", "application/json", "{}");
        CommitThenFailTransactionTemplate lostAcknowledgement = new CommitThenFailTransactionTemplate(
                transactionManager);
        lostAcknowledgement.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> migrationService(lostAcknowledgement).migrate(SOURCE_SCHEMA, 1, false))
                .isInstanceOf(OutboxStorageException.class);
        assertThat(targetCount()).isEqualTo(1L);

        OutboxMigrationResult resumed = migrationService().migrate(SOURCE_SCHEMA, 1, false);

        assertThat(resumed.isVerified()).isTrue();
        assertThat(resumed.getCopiedCount()).isEqualTo(1L);
        assertThat(resumed.getMatchedCount()).isEqualTo(1L);
        assertThat(resumed.getTargetCount()).isEqualTo(2L);
    }

    @Test
    void shouldRejectConflictingMessageIdentityWithoutReplacingTheTarget() {
        insertLegacy(1, "legacy-same-message", "key-source", "PENDING", 0, null,
                "source-payload", "application/json", "{}");
        insertTarget(100, "legacy-same-message", "key-target", "target-payload");

        assertThatThrownBy(() -> migrationService().migrate(SOURCE_SCHEMA, 10, false))
                .isInstanceOf(OutboxIdempotencyConflictException.class);
        assertThat(targetCount()).isEqualTo(1L);
        assertThat(physicalJdbcTemplate.queryForObject("""
                SELECT payload FROM egon_outbox.egon_cola_outbox_message WHERE id = 100
                """, String.class)).isEqualTo("target-payload");
    }

    @Test
    void shouldRejectAnExtraTargetRowInsteadOfReportingVerified() {
        insertLegacy(1, "legacy-message", "key-1", "PENDING", 0, null,
                "source-payload", "application/json", "{}");
        insertTarget(99, "unrelated-target-message", "key-99", "target-only-payload");

        assertThatThrownBy(() -> migrationService().migrate(SOURCE_SCHEMA, 10, false))
                .isInstanceOf(OutboxIdempotencyConflictException.class);
        assertThat(targetCount()).isEqualTo(2L);
        assertThat(physicalJdbcTemplate.queryForObject("""
                SELECT payload FROM egon_outbox.egon_cola_outbox_message WHERE id = 99
                """, String.class)).isEqualTo("target-only-payload");
    }

    @Test
    void shouldFailWithinTheConfiguredBoundWhenSourceWritersHoldAnExclusiveLock() throws Exception {
        insertLegacy(1, "legacy-message", "key-1", "PENDING", 0, null,
                "payload", "application/json", "{}");
        migrationProperties().setMigrationLockTimeout(Duration.ofMillis(50));
        try (var blocker = physicalJdbcTemplate.getDataSource().getConnection()) {
            blocker.setAutoCommit(false);
            try (var statement = blocker.createStatement()) {
                statement.execute("LOCK TABLE legacy_outbox.egon_cola_outbox_message IN ACCESS EXCLUSIVE MODE");
            }
            assertThatThrownBy(() -> migrationService().migrate(SOURCE_SCHEMA, 10, false))
                    .isInstanceOf(OutboxConfigurationException.class)
                    .hasMessage("OUTBOX_MIGRATION_LOCK_TIMEOUT");
            blocker.rollback();
        }
        assertThat(targetCount()).isZero();
    }

    @Test
    void shouldRefuseCopyWhenMaintenanceModeIsDisabled() {
        insertLegacy(1, "legacy-pending", "key-1", "PENDING", 0, null,
                "payload", "application/json", "{}");
        migrationProperties().setMigrationMode(false);

        assertThatThrownBy(() -> migrationService().migrate(SOURCE_SCHEMA, 10, false))
                .isInstanceOf(OutboxConfigurationException.class);
        assertThat(targetCount()).isZero();
    }

    private OutboxLegacyMigrationService migrationService() {
        return context.getBean(OutboxLegacyMigrationService.class);
    }

    private OutboxLegacyMigrationService migrationService(TransactionTemplate workerTransaction) {
        return new OutboxLegacyMigrationService(
                context.getBean("outboxManagedDdlInitializer", OutboxManagedDdlInitializer.class),
                context.getBean("egonColaShardingRouteFingerprint", String.class),
                context.getBean("outboxMessageRepository", OutboxMessageRepository.class),
                context.getBean("outboxMessageConverterImpl", OutboxMessageConverter.class),
                context.getBean("outboxHeadersConverter", OutboxHeadersConverter.class),
                context.getBean("outboxMessageValidator", OutboxMessageValidator.class),
                context.getBean("outboxTechnicalContextExecutor", OutboxTechnicalContextExecutor.class),
                migrationProperties(),
                workerTransaction,
                context.getBean("egonColaValidationUtils", ValidationUtils.class)
        );
    }

    private OutboxMpStorageProperties migrationProperties() {
        return context.getBean("outboxMpStorageProperties", OutboxMpStorageProperties.class);
    }

    private void insertLegacy(
            long id,
            String messageId,
            String idempotencyKey,
            String status,
            int attemptCount,
            String lockedBy,
            String payload,
            String contentType,
            String headersJson
    ) {
        physicalJdbcTemplate.update("""
                INSERT INTO legacy_outbox.egon_cola_outbox_message (
                    id, message_id, idempotency_key, message_fingerprint, channel, destination, payload,
                    content_type, schema_version, headers_json, trace_id, status, attempt_count, max_attempts,
                    next_attempt_at, locked_by, locked_until, last_error_code, last_error_message,
                    created_at, updated_at, completed_at
                ) VALUES (
                    ?, ?, ?, ?, 'legacy', 'archive', ?, ?, '1', ?, ?, ?, ?, 5,
                    clock_timestamp() - interval '10 minutes', ?,
                    CASE WHEN ? = 'PROCESSING' THEN clock_timestamp() + interval '1 minute' ELSE NULL END,
                    CASE WHEN ? = 'RETRY_WAIT' THEN 'HTTP_503' ELSE NULL END,
                    CASE WHEN ? = 'RETRY_WAIT' THEN 'temporarily unavailable' ELSE NULL END,
                    clock_timestamp() - interval '1 day', clock_timestamp() - interval '1 hour',
                    CASE WHEN ? = 'SUCCEEDED' OR ? = 'DEAD' THEN clock_timestamp() - interval '30 minutes' ELSE NULL END
                )
                """,
                id, messageId, idempotencyKey, "a".repeat(64), payload, contentType, headersJson,
                "trace-" + id, status, attemptCount, lockedBy, status, status, status, status, status);
    }

    private void insertTarget(long id, String messageId, String idempotencyKey, String payload) {
        physicalJdbcTemplate.update("""
                INSERT INTO egon_outbox.egon_cola_outbox_message (
                    id, message_id, idempotency_key, message_fingerprint, channel, destination, payload,
                    content_type, schema_version, headers_json, trace_id, status, attempt_count, max_attempts,
                    next_attempt_at, locked_by, locked_until, last_error_code, last_error_message,
                    created_at, updated_at, completed_at, tenant_id, create_user_id, create_time,
                    update_user_id, update_time, deleted_at, version
                ) VALUES (?, ?, ?, ?, 'legacy', 'archive', ?, 'application/json', '1', '{}', 'trace',
                    'PENDING', 0, 5, clock_timestamp(), NULL, NULL, NULL, NULL,
                    clock_timestamp(), clock_timestamp(), NULL, 0, 'system:outbox:migration',
                    clock_timestamp(), 'system:outbox:migration', clock_timestamp(), NULL, 0)
                """, id, messageId, idempotencyKey, "f".repeat(64), payload);
    }

    private long targetCount() {
        return physicalJdbcTemplate.queryForObject(
                "SELECT count(*) FROM egon_outbox.egon_cola_outbox_message", Long.class);
    }

    private long legacyTargetDifferences() {
        return physicalJdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM legacy_outbox.egon_cola_outbox_message source
                FULL OUTER JOIN egon_outbox.egon_cola_outbox_message target USING (id)
                WHERE source.id IS NULL OR target.id IS NULL
                   OR ROW(source.message_id, source.idempotency_key, source.message_fingerprint, source.channel,
                          source.destination, source.payload, source.content_type, source.schema_version,
                          source.headers_json, source.trace_id, source.status, source.attempt_count,
                          source.max_attempts, source.next_attempt_at, source.locked_by, source.locked_until,
                          source.last_error_code, source.last_error_message, source.created_at,
                          source.updated_at, source.completed_at)
                      IS DISTINCT FROM
                      ROW(target.message_id, target.idempotency_key, target.message_fingerprint, target.channel,
                          target.destination, target.payload, target.content_type, target.schema_version,
                          target.headers_json, target.trace_id, target.status, target.attempt_count,
                          target.max_attempts, target.next_attempt_at, target.locked_by, target.locked_until,
                          target.last_error_code, target.last_error_message, target.created_at,
                          target.updated_at, target.completed_at)
                """, Long.class);
    }

    private static final class CommitThenFailTransactionTemplate extends TransactionTemplate {

        private boolean loseNextCommitAcknowledgement = true;

        private CommitThenFailTransactionTemplate(PlatformTransactionManager transactionManager) {
            super(transactionManager);
        }

        @Override
        public <T> T execute(TransactionCallback<T> action) {
            T result = super.execute(action);
            if (loseNextCommitAcknowledgement) {
                loseNextCommitAcknowledgement = false;
                throw new TransientDataAccessResourceException("Injected loss after batch commit");
            }
            return result;
        }
    }
}
