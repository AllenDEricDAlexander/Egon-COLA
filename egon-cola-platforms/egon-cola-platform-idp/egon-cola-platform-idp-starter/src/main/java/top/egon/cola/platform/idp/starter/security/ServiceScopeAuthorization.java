package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.core.Authentication;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;

/**
 * 对已验证 SERVICE Principal 执行本地 IdP Scope 判断。
 * 本类只读取 Token 中由 IdP 签名授权的 Scope，不调用 RBAC3 或远程服务。
 *
 * <p>Performs local IdP scope decisions for a verified SERVICE principal. It reads only scopes
 * authorized and signed by IdP and never calls RBAC3 or a remote service.</p>
 */
public final class ServiceScopeAuthorization {

    /** 创建无状态 Scope 判断器；Creates the stateless scope evaluator. */
    public ServiceScopeAuthorization() {
    }

    /**
     * 判断当前认证是否为包含所需 Scope 的 SERVICE 身份。
     *
     * <p>Determines whether the current authentication is a SERVICE identity containing the
     * required scope.</p>
     *
     * @param authentication 当前 Spring Security 认证；current Spring Security authentication
     * @param requiredScope 操作要求的 Scope；scope required by the operation
     * @return 仅当 SERVICE 身份携带精确 Scope 时为 {@code true}；{@code true} only when the
     * SERVICE identity carries the exact scope
     */
    public boolean hasScope(
            Authentication authentication,
            String requiredScope
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || requiredScope == null
                || requiredScope.isBlank()
                || !(authentication.getPrincipal()
                instanceof ServiceIdentityPrincipal service)) {
            return false;
        }
        return service.scopes().contains(requiredScope.trim());
    }
}
