package top.egon.cola.platform.rbac3.admin.auth.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.StepUpIdentityVO;
import top.egon.cola.platform.rbac3.admin.auth.repository.IdentityRepository;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

/** 二次认证身份的 JPA 查询适配器。 JPA query adapter for step-up identity. */
@Repository
public class JpaStepUpIdentityRepository implements IdentityRepository {

    /** JPA 实体管理器。 JPA entity manager. */
    private final EntityManager entityManager;

    /**
     * 创建二次认证身份查询适配器。 Creates the step-up identity query adapter.
     *
     * @param entityManager JPA 实体管理器；JPA entity manager
     */
    public JpaStepUpIdentityRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public StepUpIdentityVO load(String tenantId, String userId) {
        TenantPO tenant = entityManager.find(TenantPO.class, Long.valueOf(tenantId));
        UserPO user = entityManager.find(UserPO.class, Long.valueOf(userId));
        if (tenant == null || user == null
                || !Long.valueOf(tenantId).equals(user.getTenantId())) {
            throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
        }
        return new StepUpIdentityVO(tenant.getCode(), user.getUsername());
    }
}
