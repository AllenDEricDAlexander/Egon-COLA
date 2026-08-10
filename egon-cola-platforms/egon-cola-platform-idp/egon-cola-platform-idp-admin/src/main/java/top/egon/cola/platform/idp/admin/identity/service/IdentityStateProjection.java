package top.egon.cola.platform.idp.admin.identity.service;

import top.egon.cola.platform.idp.contract.IdentityUserState;

/**
 * 接收需要写入运行时存储的身份用户状态。
 *
 * <p>Receives identity-user state that must be projected into runtime storage.</p>
 */
@FunctionalInterface
public interface IdentityStateProjection {

    void project(IdentityUserState state);
}
