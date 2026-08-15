package top.egon.cola.platform.rbac3.admin.iam.tenant.repository;

import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.po.TenantPO;

import java.util.Optional;

/**
 * 按租户编码查询租户的仓储契约。
 * Repository contract for looking up a tenant by tenant code.
 */
public interface TenantLookupRepository {
    Optional<TenantPO> findTenantByCode(String tenantCode);
}
