package top.egon.cola.platform.rbac3.admin.assignment.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.assignment.repository.AssignmentSessionStrengthRepository;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.AuthenticationStrengthEnum;
import top.egon.cola.platform.rbac3.admin.session.domain.po.SessionPO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** 分配操作所需会话强度的 JPA 查询适配器。 JPA query adapter for assignment session strength. */
@Repository
public class JpaAssignmentSessionStrengthRepository
        implements AssignmentSessionStrengthRepository {

    /** 强认证有效窗口。 Strong-authentication validity window. */
    private static final Duration STRONG_AUTHENTICATION_WINDOW = Duration.ofMinutes(10);
    /** JPA 实体管理器。 JPA entity manager. */
    private final EntityManager entityManager;

    /**
     * 创建分配会话强度查询适配器。 Creates the assignment session-strength adapter.
     *
     * @param entityManager JPA 实体管理器；JPA entity manager
     */
    public JpaAssignmentSessionStrengthRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public String authenticationStrength(
            String tenantId,
            String sessionId,
            Instant now) {
        List<SessionPO> rows = entityManager.createQuery("""
                        select s from SessionEntity s
                         where s.tenantId = :tenantId and s.sessionId = :sessionId
                        """, SessionPO.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("sessionId", Long.valueOf(sessionId))
                .getResultList();
        if (rows.size() != 1) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        SessionPO session = rows.getFirst();
        if (session.getAuthenticationStrength() == AuthenticationStrengthEnum.STRONG
                && !session.isStrongAuthenticationRecent(now, STRONG_AUTHENTICATION_WINDOW)) {
            return AuthenticationStrengthEnum.PASSWORD.name();
        }
        return session.getAuthenticationStrength().name();
    }
}
