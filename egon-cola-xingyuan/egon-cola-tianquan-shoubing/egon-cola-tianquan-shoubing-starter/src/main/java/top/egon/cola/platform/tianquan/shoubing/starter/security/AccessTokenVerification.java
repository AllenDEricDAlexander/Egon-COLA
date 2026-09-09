package top.egon.cola.platform.tianquan.shoubing.starter.security;

import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;

/**
 * Explicit outcome of access-token verification.
 */
public sealed interface AccessTokenVerification<T extends IdpPrincipal>
        permits AccessTokenVerification.Valid,
        AccessTokenVerification.Expired,
        AccessTokenVerification.Invalid {

    record Valid<T extends IdpPrincipal>(T principal)
            implements AccessTokenVerification<T> {
    }

    record Expired<T extends IdpPrincipal>()
            implements AccessTokenVerification<T> {
    }

    record Invalid<T extends IdpPrincipal>(String reasonCode)
            implements AccessTokenVerification<T> {
    }
}
