package top.egon.cola.platform.idp.gateway.security;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;

import java.util.Objects;

/**
 * 把共享的 IdP JWT 验证器适配为非 Servlet Gateway 的令牌验证端口。
 * 该适配器不复制验证规则，Gateway 与普通资源服务器因此使用相同的身份校验语义。
 *
 * <p>Adapts the shared IdP JWT verifier to the non-Servlet Gateway token-verification port. It does
 * not duplicate verification rules, so the Gateway and regular resource servers share the same
 * identity-validation semantics.</p>
 */
public final class IdpGatewayJwtVerifier
        implements IdpIdentityAuthenticationProvider.TokenVerifier {

    /**
     * 共享 IdP JWT 与实时用户状态验证器。
     *
     * <p>Shared IdP JWT and current-user-state verifier.</p>
     */
    private final IdpJwtVerifier delegate;

    /**
     * 创建 Gateway 验证端口适配器。
     *
     * <p>Creates the Gateway verification-port adapter.</p>
     *
     * @param delegate 共享 IdP 验证器；shared IdP verifier
     */
    public IdpGatewayJwtVerifier(IdpJwtVerifier delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * 委托共享验证器校验访问令牌。
     *
     * <p>Delegates access-token validation to the shared verifier.</p>
     *
     * @param token 原始 Bearer 访问令牌；raw Bearer access token
     * @return 已验证的统一身份主体；validated unified identity principal
     * @throws IdpJwtVerifier.InvalidTokenException 当令牌或实时用户状态未通过校验时；when the
     *                                              token or current user state fails validation
     */
    @Override
    public IdentityPrincipal verify(String token) {
        return delegate.verify(token);
    }
}
