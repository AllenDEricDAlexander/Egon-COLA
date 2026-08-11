package top.egon.cola.platform.idp.gateway.security;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.platform.idp.contract.IdpPrincipal;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityUserStateReader;

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
    private final JwtDecoder decoder;
    private final IdentityUserStateReader userStates;
    private final IdentityResourceServerStateReader resourceStates;
    private final IdentityOAuthClientStateReader clientStates;
    private final GatewayResourceServerResolver resources;

    /**
     * 创建 Gateway 验证端口适配器。
     *
     * <p>Creates the Gateway verification-port adapter.</p>
     *
     * @param decoder JWT 解码器；JWT decoder
     * @param userStates 用户状态读取器；user-state reader
     * @param resourceStates Resource 状态读取器；Resource-state reader
     * @param clientStates OAuth Client 状态读取器；OAuth Client-state reader
     * @param resources 路由 Resource 解析器；route Resource resolver
     */
    public IdpGatewayJwtVerifier(
            JwtDecoder decoder,
            IdentityUserStateReader userStates,
            IdentityResourceServerStateReader resourceStates,
            IdentityOAuthClientStateReader clientStates,
            GatewayResourceServerResolver resources
    ) {
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.userStates = Objects.requireNonNull(userStates, "userStates");
        this.resourceStates = Objects.requireNonNull(resourceStates, "resourceStates");
        this.clientStates = Objects.requireNonNull(clientStates, "clientStates");
        this.resources = Objects.requireNonNull(resources, "resources");
    }

    /**
     * 委托共享验证器校验访问令牌。
     *
     * <p>Delegates access-token validation to the shared verifier.</p>
     *
     * @param context 可信 Gateway 路由上下文；trusted Gateway route context
     * @param token 原始 Bearer 访问令牌；raw Bearer access token
     * @return 已验证的统一身份主体；validated unified identity principal
     * @throws IdpJwtVerifier.InvalidTokenException 当令牌或实时用户状态未通过校验时；when the
     *                                              token or current user state fails validation
     */
    @Override
    public IdpPrincipal verify(GatewayAuthContext context, String token) {
        try {
            IdentityResourceServerState resource = resources.resolve(
                    context.attributes());
            return new IdpJwtVerifier(
                    decoder, userStates, resourceStates, clientStates,
                    resource.resourceServerId(), resource.resourceUri())
                    .verify(token);
        } catch (IdpJwtVerifier.InvalidTokenException invalid) {
            String reason = "JWT_AUDIENCE_INVALID".equals(invalid.getMessage())
                    ? "IDP_RESOURCE_AUDIENCE_MISMATCH"
                    : "IDP_" + invalid.getMessage();
            throw new TokenVerificationException(reason, invalid);
        } catch (GatewayResourceServerResolver.ResourceResolutionException invalid) {
            throw new TokenVerificationException(invalid.getMessage(), invalid);
        }
    }

    /** Gateway Token 验证失败；Gateway token-verification failure. */
    public static final class TokenVerificationException extends RuntimeException {
        /** 使用稳定原因码创建异常；Creates an exception with a stable reason code. */
        public TokenVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
