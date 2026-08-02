package top.egon.cola.platform.rbac3.admin.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Restricts trusted internal endpoints to an authorized service principal. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("authentication.principal instanceof "
        + "T(top.egon.cola.platform.rbac3.admin.security.CurrentRbac3ServicePrincipal) "
        + "and @rbac3MethodAuthorization.hasPermission(authentication, '{permission}')")
public @interface RequiresRbac3ServicePermission {

    String permission();
}
