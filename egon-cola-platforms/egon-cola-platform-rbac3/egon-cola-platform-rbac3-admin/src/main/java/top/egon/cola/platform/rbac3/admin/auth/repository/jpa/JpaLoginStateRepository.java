package top.egon.cola.platform.rbac3.admin.auth.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.LoginStateVO;
import top.egon.cola.platform.rbac3.admin.auth.repository.LoginStateRepository;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.Locale;

/** 登录状态的 JPA 查询适配器。 JPA query adapter for login state. */
@Repository
public class JpaLoginStateRepository implements LoginStateRepository {

    /** JPA 实体管理器。 JPA entity manager. */
    private final EntityManager entityManager;
    /** 角色激活候选服务。 Role-activation candidate service. */
    private final RoleActivationCandidateService candidateService;

    /**
     * 创建登录状态查询适配器。 Creates the login-state query adapter.
     *
     * @param entityManager JPA 实体管理器；JPA entity manager
     * @param candidateService 角色激活候选服务；role-activation candidate service
     */
    public JpaLoginStateRepository(
            EntityManager entityManager,
            RoleActivationCandidateService candidateService) {
        this.entityManager = entityManager;
        this.candidateService = candidateService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public LoginStateVO load(String tenantCode, String userId, Instant now) {
        TenantPO tenant = entityManager.createQuery("""
                        select t from TenantEntity t where lower(t.code) = :code
                        """, TenantPO.class)
                .setParameter("code", tenantCode.toLowerCase(Locale.ROOT))
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new Rbac3RuleViolation("AUTHENTICATION_FAILED"));
        UserPO user = requireUser(tenant.getId(), Long.valueOf(userId));
        int candidates = candidateService.candidates(
                        tenant.getId().toString(), userId, now)
                .applications().stream()
                .mapToInt(application -> application.candidates().size())
                .sum();
        return new LoginStateVO(
                tenant.getId().toString(), user.getAuthVersion(),
                tenant.getPolicyVersion(), candidates);
    }

    /** 按租户校验并读取用户。 Loads a user after enforcing tenant ownership. */
    private UserPO requireUser(Long tenantId, Long userId) {
        UserPO user = entityManager.find(UserPO.class, userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return user;
    }
}
