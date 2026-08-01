package top.egon.cola.platform.rbac3.admin.participation.infrastructure;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.participation.application.ParticipationFacade;
import top.egon.cola.platform.rbac3.admin.participation.domain.BusinessParticipationEntity;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;

/**
 * Serializes same-object checks and append through a PostgreSQL transaction lock.
 */
@Repository
public class PostgresqlParticipationStore
        implements ParticipationFacade.ParticipationStore {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;

    public PostgresqlParticipationStore(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    @Override
    @Transactional
    public ParticipationFacade.AppendResult appendAtomically(
            ParticipationFacade.ParticipationRecord record,
            List<ParticipationFacade.PriorActionRule> rules) {
        lock(record);
        BusinessParticipationEntity existing = existing(record);
        if (existing != null) {
            if (!existing.getPayloadDigest().equals(record.payloadDigest())) {
                throw new Rbac3RuleViolation("IDEMPOTENCY_CONFLICT");
            }
            return new ParticipationFacade.AppendResult(
                    false, existing.getId().toString(), List.of());
        }
        List<String> conflicts = objectFacts(record).stream()
                .filter(fact -> rules.stream().anyMatch(rule ->
                        rule.actionCode().equals(fact.getActionCode())
                                && !fact.getOccurredAt().isBefore(rule.lookbackFrom())))
                .map(fact -> fact.getId().toString())
                .toList();
        if (!conflicts.isEmpty()) {
            return new ParticipationFacade.AppendResult(false, null, conflicts);
        }
        Long id = idGenerator.nextLongId();
        entityManager.persist(new BusinessParticipationEntity(
                id, Long.valueOf(record.tenantId()), record.applicationCode(),
                record.businessResource(), record.businessId(),
                Long.valueOf(record.actorUserId()), record.actionCode(),
                record.businessEventId(), record.occurredAt(), record.traceId(),
                record.payloadDigest(), databaseClock.transactionNow(),
                record.applicationCode()));
        entityManager.flush();
        return new ParticipationFacade.AppendResult(true, id.toString(), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationFacade.ParticipationFact> find(
            ParticipationFacade.ConflictQuery query,
            String tenantId,
            Instant lookbackFrom) {
        return entityManager.createQuery("""
                        select p from BusinessParticipationEntity p
                         where p.tenantId = :tenantId
                           and p.applicationCode = :applicationCode
                           and p.businessResource = :businessResource
                           and p.businessId = :businessId
                           and p.actorUserId = :actorUserId
                           and p.occurredAt >= :lookbackFrom
                         order by p.occurredAt, p.id
                        """, BusinessParticipationEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("applicationCode", query.applicationCode())
                .setParameter("businessResource", query.businessResource())
                .setParameter("businessId", query.businessId())
                .setParameter("actorUserId", Long.valueOf(query.actorUserId()))
                .setParameter("lookbackFrom", lookbackFrom)
                .getResultList().stream()
                .map(this::toFact)
                .toList();
    }

    private void lock(ParticipationFacade.ParticipationRecord record) {
        String lockKey = String.join("\u001f",
                record.tenantId(), record.applicationCode(), record.businessResource(),
                record.businessId(), record.actorUserId());
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(?1 as text), 0))")
                .setParameter(1, lockKey)
                .getSingleResult();
    }

    private BusinessParticipationEntity existing(
            ParticipationFacade.ParticipationRecord record) {
        return entityManager.createQuery("""
                        select p from BusinessParticipationEntity p
                         where p.tenantId = :tenantId
                           and p.applicationCode = :applicationCode
                           and p.businessEventId = :eventId
                        """, BusinessParticipationEntity.class)
                .setParameter("tenantId", Long.valueOf(record.tenantId()))
                .setParameter("applicationCode", record.applicationCode())
                .setParameter("eventId", record.businessEventId())
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private List<BusinessParticipationEntity> objectFacts(
            ParticipationFacade.ParticipationRecord record) {
        return entityManager.createQuery("""
                        select p from BusinessParticipationEntity p
                         where p.tenantId = :tenantId
                           and p.applicationCode = :applicationCode
                           and p.businessResource = :businessResource
                           and p.businessId = :businessId
                           and p.actorUserId = :actorUserId
                        """, BusinessParticipationEntity.class)
                .setParameter("tenantId", Long.valueOf(record.tenantId()))
                .setParameter("applicationCode", record.applicationCode())
                .setParameter("businessResource", record.businessResource())
                .setParameter("businessId", record.businessId())
                .setParameter("actorUserId", Long.valueOf(record.actorUserId()))
                .getResultList();
    }

    private ParticipationFacade.ParticipationFact toFact(
            BusinessParticipationEntity entity) {
        return new ParticipationFacade.ParticipationFact(
                entity.getId().toString(), entity.getTenantId().toString(),
                entity.getApplicationCode(), entity.getBusinessResource(),
                entity.getBusinessId(), entity.getActorUserId().toString(),
                entity.getActionCode(), entity.getBusinessEventId(),
                entity.getOccurredAt());
    }
}
