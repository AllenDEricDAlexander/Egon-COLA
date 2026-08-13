package top.egon.cola.platform.rbac3.admin.auth.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.StepUpResultVO;
import top.egon.cola.platform.rbac3.admin.auth.repository.SessionStrengthRepository;
import top.egon.cola.platform.rbac3.admin.session.domain.po.SessionPO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;

/** 二次认证会话强度的 JPA 写适配器。 JPA write adapter for step-up session strength. */
@Repository
public class JpaStepUpSessionStrengthRepository implements SessionStrengthRepository {

    /** JPA 实体管理器。 JPA entity manager. */
    private final EntityManager entityManager;

    /**
     * 创建二次认证会话写适配器。 Creates the step-up session write adapter.
     *
     * @param entityManager JPA 实体管理器；JPA entity manager
     */
    public JpaStepUpSessionStrengthRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public StepUpResultVO strengthen(
            String tenantId,
            String userId,
            String sessionId,
            Instant now) {
        List<SessionPO> sessions = entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId
                           and s.userId = :userId
                           and s.sessionId = :sessionId
                        """, SessionPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("userId", Long.valueOf(userId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (sessions.size() != 1) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        SessionPO session = sessions.getFirst();
        session.stepUp(userId, now);
        return new StepUpResultVO(
                sessionId, session.getAuthenticationStrength().name(),
                session.getStrongAuthenticatedAt());
    }
}
