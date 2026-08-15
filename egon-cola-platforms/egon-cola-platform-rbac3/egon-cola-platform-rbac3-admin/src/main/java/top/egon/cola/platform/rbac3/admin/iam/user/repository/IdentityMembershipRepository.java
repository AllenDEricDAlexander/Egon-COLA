package top.egon.cola.platform.rbac3.admin.iam.user.repository;

import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.TenantMembershipVO;

import java.util.List;
import java.util.Optional;

/**
 * Reads RBAC membership directly from the minimal {@code rbac3_user} projection.
 */
public interface IdentityMembershipRepository {

    Optional<TenantMembershipVO> resolve(String tenantId, String identitySub);

    List<TenantMembershipVO> tenants(String identitySub);
}
