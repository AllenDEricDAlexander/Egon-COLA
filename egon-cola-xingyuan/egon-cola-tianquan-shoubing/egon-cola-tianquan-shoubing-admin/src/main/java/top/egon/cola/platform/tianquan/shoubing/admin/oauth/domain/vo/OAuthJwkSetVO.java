package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo;

import java.util.List;

/** Typed public JWK Set response without private key material. */
public record OAuthJwkSetVO(List<Jwk> keys) {

    /** Public RSA JWK fields emitted by the Tianquan-Shoubing. */
    public record Jwk(
            String kty,
            String e,
            String kid,
            String n,
            String alg,
            String use) {
    }
}
