package top.egon.cola.platform.rbac3.admin.directory.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.directory.domain.DirectorySnapshotEntity;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.List;
import java.util.Objects;

/**
 * Enforces monotonic, idempotent directory snapshot ingestion per provider.
 */
@Repository
public class DirectorySnapshotStore {

    private final EntityManager entityManager;

    public DirectorySnapshotStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public IngestionResult accept(DirectorySnapshotEntity incoming) {
        Objects.requireNonNull(incoming, "incoming");
        List<DirectorySnapshotEntity> existing = entityManager.createQuery("""
                        select s from DirectorySnapshotEntity s
                         where s.tenantId = :tenantId
                           and s.providerCode = :providerCode
                           and s.snapshotVersion = :snapshotVersion
                        """, DirectorySnapshotEntity.class)
                .setParameter("tenantId", incoming.getTenantId())
                .setParameter("providerCode", incoming.getProviderCode())
                .setParameter("snapshotVersion", incoming.getSnapshotVersion())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (!existing.isEmpty()) {
            DirectorySnapshotEntity current = existing.getFirst();
            if (current.getChecksum().equals(incoming.getChecksum())) {
                return new IngestionResult(Outcome.IDEMPOTENT, current.getId());
            }
            throw new Rbac3RuleViolation("DIRECTORY_SNAPSHOT_CONFLICT");
        }
        Long maximum = entityManager.createQuery("""
                        select max(s.snapshotVersion) from DirectorySnapshotEntity s
                         where s.tenantId = :tenantId and s.providerCode = :providerCode
                        """, Long.class)
                .setParameter("tenantId", incoming.getTenantId())
                .setParameter("providerCode", incoming.getProviderCode())
                .getSingleResult();
        if (maximum != null && incoming.getSnapshotVersion() < maximum) {
            throw new Rbac3RuleViolation("DIRECTORY_SNAPSHOT_STALE");
        }
        entityManager.persist(incoming);
        return new IngestionResult(Outcome.ACCEPTED, incoming.getId());
    }

    public record IngestionResult(Outcome outcome, Long snapshotId) {
    }

    public enum Outcome {
        ACCEPTED,
        IDEMPOTENT
    }
}
