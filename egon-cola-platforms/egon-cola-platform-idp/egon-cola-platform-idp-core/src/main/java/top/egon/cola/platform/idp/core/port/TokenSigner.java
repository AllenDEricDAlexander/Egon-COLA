package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.token.AccessTokenClaims;
import top.egon.cola.platform.idp.core.token.RefreshTokenClaims;
import top.egon.cola.platform.idp.core.token.ServiceAccessTokenClaims;

/**
 * IdP USER、SERVICE 和内部 Refresh Token 的签名与验证端口。
 *
 * <p>Signing and verification port for IdP USER, SERVICE, and internal refresh tokens.</p>
 */
public interface TokenSigner {

    /**
     * 签发 USER Access Token。
     *
     * <p>Signs a USER access token.</p>
     *
     * @param claims USER Token 声明；USER token claims
     * @return 紧凑 JWT；compact JWT
     */
    String signAccess(AccessTokenClaims claims);

    /**
     * 签发 SERVICE Access Token。
     *
     * <p>Signs a SERVICE access token.</p>
     *
     * @param claims SERVICE Token 声明；SERVICE token claims
     * @return 紧凑 JWT；compact JWT
     */
    String signServiceAccess(ServiceAccessTokenClaims claims);

    /**
     * 签发内部 Refresh Token。
     *
     * <p>Signs an internal refresh token.</p>
     *
     * @param claims Refresh Token 声明；refresh-token claims
     * @return 紧凑 JWT；compact JWT
     */
    String signRefresh(RefreshTokenClaims claims);

    /**
     * 验证并解析内部 Refresh Token。
     *
     * <p>Verifies and parses an internal refresh token.</p>
     *
     * @param rawRefreshToken 原始 Refresh Token；raw refresh token
     * @return 已验证声明；verified claims
     */
    RefreshTokenClaims verifyRefresh(String rawRefreshToken);
}
