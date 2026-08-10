package top.egon.cola.platform.idp.admin.support.security;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;

@FunctionalInterface
public interface IdpAdminAuthorizationPort {

    void require(IdentityPrincipal principal, String permission);
}
