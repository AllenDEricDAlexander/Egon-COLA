package top.egon.cola.platform.rbac3.admin.session.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.session.application.RefreshTokenService;
import top.egon.cola.platform.rbac3.admin.session.domain.RefreshTokenEntity;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * PostgreSQL implementation of the refresh-token atomic lock boundary.
 */
@Repository
public class RefreshTokenRepository implements RefreshTokenService.RefreshTokenStore {

    private final EntityManager entityManager;
    private final SessionRepository sessionRepository;
    private final LongIdGenerator idGenerator;

    public RefreshTokenRepository(
            EntityManager entityManager,
            SessionRepository sessionRepository,
            LongIdGenerator idGenerator) {
        this.entityManager = entityManager;
        this.sessionRepository = sessionRepository;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public <T> T withLockedToken(
            String tokenHash,
            Function<RefreshTokenService.TokenRecord, T> action) {
        RefreshTokenEntity entity = findByHash(tokenHash, LockModeType.PESSIMISTIC_WRITE);
        return action.apply(entity == null ? null : toRecord(entity));
    }

    @Override
    public void rotate(
            RefreshTokenService.TokenRecord oldToken,
            RefreshTokenService.TokenRecord newToken) {
        RefreshTokenEntity current = findByHash(
                oldToken.tokenHash(), LockModeType.PESSIMISTIC_WRITE);
        if (current == null) {
            throw new IllegalStateException("locked refresh token disappeared");
        }
        var session = sessionRepository.lockByTenantIdAndSessionId(
                        current.getTenantId(), current.getSessionId())
                .orElseThrow(() -> new IllegalStateException("refresh session is missing"));
        session.refresh(
                session.getPolicyVersionAtIssue(),
                oldToken.rotatedAt(),
                oldToken.rotatedAt().plusSeconds(30 * 60L),
                "refresh");
        long nextId = idGenerator.nextLongId();
        RefreshTokenEntity replacement = new RefreshTokenEntity(
                nextId,
                Long.valueOf(newToken.tenantId()),
                Long.valueOf(newToken.sessionId()),
                newToken.familyId(),
                newToken.generation(),
                newToken.tokenHash(),
                oldToken.rotatedAt(),
                newToken.expiresAt(),
                "refresh");
        current.rotate(nextId, oldToken.rotatedAt(), "refresh");
        entityManager.persist(replacement);
    }

    @Override
    public void compromiseFamily(String familyId, Instant detectedAt) {
        List<RefreshTokenEntity> family = entityManager.createQuery("""
                        select t from RefreshTokenEntity t where t.familyId = :familyId
                        """, RefreshTokenEntity.class)
                .setParameter("familyId", familyId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (family.isEmpty()) {
            return;
        }
        for (RefreshTokenEntity token : family) {
            if (token.getStatus() == RefreshTokenEntity.Status.ROTATED) {
                token.markReused(detectedAt, "refresh-replay");
            } else {
                token.revoke(detectedAt, "refresh-replay");
            }
        }
        RefreshTokenEntity evidence = family.getFirst();
        sessionRepository.lockByTenantIdAndSessionId(
                        evidence.getTenantId(), evidence.getSessionId())
                .ifPresent(session -> session.compromise(detectedAt, "refresh-replay"));
    }

    private RefreshTokenEntity findByHash(String tokenHash, LockModeType lockMode) {
        return entityManager.createQuery(
                        "select t from RefreshTokenEntity t where t.tokenHash = :tokenHash",
                        RefreshTokenEntity.class)
                .setParameter("tokenHash", tokenHash)
                .setLockMode(lockMode)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private static RefreshTokenService.TokenRecord toRecord(RefreshTokenEntity entity) {
        return new RefreshTokenService.TokenRecord(
                entity.getId().toString(),
                entity.getTenantId().toString(),
                entity.getSessionId().toString(),
                entity.getFamilyId(),
                entity.getGeneration(),
                entity.getTokenHash(),
                RefreshTokenService.TokenStatus.valueOf(entity.getStatus().name()),
                entity.getExpiresAt(),
                null);
    }
}
