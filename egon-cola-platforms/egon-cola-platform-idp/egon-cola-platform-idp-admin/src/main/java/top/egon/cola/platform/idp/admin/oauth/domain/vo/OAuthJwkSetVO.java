package top.egon.cola.platform.idp.admin.oauth.domain.vo;

import java.util.List;

/** Typed public JWK Set response without private key material. */
public record OAuthJwkSetVO(List<Jwk> keys) {

    /** Public RSA JWK fields emitted by the IdP. */
    public record Jwk(
            String kty,
            String e,
            String kid,
            String n,
            String alg,
            String use) {
    }
}
