package top.egon.cola.platform.rbac3.admin.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("rbac3MethodAuthorization")
public final class Rbac3MethodAuthorization {

    public boolean hasPermission(Authentication authentication, String permission) {
        if (authentication == null) {
            return false;
        }
        return switch (authentication.getPrincipal()) {
            case CurrentRbac3Principal principal -> principal.hasPermission(permission);
            case CurrentRbac3ServicePrincipal principal -> principal.hasPermission(permission);
            default -> false;
        };
    }
}
