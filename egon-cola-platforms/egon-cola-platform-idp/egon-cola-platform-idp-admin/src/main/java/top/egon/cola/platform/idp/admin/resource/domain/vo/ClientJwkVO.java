package top.egon.cola.platform.idp.admin.resource.domain.vo;

import java.time.Instant;

/**
 * Client 公开 JWK 视图，不返回私钥材料。
 *
 * <p>Client public-JWK view that never returns private key material.</p>
 *
 * @param kid JWK kid；JWK kid
 * @param algorithm 签名算法；signature algorithm
 * @param status 凭证状态；credential status
 * @param validFrom 生效时间；valid-from instant
 * @param validTo 失效时间；valid-to instant
 * @param lastUsedAt 最近使用时间；last-used instant
 * @param version 乐观锁版本；optimistic-lock version
 */
public record ClientJwkVO(
        String kid,
        String algorithm,
        String status,
        Instant validFrom,
        Instant validTo,
        Instant lastUsedAt,
        long version
) {
}
