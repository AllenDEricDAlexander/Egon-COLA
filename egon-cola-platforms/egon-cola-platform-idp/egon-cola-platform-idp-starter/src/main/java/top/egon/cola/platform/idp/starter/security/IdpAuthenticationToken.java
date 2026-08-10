package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.List;
import java.util.Objects;

/**
 * 表示已通过 IdP 校验的 Spring Security 身份凭据。
 * 该对象只携带 {@link IdentityPrincipal}，不保存原始访问令牌，也不附带业务权限。
 *
 * <p>Represents a Spring Security authentication established by IdP verification. It carries only
 * the {@link IdentityPrincipal}; it does not retain the raw access token or attach business
 * authorities.</p>
 */
public final class IdpAuthenticationToken
        extends AbstractAuthenticationToken {

    /**
     * 已验证的统一身份主体。
     *
     * <p>Validated unified identity principal.</p>
     */
    private final IdentityPrincipal principal;

    /**
     * 创建已经完成身份验证的认证对象。
     *
     * <p>Creates an authentication object whose identity has already been verified.</p>
     *
     * @param principal 已验证的身份主体；validated identity principal
     */
    public IdpAuthenticationToken(IdentityPrincipal principal) {
        super(List.of());
        this.principal = Objects.requireNonNull(principal, "principal");
        setAuthenticated(true);
    }

    /**
     * 返回空凭据，避免在安全上下文中保留原始 Bearer 令牌。
     *
     * <p>Returns an empty credential so the raw Bearer token is not retained in the security
     * context.</p>
     *
     * @return 空字符串；an empty string
     */
    @Override
    public Object getCredentials() {
        return "";
    }

    /**
     * 返回已验证的统一身份主体。
     *
     * <p>Returns the validated unified identity principal.</p>
     *
     * @return 身份主体；identity principal
     */
    @Override
    public IdentityPrincipal getPrincipal() {
        return principal;
    }
}
