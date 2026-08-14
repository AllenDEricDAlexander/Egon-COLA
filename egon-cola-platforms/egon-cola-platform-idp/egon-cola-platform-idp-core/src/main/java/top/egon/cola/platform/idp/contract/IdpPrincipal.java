package top.egon.cola.platform.idp.contract;

import java.time.Instant;

/**
 * USER 和 SERVICE Access Token 共用的最小身份契约。
 *
 * <p>Minimal identity contract shared by USER and SERVICE Access Tokens.</p>
 */
public sealed interface IdpPrincipal
        permits IdentityPrincipal, ServiceIdentityPrincipal {

    /**
     * 返回主体类型。
     *
     * <p>Returns the principal type.</p>
     *
     * @return USER 或 SERVICE；USER or SERVICE
     */
    PrincipalType principalType();

    /**
     * 返回主体标识。
     *
     * <p>Returns the subject identifier.</p>
     *
     * @return 主体标识；subject identifier
     */
    String subject();

    /**
     * 返回租户标识。
     *
     * <p>Returns the tenant identifier.</p>
     *
     * @return 租户标识；tenant identifier
     */
    String tenantId();

    /**
     * 返回 Access Token jti。
     *
     * <p>Returns the Access Token jti.</p>
     *
     * @return Token 标识；Token identifier
     */
    String tokenId();

    /**
     * 返回签发时间。
     *
     * <p>Returns the issued-at instant.</p>
     *
     * @return 签发时间；issued-at instant
     */
    Instant issuedAt();

    /**
     * 返回过期时间。
     *
     * <p>Returns the expiration instant.</p>
     *
     * @return 过期时间；expiration instant
     */
    Instant expiresAt();
}
