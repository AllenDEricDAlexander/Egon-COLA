package top.egon.cola.platform.rbac3.admin.auth.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.RefreshStateVO;
import top.egon.cola.platform.rbac3.admin.auth.repository.RefreshStateRepository;
import top.egon.cola.platform.rbac3.admin.session.domain.po.RefreshTokenPO;
import top.egon.cola.platform.rbac3.admin.session.domain.po.SessionPO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.List;

/** 刷新令牌状态的 JPA 查询适配器。 JPA query adapter for refresh-token state. */
@Repository
public class JpaRefreshStateRepository implements RefreshStateRepository {

    /** JPA 实体管理器。 JPA entity manager. */
    private final EntityManager entityManager;

    /**
     * 创建刷新状态查询适配器。 Creates the refresh-state query adapter.
     *
     * @param entityManager JPA 实体管理器；JPA entity manager
     */
    public JpaRefreshStateRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public RefreshStateVO load(String familyId) {
        List<Object[]> rows = entityManager.createQuery("""
                        select token, session
                          from RefreshTokenEntity token, SessionEntity session
                         where token.familyId = :familyId
                           and session.tenantId = token.tenantId
                           and session.sessionId = token.sessionId
                         order by token.generation desc
                        """, Object[].class)
                .setParameter("familyId", familyId)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            throw new Rbac3RuleViolation("AUTHENTICATION_FAILED");
        }
        RefreshTokenPO token = (RefreshTokenPO) rows.getFirst()[0];
        SessionPO session = (SessionPO) rows.getFirst()[1];
        return new RefreshStateVO(
                session.getTenantId().toString(), session.getUserId().toString(),
                session.getSessionId().toString(), session.getAuthVersionAtIssue(),
                session.getSessionVersion(), session.getPolicyVersionAtIssue(),
                token.getExpiresAt(), session.isActivationRequired(),
                session.isActivationRequired() ? "ROLE_ACTIVATION_REQUIRED" : null);
    }
}
