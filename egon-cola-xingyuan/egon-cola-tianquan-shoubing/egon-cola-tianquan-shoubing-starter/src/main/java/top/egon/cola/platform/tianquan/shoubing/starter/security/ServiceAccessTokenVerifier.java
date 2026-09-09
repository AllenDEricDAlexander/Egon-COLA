package top.egon.cola.platform.tianquan.shoubing.starter.security;

import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;

import java.util.Objects;

/**
 * Explicit SERVICE verifier facade used by internal endpoint policies.
 */
public final class ServiceAccessTokenVerifier {

    private final IdpJwtVerifier verifier;

    public ServiceAccessTokenVerifier(IdpJwtVerifier verifier) {
        this.verifier = Objects.requireNonNull(verifier, "verifier");
    }

    public AccessTokenVerification<ServiceIdentityPrincipal> verify(String token) {
        return verifier.verifyService(token);
    }
}
