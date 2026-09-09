package top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.service;

/**
 * Creates the RBAC membership for the first platform administrator.
 */
@FunctionalInterface
public interface PlatformAdminBootstrapService {

    void bootstrap(String tenantId, String identitySub);
}
