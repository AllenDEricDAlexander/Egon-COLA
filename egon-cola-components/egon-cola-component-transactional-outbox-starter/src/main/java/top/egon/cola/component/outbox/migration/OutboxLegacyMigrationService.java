package top.egon.cola.component.outbox.migration;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;
import top.egon.cola.component.outbox.common.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.common.exception.OutboxIdempotencyConflictException;
import top.egon.cola.component.outbox.common.exception.OutboxStorageException;
import top.egon.cola.component.outbox.common.exception.OutboxValidationException;
import top.egon.cola.component.outbox.persistence.OutboxTechnicalContextExecutor;
import top.egon.cola.component.outbox.persistence.converter.OutboxHeadersConverter;
import top.egon.cola.component.outbox.persistence.converter.OutboxMessageConverter;
import top.egon.cola.component.outbox.persistence.po.OutboxMessagePO;
import top.egon.cola.component.outbox.persistence.repository.OutboxMessageRepository;
import top.egon.cola.component.outbox.serialization.SerializedOutboxPayload;
import top.egon.cola.component.outbox.store.OutboxRecord;
import top.egon.cola.component.outbox.store.OutboxStatus;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Copies legacy rows into the managed MP store while preserving every legacy message field. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class OutboxLegacyMigrationService {

    private static final String TARGET_SCHEMA = "egon_outbox";
    private static final String MESSAGE_TABLE = "egon_cola_outbox_message";
    private static final String MIGRATION_USER_ID = "system:outbox:migration";
    private static final int MAX_BATCH_SIZE = 1000;

    @Qualifier("outboxManagedDdlInitializer")
    private final OutboxManagedDdlInitializer ddlInitializer;

    @Qualifier("egonColaShardingRouteFingerprint")
    private final String routeFingerprint;

    @Qualifier("outboxMessageRepository")
    private final OutboxMessageRepository repository;

    @Qualifier("outboxMessageConverterImpl")
    private final OutboxMessageConverter converter;

    @Qualifier("outboxHeadersConverter")
    private final OutboxHeadersConverter headersConverter;

    @Qualifier("outboxMessageValidator")
    private final OutboxMessageValidator messageValidator;

    @Qualifier("outboxTechnicalContextExecutor")
    private final OutboxTechnicalContextExecutor technicalContext;

    @Qualifier("outboxMpStorageProperties")
    private final OutboxMpStorageProperties storageProperties;

    @Qualifier("outboxWorkerTransactionTemplate")
    private final TransactionTemplate workerTransaction;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    public OutboxMigrationResult migrate(
            @NotBlank @Pattern(regexp = "[a-z_][a-z0-9_]{0,62}") String sourceSchema,
            @Min(1) @Max(MAX_BATCH_SIZE) int batchSize,
            boolean verifyOnly
    ) {
        validateRequest(sourceSchema, batchSize);
        if (!storageProperties.isMigrationMode()) {
            throw new OutboxConfigurationException("OUTBOX_MIGRATION_MODE_REQUIRED");
        }
        validationUtils.validate(storageProperties);

        DataSource physicalPrimary = ddlInitializer.physicalMetadataDataSource(routeFingerprint);
        if (physicalPrimary == null) {
            throw new OutboxConfigurationException("OUTBOX_MIGRATION_PRIMARY_REQUIRED");
        }
        try (Connection sourceConnection = physicalPrimary.getConnection()) {
            sourceConnection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            sourceConnection.setAutoCommit(false);
            try {
                configureSourceLock(sourceConnection);
                lockSourceTable(sourceConnection, sourceSchema);
                validateTargetNotStarted(batchSize);
                long sourceCount = countSourceRows(sourceConnection, sourceSchema);
                CopyCountsBO copied = verifyOnly
                        ? new CopyCountsBO(0, 0)
                        : copySourceRows(sourceConnection, sourceSchema, batchSize);
                VerificationBO verification = verifyAllRows(
                        sourceConnection, sourceSchema, batchSize);
                if (!verifyOnly && verification.differenceCount() > 0) {
                    throw new OutboxIdempotencyConflictException(
                            "Legacy outbox copy did not match the complete source set; first id="
                                    + verification.firstDifferenceId());
                }
                if (sourceCount != verification.sourceCount()) {
                    throw new OutboxStorageException("Legacy outbox source count changed under its share lock");
                }
                OutboxMigrationResult result = OutboxMigrationResult.builder()
                        .sourceSchema(sourceSchema)
                        .targetSchema(TARGET_SCHEMA)
                        .sourceCount(verification.sourceCount())
                        .targetCount(verification.targetCount())
                        .copiedCount(copied.copiedCount())
                        .matchedCount(verifyOnly ? verification.matchedCount() : copied.matchedCount())
                        .differenceCount(verification.differenceCount())
                        .firstDifferenceId(verification.firstDifferenceId())
                        .verified(verification.differenceCount() == 0
                                && verification.sourceCount() == verification.targetCount())
                        .build();
                validationUtils.validate(result);
                sourceConnection.commit();
                log.info("outbox legacy migration finished: sourceSchema={} sourceCount={} targetCount={} "
                                + "copiedCount={} matchedCount={} differenceCount={} verified={}",
                        sourceSchema, result.getSourceCount(), result.getTargetCount(), result.getCopiedCount(),
                        result.getMatchedCount(), result.getDifferenceCount(), result.isVerified());
                return result;
            } catch (SQLException | RuntimeException failure) {
                rollback(sourceConnection, failure);
                throw failure;
            }
        } catch (SQLException failure) {
            throw sourceFailure(failure);
        } catch (DataAccessException failure) {
            throw new OutboxStorageException("Legacy outbox migration failed", failure);
        } catch (TransactionException failure) {
            throw new OutboxStorageException("Legacy outbox target transaction failed", failure);
        } catch (ConstraintViolationException | OutboxValidationException failure) {
            throw new OutboxIdempotencyConflictException(
                    "Legacy outbox source row violates the current storage contract", failure);
        }
    }

    private void validateRequest(String sourceSchema, int batchSize) {
        if (sourceSchema == null || !sourceSchema.matches("[a-z_][a-z0-9_]{0,62}")
                || TARGET_SCHEMA.equals(sourceSchema)) {
            throw new OutboxConfigurationException("OUTBOX_MIGRATION_SOURCE_SCHEMA_INVALID");
        }
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("OUTBOX_MIGRATION_BATCH_SIZE_INVALID");
        }
    }

    private void configureSourceLock(Connection connection) throws SQLException {
        Duration timeout = storageProperties.getMigrationLockTimeout();
        long timeoutMillis;
        try {
            timeoutMillis = Math.max(1, timeout.toMillis());
        } catch (ArithmeticException failure) {
            throw new OutboxConfigurationException("OUTBOX_MIGRATION_LOCK_TIMEOUT_INVALID", failure);
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCAL lock_timeout = '" + timeoutMillis + "ms'");
        }
    }

    private static void lockSourceTable(Connection connection, String sourceSchema) throws SQLException {
        String sourceTable = qualifiedSourceTable(sourceSchema);
        try (Statement statement = connection.createStatement()) {
            statement.execute("LOCK TABLE " + sourceTable + " IN SHARE MODE");
        }
    }

    private void validateTargetNotStarted(int batchSize) {
        long afterId = 0;
        while (true) {
            List<OutboxMessagePO> targetRows = repository.selectMigrationPage(afterId, batchSize);
            if (targetRows.isEmpty()) {
                return;
            }
            for (OutboxMessagePO target : targetRows) {
                if (target.getVersion() == null || target.getVersion() != 0L) {
                    throw new OutboxConfigurationException(
                            "OUTBOX_MIGRATION_TARGET_ALREADY_USED: id=" + target.getId());
                }
            }
            afterId = targetRows.getLast().getId();
        }
    }

    private long countSourceRows(Connection connection, String sourceSchema) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT count(*) FROM " + qualifiedSourceTable(sourceSchema))) {
            if (!result.next()) {
                throw new SQLException("OUTBOX_MIGRATION_SOURCE_COUNT_MISSING");
            }
            return result.getLong(1);
        }
    }

    private CopyCountsBO copySourceRows(Connection sourceConnection, String sourceSchema, int batchSize)
            throws SQLException {
        long cursor = 0;
        long copied = 0;
        long matched = 0;
        while (true) {
            List<LegacyOutboxRowBO> sourceRows = selectSourcePage(sourceConnection, sourceSchema, cursor, batchSize);
            if (sourceRows.isEmpty()) {
                return new CopyCountsBO(copied, matched);
            }
            if (!storageProperties.isMigrationMode()) {
                throw new OutboxConfigurationException("OUTBOX_MIGRATION_MODE_REQUIRED");
            }
            BatchCopyCountsBO batch = workerTransaction.execute(status -> technicalContext.executeMigration(() -> {
                long batchCopied = 0;
                long batchMatched = 0;
                for (LegacyOutboxRowBO source : sourceRows) {
                    Map<String, String> headers = validatedHeaders(source);
                    OutboxMessagePO existing = existingTarget(source);
                    if (existing != null) {
                        requireExactMatch(source, existing);
                        batchMatched++;
                        continue;
                    }
                    OutboxMessagePO target = toMigrationPO(source, headers);
                    int inserted = repository.insertMigrationMessage(target);
                    if (inserted == 1) {
                        batchCopied++;
                        continue;
                    }
                    OutboxMessagePO concurrentTarget = existingTarget(source);
                    if (concurrentTarget == null) {
                        throw conflict(source.id(), "target insert conflicted with a different identity");
                    }
                    requireExactMatch(source, concurrentTarget);
                    batchMatched++;
                }
                return new BatchCopyCountsBO(batchCopied, batchMatched);
            }));
            if (batch == null) {
                throw new OutboxStorageException("Legacy outbox migration batch returned no result");
            }
            copied += batch.copiedCount();
            matched += batch.matchedCount();
            cursor = sourceRows.getLast().id();
        }
    }

    private List<LegacyOutboxRowBO> selectSourcePage(
            Connection connection,
            String sourceSchema,
            long afterId,
            int limit
    ) throws SQLException {
        String sql = "SELECT id, message_id, idempotency_key, message_fingerprint, channel, destination, payload, "
                + "content_type, schema_version, headers_json, trace_id, status, attempt_count, max_attempts, "
                + "next_attempt_at, locked_by, locked_until, last_error_code, last_error_message, created_at, "
                + "updated_at, completed_at FROM " + qualifiedSourceTable(sourceSchema)
                + " WHERE id > ? ORDER BY id LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, afterId);
            statement.setInt(2, limit);
            try (ResultSet result = statement.executeQuery()) {
                List<LegacyOutboxRowBO> rows = new ArrayList<>();
                while (result.next()) {
                    LegacyOutboxRowBO row = readSourceRow(result);
                    try {
                        validationUtils.validate(row);
                    } catch (ConstraintViolationException failure) {
                        throw conflict(row.id(), "legacy row violates the outbox storage contract", failure);
                    }
                    rows.add(row);
                }
                return List.copyOf(rows);
            }
        }
    }

    private LegacyOutboxRowBO readSourceRow(ResultSet result) throws SQLException {
        String statusValue = result.getString("status");
        OutboxStatus status;
        try {
            status = OutboxStatus.valueOf(statusValue);
        } catch (IllegalArgumentException | NullPointerException failure) {
            throw conflict(result.getLong("id"), "unknown source status");
        }
        return new LegacyOutboxRowBO(
                result.getLong("id"),
                result.getString("message_id"),
                result.getString("idempotency_key"),
                result.getString("message_fingerprint"),
                result.getString("channel"),
                result.getString("destination"),
                result.getString("payload"),
                result.getString("content_type"),
                result.getString("schema_version"),
                result.getString("headers_json"),
                result.getString("trace_id"),
                status,
                result.getInt("attempt_count"),
                result.getInt("max_attempts"),
                instant(result, "next_attempt_at"),
                result.getString("locked_by"),
                instant(result, "locked_until"),
                result.getString("last_error_code"),
                result.getString("last_error_message"),
                instant(result, "created_at"),
                instant(result, "updated_at"),
                instant(result, "completed_at"));
    }

    private Map<String, String> validatedHeaders(LegacyOutboxRowBO source) {
        try {
            Map<String, String> headers = headersConverter.parseHeaders(source.headersJson());
            int payloadBytes = source.payload().getBytes(StandardCharsets.UTF_8).length;
            messageValidator.validateSerialized(new SerializedOutboxPayload(source.payload(), payloadBytes), headers);
            return headers;
        } catch (OutboxValidationException failure) {
            throw conflict(source.id(), "legacy headers or payload violate the current storage contract", failure);
        }
    }

    private OutboxMessagePO toMigrationPO(LegacyOutboxRowBO source, Map<String, String> headers) {
        OutboxRecord record = source.toOutboxRecord(headers);
        return converter.toMigrationPO(record, source.payload(), source.headersJson());
    }

    private OutboxMessagePO existingTarget(LegacyOutboxRowBO source) {
        Map<Long, OutboxMessagePO> candidates = new LinkedHashMap<>();
        List<OutboxMessagePO> byId = repository.selectMigrationPage(source.id() - 1, 1);
        if (!byId.isEmpty() && byId.getFirst().getId() == source.id()) {
            candidates.put(byId.getFirst().getId(), byId.getFirst());
        }
        List<OutboxMessagePO> byIdentity = repository.selectExistingByIdentity(
                source.messageId(), source.idempotencyKey());
        for (OutboxMessagePO target : byIdentity) {
            candidates.put(target.getId(), target);
        }
        if (candidates.size() > 1) {
            throw conflict(source.id(), "source identities match multiple target rows");
        }
        return candidates.values().stream().findFirst().orElse(null);
    }

    private void requireExactMatch(LegacyOutboxRowBO source, OutboxMessagePO target) {
        if (!sameLegacyFields(source, target)
                || !Long.valueOf(0).equals(target.getTenantId())
                || target.getDeletedAt() != null
                || !Long.valueOf(0).equals(target.getVersion())
                || !MIGRATION_USER_ID.equals(target.getCreateUserId())
                || !MIGRATION_USER_ID.equals(target.getUpdateUserId())
                || target.getCreateTime() == null || target.getUpdateTime() == null) {
            throw conflict(source.id(), "existing target row differs from its source identity");
        }
    }

    private static boolean sameLegacyFields(LegacyOutboxRowBO source, OutboxMessagePO target) {
        return target != null
                && Objects.equals(target.getId(), source.id())
                && Objects.equals(target.getMessageId(), source.messageId())
                && Objects.equals(target.getIdempotencyKey(), source.idempotencyKey())
                && Objects.equals(target.getMessageFingerprint(), source.messageFingerprint())
                && Objects.equals(target.getChannel(), source.channel())
                && Objects.equals(target.getDestination(), source.destination())
                && Objects.equals(target.getPayload(), source.payload())
                && Objects.equals(target.getContentType(), source.contentType())
                && Objects.equals(target.getSchemaVersion(), source.schemaVersion())
                && Objects.equals(target.getHeadersJson(), source.headersJson())
                && Objects.equals(target.getTraceId(), source.traceId())
                && target.getStatus() == source.status()
                && Objects.equals(target.getAttemptCount(), source.attemptCount())
                && Objects.equals(target.getMaxAttempts(), source.maxAttempts())
                && Objects.equals(target.getNextAttemptAt(), source.nextAttemptAt())
                && Objects.equals(target.getLockedBy(), source.lockedBy())
                && Objects.equals(target.getLockedUntil(), source.lockedUntil())
                && Objects.equals(target.getLastErrorCode(), source.lastErrorCode())
                && Objects.equals(target.getLastErrorMessage(), source.lastErrorMessage())
                && Objects.equals(target.getCreatedAt(), source.createdAt())
                && Objects.equals(target.getUpdatedAt(), source.updatedAt())
                && Objects.equals(target.getCompletedAt(), source.completedAt());
    }

    private VerificationBO verifyAllRows(Connection sourceConnection, String sourceSchema, int batchSize)
            throws SQLException {
        long sourceCursor = 0;
        long targetCursor = 0;
        List<LegacyOutboxRowBO> sourcePage = List.of();
        List<OutboxMessagePO> targetPage = List.of();
        int sourceIndex = 0;
        int targetIndex = 0;
        boolean sourceDone = false;
        boolean targetDone = false;
        long sourceCount = 0;
        long targetCount = 0;
        long matchedCount = 0;
        long differenceCount = 0;
        Long firstDifferenceId = null;

        while (!sourceDone || !targetDone) {
            if (!sourceDone && sourceIndex >= sourcePage.size()) {
                sourcePage = selectSourcePage(sourceConnection, sourceSchema, sourceCursor, batchSize);
                sourceIndex = 0;
                if (sourcePage.isEmpty()) {
                    sourceDone = true;
                } else {
                    sourceCursor = sourcePage.getLast().id();
                }
            }
            if (!targetDone && targetIndex >= targetPage.size()) {
                targetPage = repository.selectMigrationPage(targetCursor, batchSize);
                targetIndex = 0;
                if (targetPage.isEmpty()) {
                    targetDone = true;
                } else {
                    targetCursor = targetPage.getLast().getId();
                }
            }
            if (sourceDone && targetDone) {
                break;
            }
            LegacyOutboxRowBO source = sourceDone ? null : sourcePage.get(sourceIndex);
            OutboxMessagePO target = targetDone ? null : targetPage.get(targetIndex);
            if (source == null) {
                targetCount++;
                differenceCount++;
                firstDifferenceId = firstDifferenceId == null ? target.getId() : firstDifferenceId;
                targetIndex++;
            } else if (target == null) {
                sourceCount++;
                differenceCount++;
                firstDifferenceId = firstDifferenceId == null ? source.id() : firstDifferenceId;
                sourceIndex++;
            } else if (source.id() == target.getId()) {
                sourceCount++;
                targetCount++;
                if (sameLegacyFields(source, target)
                        && Long.valueOf(0).equals(target.getTenantId())
                        && target.getDeletedAt() == null
                        && Long.valueOf(0).equals(target.getVersion())
                        && MIGRATION_USER_ID.equals(target.getCreateUserId())
                        && MIGRATION_USER_ID.equals(target.getUpdateUserId())
                        && target.getCreateTime() != null && target.getUpdateTime() != null) {
                    matchedCount++;
                } else {
                    differenceCount++;
                    firstDifferenceId = firstDifferenceId == null ? source.id() : firstDifferenceId;
                }
                sourceIndex++;
                targetIndex++;
            } else if (source.id() < target.getId()) {
                sourceCount++;
                differenceCount++;
                firstDifferenceId = firstDifferenceId == null ? source.id() : firstDifferenceId;
                sourceIndex++;
            } else {
                targetCount++;
                differenceCount++;
                firstDifferenceId = firstDifferenceId == null ? target.getId() : firstDifferenceId;
                targetIndex++;
            }
        }
        return new VerificationBO(sourceCount, targetCount, matchedCount, differenceCount, firstDifferenceId);
    }

    private static String qualifiedSourceTable(String sourceSchema) {
        return "\"" + sourceSchema + "\".\"" + MESSAGE_TABLE + "\"";
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        OffsetDateTime value = result.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    private static RuntimeException sourceFailure(SQLException failure) {
        if ("55P03".equals(failure.getSQLState())) {
            return new OutboxConfigurationException("OUTBOX_MIGRATION_LOCK_TIMEOUT", failure);
        }
        return new OutboxStorageException("Legacy outbox source read failed", failure);
    }

    private static OutboxIdempotencyConflictException conflict(long id, String reason) {
        return new OutboxIdempotencyConflictException("Legacy outbox row conflicts at id " + id + ": " + reason);
    }

    private static OutboxIdempotencyConflictException conflict(long id, String reason, Throwable cause) {
        return new OutboxIdempotencyConflictException(
                "Legacy outbox row is invalid at id " + id + ": " + reason, cause);
    }

    private record CopyCountsBO(long copiedCount, long matchedCount) {
    }

    private record BatchCopyCountsBO(long copiedCount, long matchedCount) {
    }

    private record VerificationBO(
            long sourceCount,
            long targetCount,
            long matchedCount,
            long differenceCount,
            Long firstDifferenceId
    ) {
    }

    private record LegacyOutboxRowBO(
            @Positive long id,
            @NotBlank @Size(max = 64) String messageId,
            @Size(max = 256) String idempotencyKey,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String messageFingerprint,
            @NotBlank @Size(max = 64) String channel,
            @NotBlank @Size(max = 256) String destination,
            @NotNull String payload,
            @NotBlank @Size(max = 128) String contentType,
            @Size(max = 32) String schemaVersion,
            @NotBlank String headersJson,
            @Size(max = 128) String traceId,
            @NotNull OutboxStatus status,
            @PositiveOrZero int attemptCount,
            @Min(1) int maxAttempts,
            @NotNull Instant nextAttemptAt,
            @Size(max = 128) String lockedBy,
            Instant lockedUntil,
            @Size(max = 64) String lastErrorCode,
            String lastErrorMessage,
            @NotNull Instant createdAt,
            @NotNull Instant updatedAt,
            Instant completedAt
    ) {

        private OutboxRecord toOutboxRecord(Map<String, String> headers) {
            return new OutboxRecord(id, messageId, idempotencyKey, messageFingerprint, channel, destination,
                    payload, contentType, schemaVersion, headers, traceId, status, attemptCount, maxAttempts,
                    nextAttemptAt, lockedBy, lockedUntil, lastErrorCode, lastErrorMessage, createdAt,
                    updatedAt, completedAt);
        }
    }
}
