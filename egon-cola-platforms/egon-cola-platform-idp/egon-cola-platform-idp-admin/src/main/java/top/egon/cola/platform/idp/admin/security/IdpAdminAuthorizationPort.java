package top.egon.cola.platform.idp.admin.security;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;

@FunctionalInterface
public interface IdpAdminAuthorizationPort {

    void require(IdentityPrincipal principal, String permission);
}
