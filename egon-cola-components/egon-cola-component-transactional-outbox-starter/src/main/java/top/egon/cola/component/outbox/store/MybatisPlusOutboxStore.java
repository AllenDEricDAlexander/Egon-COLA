package top.egon.cola.component.outbox.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.common.exception.OutboxIdempotencyConflictException;
import top.egon.cola.component.outbox.common.exception.OutboxStorageException;
import top.egon.cola.component.outbox.common.exception.OutboxValidationException;
import top.egon.cola.component.outbox.persistence.OutboxSchemaMetadataValidator;
import top.egon.cola.component.outbox.persistence.OutboxTechnicalContextExecutor;
import top.egon.cola.component.outbox.persistence.converter.OutboxMessageConverter;
import top.egon.cola.component.outbox.persistence.po.OutboxMessagePO;
import top.egon.cola.component.outbox.persistence.repository.OutboxMessageRepository;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleService;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleStateMachineFactory;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleSignalEnum;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** MyBatis-Plus implementation of the existing public outbox storage contract. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class MybatisPlusOutboxStore implements OutboxStore {

    private static final String TECHNICAL_USER_ID = "system:outbox";

    @Qualifier("outboxMessageRepository")
    private final OutboxMessageRepository repository;

    @Qualifier("outboxMessageConverterImpl")
    private final OutboxMessageConverter converter;

    @Qualifier("outboxTechnicalContextExecutor")
    private final OutboxTechnicalContextExecutor technicalContext;

    @Qualifier("outboxLifecycleService")
    private final OutboxLifecycleService lifecycle;

    @Qualifier("outboxWorkerTransactionTemplate")
    private final TransactionTemplate workerTransaction;

    @Qualifier("outboxSchemaMetadataValidator")
    private final OutboxSchemaMetadataValidator metadataValidator;

    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    public OutboxReceipt enqueue(@NotNull @Valid NewOutboxRecord record) {
        Objects.requireNonNull(record, "record");
        try {
            return technicalContext.execute(() -> {
                OutboxMessagePO message = converter.toInsertPO(record);
                message.setId(SnowflakeIdGenerator.nextLongId());
                message.setStatus(lifecycle.evaluate(
                        OutboxLifecycleStateMachineFactory.NEW_STATE,
                        OutboxLifecycleSignalEnum.ENQUEUE, 0, record.maxAttempts()));
                message.setAttemptCount(0);
                if (repository.insertMessage(message) == 1) {
                    return new OutboxReceipt(record.messageId(), record.idempotencyKey(), true);
                }
                return resolveExisting(record);
            });
        } catch (OutboxIdempotencyConflictException | OutboxStorageException failure) {
            throw failure;
        } catch (DataAccessException failure) {
            throw storageFailure("Failed to enqueue outbox message", failure);
        }
    }

    @Override
    public List<OutboxRecord> claimDue(@Positive int limit, @NotNull String leaseOwner,
                                      @NotNull Duration leaseDuration) {
        requireLease(leaseOwner, leaseDuration);
        return inWorkerTransaction("Failed to claim due outbox messages", () -> {
            List<OutboxMessagePO> candidates = repository.selectDueForUpdate(limit);
            return claim(candidates, leaseOwner, leaseDuration);
        });
    }

    @Override
    public List<OutboxRecord> claimByMessageIds(
            Collection<String> messageIds,
            @Positive int limit,
            @NotNull String leaseOwner,
            @NotNull Duration leaseDuration
    ) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        requireLease(leaseOwner, leaseDuration);
        return inWorkerTransaction("Failed to claim selected outbox messages", () -> {
            List<OutboxMessagePO> candidates = repository.selectByMessageIdsForUpdate(messageIds, limit);
            return claim(candidates, leaseOwner, leaseDuration);
        });
    }

    @Override
    public boolean markSucceeded(@Positive long id, @NotNull String leaseOwner) {
        return inWorkerTransaction("Failed to complete outbox message", () -> {
            OutboxMessagePO current = repository.selectProcessingForUpdate(id, leaseOwner);
            if (current == null) {
                return false;
            }
            OutboxStatus target = lifecycle.evaluate(
                    current.getStatus().getMessage(), OutboxLifecycleSignalEnum.DELIVERY_SUCCEEDED,
                    current.getAttemptCount(), current.getMaxAttempts());
            return repository.updateSucceeded(id, current.getVersion(), leaseOwner, target,
                    clock.instant(), TECHNICAL_USER_ID) == 1;
        });
    }

    @Override
    public boolean markRetry(@Positive long id, @NotNull String leaseOwner, @NotNull Duration delay,
                             String errorCode, String errorMessage) {
        if (delay == null || delay.isNegative()) {
            throw new IllegalArgumentException("OUTBOX_RETRY_DELAY_INVALID");
        }
        return inWorkerTransaction("Failed to retry outbox message", () -> {
            OutboxMessagePO current = repository.selectProcessingForUpdate(id, leaseOwner);
            if (current == null) {
                return false;
            }
            OutboxStatus target = lifecycle.evaluate(
                    current.getStatus().getMessage(), OutboxLifecycleSignalEnum.DELIVERY_RETRYABLE,
                    current.getAttemptCount(), current.getMaxAttempts());
            if (target == OutboxStatus.DEAD) {
                return repository.updateDead(id, current.getVersion(), leaseOwner, target,
                        sanitize(errorCode, 64), sanitize(errorMessage, 2000), clock.instant(),
                        TECHNICAL_USER_ID) == 1;
            }
            return repository.updateRetry(id, current.getVersion(), leaseOwner, target, delay.toMillis(),
                    sanitize(errorCode, 64), sanitize(errorMessage, 2000), clock.instant(),
                    TECHNICAL_USER_ID) == 1;
        });
    }

    @Override
    public boolean markDead(@Positive long id, @NotNull String leaseOwner, String errorCode, String errorMessage) {
        return inWorkerTransaction("Failed to dead-letter outbox message", () -> {
            OutboxMessagePO current = repository.selectProcessingForUpdate(id, leaseOwner);
            if (current == null) {
                return false;
            }
            OutboxStatus target = lifecycle.evaluate(
                    current.getStatus().getMessage(), OutboxLifecycleSignalEnum.DELIVERY_PERMANENT,
                    current.getAttemptCount(), current.getMaxAttempts());
            return repository.updateDead(id, current.getVersion(), leaseOwner, target,
                    sanitize(errorCode, 64), sanitize(errorMessage, 2000), clock.instant(),
                    TECHNICAL_USER_ID) == 1;
        });
    }

    @Override
    public int deleteSucceeded(@NotNull Duration retention, @Positive int limit) {
        if (retention == null || retention.isNegative()) {
            throw new IllegalArgumentException("OUTBOX_RETENTION_INVALID");
        }
        return inWorkerTransaction("Failed to clean up outbox messages", () -> {
            List<Long> expiredIds = repository.selectExpiredSucceededForUpdate(retention.toMillis(), limit)
                    .stream().map(OutboxMessagePO::getId).toList();
            return repository.deleteSucceededByIds(expiredIds);
        });
    }

    @Override
    public long countBacklog() {
        return inWorkerTransaction("Failed to count outbox backlog", repository::countBacklog);
    }

    @Override
    public void validateSchema() {
        metadataValidator.validate();
    }

    private List<OutboxRecord> claim(List<OutboxMessagePO> candidates, String leaseOwner, Duration leaseDuration) {
        java.util.ArrayList<OutboxRecord> claimed = new java.util.ArrayList<>(candidates.size());
        for (OutboxMessagePO candidate : candidates) {
            OutboxLifecycleSignalEnum signal = candidate.getStatus() == OutboxStatus.PROCESSING
                    ? OutboxLifecycleSignalEnum.RECLAIM : OutboxLifecycleSignalEnum.CLAIM;
            OutboxStatus target = lifecycle.evaluate(candidate.getStatus().getMessage(), signal,
                    candidate.getAttemptCount(), candidate.getMaxAttempts());
            int changed = repository.updateClaim(candidate.getId(), candidate.getVersion(),
                    candidate.getStatus(), target, leaseOwner, leaseDuration.toMillis(), clock.instant(),
                    TECHNICAL_USER_ID);
            if (changed == 0) {
                continue;
            }
            OutboxMessagePO updated = repository.selectProcessingForUpdate(candidate.getId(), leaseOwner);
            if (updated == null) {
                throw new OutboxStorageException("Claimed outbox message could not be reloaded");
            }
            claimed.add(toRecord(updated));
        }
        return List.copyOf(claimed);
    }

    private OutboxReceipt resolveExisting(NewOutboxRecord record) {
        List<OutboxMessagePO> existing = repository.selectExistingByIdentity(
                record.messageId(), record.idempotencyKey());
        if (existing.size() != 1
                || !existing.getFirst().getMessageFingerprint().equals(record.messageFingerprint())) {
            throw new OutboxIdempotencyConflictException(
                    "Outbox message identifier conflicts with an existing message");
        }
        OutboxMessagePO resolved = existing.getFirst();
        return new OutboxReceipt(resolved.getMessageId(), resolved.getIdempotencyKey(), false);
    }

    private <T> T inWorkerTransaction(String publicMessage, Supplier<T> operation) {
        try {
            return workerTransaction.execute(status -> technicalContext.execute(operation));
        } catch (OutboxStorageException failure) {
            throw failure;
        } catch (DataAccessException failure) {
            throw storageFailure(publicMessage, failure);
        }
    }

    private OutboxRecord toRecord(OutboxMessagePO message) {
        try {
            return converter.toTarget(message);
        } catch (OutboxValidationException failure) {
            throw storageFailure("Failed to read outbox message headers", failure);
        }
    }

    private static void requireLease(String leaseOwner, Duration leaseDuration) {
        if (leaseOwner == null || leaseOwner.isBlank() || leaseDuration == null
                || leaseDuration.isNegative() || leaseDuration.isZero()) {
            throw new IllegalArgumentException("OUTBOX_LEASE_INVALID");
        }
    }

    private static String sanitize(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        StringBuilder sanitized = new StringBuilder(Math.min(value.length(), maximumLength));
        value.codePoints().forEach(codePoint -> {
            if (Character.charCount(codePoint) <= maximumLength - sanitized.length()) {
                sanitized.appendCodePoint(Character.isISOControl(codePoint) ? ' ' : codePoint);
            }
        });
        return sanitized.toString();
    }

    private static OutboxStorageException storageFailure(String publicMessage, Exception cause) {
        return new OutboxStorageException(publicMessage, cause);
    }
}
