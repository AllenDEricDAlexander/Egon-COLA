package top.egon.cola.component.outbox.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.exception.OutboxIdempotencyConflictException;
import top.egon.cola.component.outbox.exception.OutboxStorageException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PostgresqlJdbcOutboxStore implements OutboxStore {

    private static final String INSERT_SQL = """
            insert into egon_cola_outbox_message (
                message_id, idempotency_key, message_fingerprint, channel, destination,
                payload, content_type, schema_version, headers_json, trace_id,
                status, attempt_count, max_attempts, next_attempt_at, created_at, updated_at
            ) values (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                'PENDING', 0, ?, coalesce(?, clock_timestamp()), clock_timestamp(), clock_timestamp()
            )
            on conflict do nothing
            returning message_id
            """;

    private static final String CLAIM_DUE_SQL = """
            with candidates as (
                select id
                from egon_cola_outbox_message
                where (
                    status in ('PENDING', 'RETRY_WAIT')
                    and next_attempt_at <= clock_timestamp()
                ) or (
                    status = 'PROCESSING'
                    and locked_until < clock_timestamp()
                )
                order by next_attempt_at, id
                for update skip locked
                limit ?
            )
            update egon_cola_outbox_message message
            set status = 'PROCESSING',
                attempt_count = attempt_count + 1,
                locked_by = ?,
                locked_until = clock_timestamp() + (? * interval '1 millisecond'),
                updated_at = clock_timestamp()
            from candidates
            where message.id = candidates.id
            returning message.*
            """;

    private static final String CLAIM_BY_IDS_SQL = """
            with candidates as (
                select id
                from egon_cola_outbox_message
                where message_id in (:messageIds)
                  and (
                    (
                        status in ('PENDING', 'RETRY_WAIT')
                        and next_attempt_at <= clock_timestamp()
                    ) or (
                        status = 'PROCESSING'
                        and locked_until < clock_timestamp()
                    )
                  )
                order by next_attempt_at, id
                for update skip locked
                limit :limit
            )
            update egon_cola_outbox_message message
            set status = 'PROCESSING',
                attempt_count = attempt_count + 1,
                locked_by = :leaseOwner,
                locked_until = clock_timestamp() + (:leaseMillis * interval '1 millisecond'),
                updated_at = clock_timestamp()
            from candidates
            where message.id = candidates.id
            returning message.*
            """;

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "id", "message_id", "idempotency_key", "message_fingerprint", "channel",
            "destination", "payload", "content_type", "schema_version", "headers_json",
            "trace_id", "status", "attempt_count", "max_attempts", "next_attempt_at",
            "locked_by", "locked_until", "last_error_code", "last_error_message",
            "created_at", "updated_at", "completed_at"
    );

    private static final Set<String> REQUIRED_INDEXES = Set.of(
            "uk_outbox_message_id",
            "uk_outbox_idempotency_key",
            "idx_outbox_claim",
            "idx_outbox_reclaim",
            "idx_outbox_cleanup"
    );

    private static final Set<String> REQUIRED_STATUSES = Set.of(
            "PENDING", "PROCESSING", "RETRY_WAIT", "SUCCEEDED", "DEAD"
    );

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate workerTransaction;
    private final RowMapper<OutboxRecord> rowMapper = this::mapRecord;

    public PostgresqlJdbcOutboxStore(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
        this.workerTransaction = new TransactionTemplate(transactionManager);
        this.workerTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public OutboxReceipt enqueue(NewOutboxRecord record) {
        try {
            List<String> inserted = jdbcTemplate.query(
                    INSERT_SQL,
                    (resultSet, rowNumber) -> resultSet.getString("message_id"),
                    record.messageId(),
                    record.idempotencyKey(),
                    record.messageFingerprint(),
                    record.channel(),
                    record.destination(),
                    record.payload(),
                    record.contentType(),
                    record.schemaVersion(),
                    record.headersJson(),
                    record.traceId(),
                    record.maxAttempts(),
                    timestamp(record.availableAt())
            );
            if (!inserted.isEmpty()) {
                return new OutboxReceipt(record.messageId(), record.idempotencyKey(), true);
            }
            return resolveExisting(record);
        } catch (OutboxIdempotencyConflictException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw storageFailure("Failed to enqueue outbox message", exception);
        }
    }

    @Override
    public List<OutboxRecord> claimDue(int limit, String leaseOwner, Duration leaseDuration) {
        return inWorkerTransaction(() -> jdbcTemplate.query(
                CLAIM_DUE_SQL,
                rowMapper,
                limit,
                leaseOwner,
                leaseDuration.toMillis()
        ), "Failed to claim due outbox messages");
    }

    @Override
    public List<OutboxRecord> claimByMessageIds(
            Collection<String> messageIds,
            int limit,
            String leaseOwner,
            Duration leaseDuration
    ) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("messageIds", messageIds)
                .addValue("limit", limit)
                .addValue("leaseOwner", leaseOwner)
                .addValue("leaseMillis", leaseDuration.toMillis());
        return inWorkerTransaction(
                () -> namedParameterJdbcTemplate.query(CLAIM_BY_IDS_SQL, parameters, rowMapper),
                "Failed to claim selected outbox messages"
        );
    }

    @Override
    public boolean markSucceeded(long id, String leaseOwner) {
        return inWorkerTransaction(() -> jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'SUCCEEDED',
                    locked_by = null,
                    locked_until = null,
                    last_error_code = null,
                    last_error_message = null,
                    completed_at = clock_timestamp(),
                    updated_at = clock_timestamp()
                where id = ?
                  and status = 'PROCESSING'
                  and locked_by = ?
                """, id, leaseOwner) == 1, "Failed to complete outbox message");
    }

    @Override
    public boolean markRetry(
            long id,
            String leaseOwner,
            Duration delay,
            String errorCode,
            String errorMessage
    ) {
        return inWorkerTransaction(() -> jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'RETRY_WAIT',
                    next_attempt_at = clock_timestamp() + (? * interval '1 millisecond'),
                    locked_by = null,
                    locked_until = null,
                    last_error_code = ?,
                    last_error_message = ?,
                    completed_at = null,
                    updated_at = clock_timestamp()
                where id = ?
                  and status = 'PROCESSING'
                  and locked_by = ?
                """,
                delay.toMillis(),
                sanitize(errorCode, 64),
                sanitize(errorMessage, 2000),
                id,
                leaseOwner
        ) == 1, "Failed to retry outbox message");
    }

    @Override
    public boolean markDead(long id, String leaseOwner, String errorCode, String errorMessage) {
        return inWorkerTransaction(() -> jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'DEAD',
                    locked_by = null,
                    locked_until = null,
                    last_error_code = ?,
                    last_error_message = ?,
                    completed_at = clock_timestamp(),
                    updated_at = clock_timestamp()
                where id = ?
                  and status = 'PROCESSING'
                  and locked_by = ?
                """,
                sanitize(errorCode, 64),
                sanitize(errorMessage, 2000),
                id,
                leaseOwner
        ) == 1, "Failed to dead-letter outbox message");
    }

    @Override
    public int deleteSucceeded(Duration retention, int limit) {
        return inWorkerTransaction(() -> jdbcTemplate.update("""
                with candidates as (
                    select id
                    from egon_cola_outbox_message
                    where status = 'SUCCEEDED'
                      and completed_at < clock_timestamp() - (? * interval '1 millisecond')
                    order by completed_at, id
                    for update skip locked
                    limit ?
                )
                delete from egon_cola_outbox_message message
                using candidates
                where message.id = candidates.id
                  and message.status = 'SUCCEEDED'
                """, retention.toMillis(), limit), "Failed to clean up outbox messages");
    }

    @Override
    public long countBacklog() {
        return inWorkerTransaction(() -> {
            Long count = jdbcTemplate.queryForObject("""
                    select count(*)
                    from egon_cola_outbox_message
                    where status in ('PENDING', 'PROCESSING', 'RETRY_WAIT')
                    """, Long.class);
            return count == null ? 0L : count;
        }, "Failed to count outbox backlog");
    }

    @Override
    public void validateSchema() {
        try {
            workerTransaction.executeWithoutResult(status -> validateSchemaInTransaction());
        } catch (OutboxConfigurationException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw new OutboxConfigurationException(
                    "Transactional outbox schema validation failed",
                    exception
            );
        }
    }

    private OutboxReceipt resolveExisting(NewOutboxRecord record) {
        List<ExistingRecord> existing = jdbcTemplate.query("""
                select message_id, idempotency_key, rtrim(message_fingerprint) as message_fingerprint
                from egon_cola_outbox_message
                where message_id = ?
                   or (? is not null and idempotency_key = ?)
                """,
                (resultSet, rowNumber) -> new ExistingRecord(
                        resultSet.getString("message_id"),
                        resultSet.getString("idempotency_key"),
                        resultSet.getString("message_fingerprint")
                ),
                record.messageId(),
                record.idempotencyKey(),
                record.idempotencyKey()
        );
        if (existing.size() != 1
                || !existing.getFirst().messageFingerprint().equals(record.messageFingerprint())) {
            throw new OutboxIdempotencyConflictException(
                    "Outbox message identifier conflicts with an existing message"
            );
        }
        ExistingRecord resolved = existing.getFirst();
        return new OutboxReceipt(resolved.messageId(), resolved.idempotencyKey(), false);
    }

    private void validateSchemaInTransaction() {
        String table = jdbcTemplate.queryForObject(
                "select to_regclass('egon_cola_outbox_message')::text",
                String.class
        );
        if (table == null) {
            throw schemaFailure();
        }

        Set<String> columns = new HashSet<>(jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'egon_cola_outbox_message'
                """, String.class));
        if (!columns.containsAll(REQUIRED_COLUMNS)) {
            throw schemaFailure();
        }

        Set<String> indexes = new HashSet<>(jdbcTemplate.queryForList("""
                select indexname
                from pg_indexes
                where schemaname = current_schema()
                  and tablename = 'egon_cola_outbox_message'
                """, String.class));
        if (!indexes.containsAll(REQUIRED_INDEXES)) {
            throw schemaFailure();
        }

        String statusConstraint = jdbcTemplate.query("""
                select pg_get_constraintdef(constraint_row.oid)
                from pg_constraint constraint_row
                join pg_class table_row on table_row.oid = constraint_row.conrelid
                where table_row.relname = 'egon_cola_outbox_message'
                  and constraint_row.conname = 'ck_outbox_status'
                """, resultSet -> resultSet.next() ? resultSet.getString(1) : null);
        if (statusConstraint == null
                || REQUIRED_STATUSES.stream().anyMatch(status -> !statusConstraint.contains(status))) {
            throw schemaFailure();
        }
    }

    private OutboxRecord mapRecord(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OutboxRecord(
                resultSet.getLong("id"),
                resultSet.getString("message_id"),
                resultSet.getString("idempotency_key"),
                resultSet.getString("message_fingerprint"),
                resultSet.getString("channel"),
                resultSet.getString("destination"),
                resultSet.getString("payload"),
                resultSet.getString("content_type"),
                resultSet.getString("schema_version"),
                readHeaders(resultSet.getString("headers_json")),
                resultSet.getString("trace_id"),
                OutboxStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("attempt_count"),
                resultSet.getInt("max_attempts"),
                instant(resultSet, "next_attempt_at"),
                resultSet.getString("locked_by"),
                instant(resultSet, "locked_until"),
                resultSet.getString("last_error_code"),
                resultSet.getString("last_error_message"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"),
                instant(resultSet, "completed_at")
        );
    }

    private Map<String, String> readHeaders(String json) {
        try {
            Map<String, String> headers = objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, String>>() {
                    }
            );
            return Map.copyOf(headers);
        } catch (JsonProcessingException exception) {
            throw storageFailure("Failed to read outbox message headers", exception);
        }
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private String sanitize(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        StringBuilder sanitized = new StringBuilder(Math.min(value.length(), maximumLength));
        value.codePoints().forEach(codePoint -> {
            if (sanitized.length() >= maximumLength) {
                return;
            }
            sanitized.appendCodePoint(Character.isISOControl(codePoint) ? ' ' : codePoint);
        });
        return sanitized.length() <= maximumLength
                ? sanitized.toString()
                : sanitized.substring(0, maximumLength);
    }

    private <T> T inWorkerTransaction(WorkerOperation<T> operation, String publicMessage) {
        try {
            return workerTransaction.execute(status -> operation.execute());
        } catch (OutboxStorageException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw storageFailure(publicMessage, exception);
        }
    }

    private OutboxStorageException storageFailure(String publicMessage, Exception cause) {
        return new OutboxStorageException(publicMessage, cause);
    }

    private OutboxConfigurationException schemaFailure() {
        return new OutboxConfigurationException(
                "Transactional outbox schema is missing required database objects"
        );
    }

    private record ExistingRecord(
            String messageId,
            String idempotencyKey,
            String messageFingerprint
    ) {
    }

    @FunctionalInterface
    private interface WorkerOperation<T> {

        T execute();
    }
}
