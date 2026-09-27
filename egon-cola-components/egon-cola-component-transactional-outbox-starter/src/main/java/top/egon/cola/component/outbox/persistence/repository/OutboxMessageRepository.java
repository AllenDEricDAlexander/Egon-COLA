package top.egon.cola.component.outbox.persistence.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.outbox.persistence.dao.OutboxMessageDAO;
import top.egon.cola.component.outbox.persistence.po.OutboxMessagePO;
import top.egon.cola.component.outbox.store.OutboxStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** Named repository operations for the technical outbox message table. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class OutboxMessageRepository extends EgonColaRepository<OutboxMessageDAO, OutboxMessagePO> {

    @Getter
    @Qualifier("outboxMessageDAO")
    private final OutboxMessageDAO baseMapper;

    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public int insertMessage(@NotNull @Valid OutboxMessagePO entity) {
        return requireAtMostOne("insertMessage", getBaseMapper().insertMessage(entity));
    }

    public int insertMigrationMessage(@NotNull @Valid OutboxMessagePO entity) {
        return requireAtMostOne("insertMigrationMessage", getBaseMapper().insertMigrationMessage(entity));
    }

    public List<OutboxMessagePO> selectExistingByIdentity(
            @NotBlank @Size(max = 64) String messageId,
            @Size(max = 256) String idempotencyKey
    ) {
        return getBaseMapper().selectExistingByIdentity(messageId, idempotencyKey);
    }

    public List<OutboxMessagePO> selectDueCandidates(@Min(1) @Max(10_000) int limit) {
        requireBatchLimit(limit);
        return getBaseMapper().selectDueCandidates(limit);
    }

    public List<OutboxMessagePO> selectByMessageIdsCandidates(
            @NotNull @Size(max = 10_000) Collection<@NotBlank @Size(max = 64) String> messageIds,
            @Min(1) @Max(10_000) int limit
    ) {
        if (messageIds == null) {
            throw new IllegalArgumentException("OUTBOX_MESSAGE_IDS_REQUIRED");
        }
        requireCollectionBound(messageIds.size());
        if (messageIds.stream().anyMatch(messageId -> messageId == null || messageId.isBlank()
                || messageId.length() > 64)) {
            throw new IllegalArgumentException("OUTBOX_MESSAGE_IDS_INVALID");
        }
        if (messageIds.isEmpty()) {
            return List.of();
        }
        requireBatchLimit(limit);
        return getBaseMapper().selectByMessageIdsCandidates(messageIds, limit);
    }

    public boolean tryAcquireTransactionLock(@Positive long id) {
        return Boolean.TRUE.equals(getBaseMapper().tryAcquireTransactionLock(id));
    }

    public int updateClaim(
            @Positive long id,
            @Min(0) long version,
            @NotNull OutboxStatus source,
            @NotNull OutboxStatus target,
            @NotBlank @Size(max = 128) String owner,
            @Positive long leaseMillis,
            @NotNull Instant updateTime,
            @NotBlank @Size(max = 128) String updateUserId
    ) {
        return requireAtMostOne("updateClaim", getBaseMapper().updateClaim(
                id, version, source, target, owner, leaseMillis, updateTime, updateUserId));
    }

    public OutboxMessagePO selectProcessingForUpdate(@Positive long id, @NotBlank @Size(max = 128) String owner) {
        return getBaseMapper().selectProcessingForUpdate(id, owner);
    }

    public int updateSucceeded(
            @Positive long id,
            @Min(0) long version,
            @NotBlank @Size(max = 128) String owner,
            @NotNull OutboxStatus target,
            @NotNull Instant updateTime,
            @NotBlank @Size(max = 128) String updateUserId
    ) {
        return requireAtMostOne("updateSucceeded", getBaseMapper().updateSucceeded(
                id, version, owner, target, updateTime, updateUserId));
    }

    public int updateRetry(
            @Positive long id,
            @Min(0) long version,
            @NotBlank @Size(max = 128) String owner,
            @NotNull OutboxStatus target,
            @Min(0) long delayMillis,
            @Size(max = 64) String errorCode,
            String errorMessage,
            @NotNull Instant updateTime,
            @NotBlank @Size(max = 128) String updateUserId
    ) {
        return requireAtMostOne("updateRetry", getBaseMapper().updateRetry(
                id, version, owner, target, delayMillis, errorCode, errorMessage, updateTime, updateUserId));
    }

    public int updateDead(
            @Positive long id,
            @Min(0) long version,
            @NotBlank @Size(max = 128) String owner,
            @NotNull OutboxStatus target,
            @Size(max = 64) String errorCode,
            String errorMessage,
            @NotNull Instant updateTime,
            @NotBlank @Size(max = 128) String updateUserId
    ) {
        return requireAtMostOne("updateDead", getBaseMapper().updateDead(
                id, version, owner, target, errorCode, errorMessage, updateTime, updateUserId));
    }

    public List<OutboxMessagePO> selectExpiredSucceededCandidates(
            @Min(0) long retentionMillis,
            @Min(1) @Max(10_000) int limit
    ) {
        requireBatchLimit(limit);
        return getBaseMapper().selectExpiredSucceededCandidates(retentionMillis, limit);
    }

    public int deleteSucceededByIdVersion(
            @Positive long id,
            @Min(0) long version,
            @Min(0) long retentionMillis
    ) {
        return requireAtMostOne("deleteSucceededByIdVersion", getBaseMapper().deleteSucceededByIdVersion(
                id, version, retentionMillis));
    }

    public long countBacklog() {
        return getBaseMapper().countBacklog();
    }

    public List<OutboxMessagePO> selectMigrationPage(@Min(0) long afterId, @Min(1) @Max(10_000) int limit) {
        requireBatchLimit(limit);
        return getBaseMapper().selectMigrationPage(afterId, limit);
    }

    private void requireBatchLimit(int limit) {
        if (limit < 1 || limit > properties.getBatch().getMaxCollectionSize()) {
            throw new IllegalArgumentException("OUTBOX_BATCH_LIMIT_INVALID");
        }
    }

    private void requireCollectionBound(int size) {
        if (size > properties.getBatch().getMaxCollectionSize()) {
            throw new IllegalArgumentException("OUTBOX_BATCH_COLLECTION_TOO_LARGE");
        }
    }

    private int requireAtMostOne(String operation, int affectedRows) {
        if (affectedRows > 1) {
            log.error("Outbox persistence operation affected more than one row: operation={} affectedRows={}",
                    operation, affectedRows);
            throw new IllegalStateException("OUTBOX_SINGLE_ROW_OPERATION_AFFECTED_MULTIPLE_ROWS: " + operation);
        }
        return affectedRows;
    }
}
