package top.egon.cola.platform.idp.admin.identity.service;

/**
 * 将持久化用户状态恢复到身份运行时投影。
 *
 * <p>Restores persistent identity-user state into the runtime projection.</p>
 */
public interface IdentityUserStateService {

    int reconcile();
}
