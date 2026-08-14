package top.egon.cola.platform.idp.starter.security;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;

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
