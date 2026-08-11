package top.egon.cola.platform.rbac3.admin.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("rbac3MethodAuthorization")
public final class Rbac3MethodAuthorization {

    public boolean hasPermission(
            Authentication authentication,
            String requiredPermission) {
        if (authentication == null) {
            return false;
        }
        return authentication.getPrincipal() instanceof CurrentRbac3Principal principal
                && principal.hasPermission(requiredPermission);
    }
}
