package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求当前主体是携带指定 IdP Scope 的 SERVICE Principal。
 *
 * <p>Requires the current principal to be a SERVICE principal carrying the specified IdP scope.</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@serviceScopeAuthorization.hasScope(authentication, '{value}')")
public @interface RequiresServiceScope {

    /**
     * 返回当前操作要求的 IdP 服务 Scope。
     *
     * <p>Returns the IdP service scope required by the current operation.</p>
     *
     * @return 服务 Scope；service scope
     */
    String value();
}
