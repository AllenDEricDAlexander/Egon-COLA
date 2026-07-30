package top.egon.cola.platform.rbac3.admin.runtime.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.runtime.application.IdempotencyService;
import top.egon.cola.platform.rbac3.admin.runtime.domain.IdempotencyRecordEntity;

import java.time.Instant;
import java.util.List;

@Repository
public class IdempotencyRepository implements IdempotencyService.IdempotencyStore {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;

    public IdempotencyRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator
    ) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public IdempotencyService.Claim claim(IdempotencyService.StoredCommand command) {
        List<IdempotencyRecordEntity> records = entityManager.createQuery("""
                        select r from IdempotencyRecordEntity r
                         where r.tenantId = :tenantId and r.actorType = :actorType
                           and r.actorId = :actorId
                           and r.operationCode = :operationCode
                           and r.keyHash = :keyHash
                        """, IdempotencyRecordEntity.class)
                .setParameter("tenantId", Long.valueOf(command.tenantId()))
                .setParameter("actorType",
                        IdempotencyRecordEntity.ActorType.valueOf(command.actorType()))
                .setParameter("actorId", command.actorId())
                .setParameter("operationCode", command.operationCode())
                .setParameter("keyHash", command.keyHash())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (!records.isEmpty()) {
            IdempotencyRecordEntity record = records.getFirst();
            if (!record.getRequestHash().equals(command.requestHash())) {
                return claim(record, IdempotencyService.Outcome.CONFLICT);
            }
            return claim(record,
                    record.getStatus() == IdempotencyRecordEntity.Status.COMPLETED
                            ? IdempotencyService.Outcome.REPLAY
                            : IdempotencyService.Outcome.IN_PROGRESS);
        }
        Long id = idGenerator.nextLongId();
        entityManager.persist(new IdempotencyRecordEntity(
                id, Long.valueOf(command.tenantId()),
                IdempotencyRecordEntity.ActorType.valueOf(command.actorType()),
                command.actorId(), command.operationCode(), command.keyHash(),
                command.requestHash(), command.expiresAt(), command.now()));
        return new IdempotencyService.Claim(
                id.toString(), IdempotencyService.Outcome.CLAIMED,
                null, null, null);
    }

    @Override
    @Transactional
    public void complete(
            String recordId,
            String resourceType,
            String resourceId,
            int responseStatus,
            String responseDigest,
            Instant now
    ) {
        IdempotencyRecordEntity record = entityManager.find(
                IdempotencyRecordEntity.class, Long.valueOf(recordId),
                LockModeType.PESSIMISTIC_WRITE);
        if (record == null) {
            throw new IllegalStateException("idempotency record is missing");
        }
        record.complete(
                resourceType, resourceId, responseStatus, responseDigest, now);
    }

    private IdempotencyService.Claim claim(
            IdempotencyRecordEntity record,
            IdempotencyService.Outcome outcome
    ) {
        return new IdempotencyService.Claim(
                Long.toString((Long) entityManager.getEntityManagerFactory()
                        .getPersistenceUnitUtil().getIdentifier(record)),
                outcome, record.getResourceId(), record.getResponseStatus(),
                record.getResponseDigest());
    }
}
