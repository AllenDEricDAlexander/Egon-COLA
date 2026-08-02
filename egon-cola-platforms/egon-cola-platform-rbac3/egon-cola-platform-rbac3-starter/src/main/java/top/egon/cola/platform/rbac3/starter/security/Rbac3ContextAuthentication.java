package top.egon.cola.platform.rbac3.starter.security;

import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;

/** Authentication contract retaining the bound RBAC3 runtime context. */
public interface Rbac3ContextAuthentication {

    AuthorizationService.RuntimeAuthorizationContext context();
}
