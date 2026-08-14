package top.egon.cola.platform.rbac3.admin.bootstrap.repository;

/**
 * 持久化首个平台安全管理员所需的完整初始化数据。
 * Persists the complete initialization data for the first platform security administrator.
 */
@FunctionalInterface
public interface PlatformAdminBootstrapRepository {

    /**
     * 在单个事务边界内初始化平台管理员及其内置权限。
     * Bootstraps the platform administrator and built-in permissions within one transaction boundary.
     *
     * @param tenantCode 租户编码；tenant code
     * @param identitySub IdP 主体标识；IdP subject
     */
    void bootstrap(String tenantCode, String identitySub);
}
