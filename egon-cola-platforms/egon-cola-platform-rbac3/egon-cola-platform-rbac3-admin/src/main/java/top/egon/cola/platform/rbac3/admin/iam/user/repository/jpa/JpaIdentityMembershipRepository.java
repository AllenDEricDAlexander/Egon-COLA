package top.egon.cola.platform.rbac3.admin.iam.user.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.TenantMembershipVO;
import top.egon.cola.platform.rbac3.admin.iam.user.repository.IdentityMembershipRepository;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.enums.TenantStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.po.TenantPO;

import java.util.List;
import java.util.Optional;

/**
 * JPA membership reader backed by the direct {@code UserPO.identitySub} projection.
 */
@Repository
public class JpaIdentityMembershipRepository implements IdentityMembershipRepository {

    private final EntityManager entityManager;

    public JpaIdentityMembershipRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantMembershipVO> resolve(String tenantId, String identitySub) {
        return activeRows(identitySub, Long.valueOf(tenantId)).stream()
                .findFirst()
                .map(row -> new TenantMembershipVO(
                        row.tenant().getId().toString(),
                        row.tenant().getCode(),
                        row.tenant().getName(),
                        row.user().getId().toString(),
                        row.tenant().getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantMembershipVO> tenants(String identitySub) {
        return activeRows(identitySub, null).stream()
                .map(row -> new TenantMembershipVO(
                        row.tenant().getId().toString(),
                        row.tenant().getCode(),
                        row.tenant().getName(),
                        row.user().getId().toString(),
                        row.tenant().getName()))
                .toList();
    }

    private List<MembershipRow> activeRows(String identitySub, Long tenantId) {
        String tenantPredicate = tenantId == null ? "" : " and tenant.id = :tenantId";
        var query = entityManager.createQuery("""
                        select tenant, user
                          from TenantEntity tenant, UserEntity user
                         where user.identitySub = :identitySub
                           and user.tenantId = tenant.id
                           and user.status = :userStatus
                           and tenant.status = :tenantStatus
                        """ + tenantPredicate + " order by tenant.id", Object[].class)
                .setParameter("identitySub", identitySub)
                .setParameter("userStatus", UserStatusEnum.ACTIVE)
                .setParameter("tenantStatus", TenantStatusEnum.ACTIVE);
        if (tenantId != null) {
            query.setParameter("tenantId", tenantId);
        }
        return query.getResultList().stream()
                .map(values -> new MembershipRow(
                        (TenantPO) values[0], (UserPO) values[1]))
                .toList();
    }

}
