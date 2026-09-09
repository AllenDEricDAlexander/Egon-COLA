package top.egon.cola.platform.tianquan.shoubing.admin.support.security;

import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

@FunctionalInterface
public interface IdpAdminAuthorizationPort {

    void require(IdentityPrincipal principal, String permission);
}
