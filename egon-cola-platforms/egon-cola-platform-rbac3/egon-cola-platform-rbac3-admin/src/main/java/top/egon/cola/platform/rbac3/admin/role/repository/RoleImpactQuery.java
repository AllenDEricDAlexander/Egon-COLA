package top.egon.cola.platform.rbac3.admin.role.repository;

import top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleImpactVO;

/**
 * 读取角色影响的查询边界。
 * Query boundary for reading role impact.
 */
@FunctionalInterface
public interface RoleImpactQuery {

    /**
     * 读取指定角色的影响摘要。
     * Loads the impact summary for one role.
     *
     * @param tenantId 租户标识；tenant identifier
     * @param roleId 角色标识；role identifier
     * @return 角色影响；role impact
     */
    RoleImpactVO impact(String tenantId, String roleId);
}
