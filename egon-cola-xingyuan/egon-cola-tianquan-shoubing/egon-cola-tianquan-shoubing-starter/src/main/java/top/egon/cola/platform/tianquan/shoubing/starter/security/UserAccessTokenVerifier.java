package top.egon.cola.platform.tianquan.shoubing.starter.security;

import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.util.Objects;

/**
 * Explicit USER verifier facade used by endpoint policies.
 */
public final class UserAccessTokenVerifier {

    private final IdpJwtVerifier verifier;

    public UserAccessTokenVerifier(IdpJwtVerifier verifier) {
        this.verifier = Objects.requireNonNull(verifier, "verifier");
    }

    public AccessTokenVerification<IdentityPrincipal> verify(String token) {
        return verifier.verifyUser(token);
    }
}
