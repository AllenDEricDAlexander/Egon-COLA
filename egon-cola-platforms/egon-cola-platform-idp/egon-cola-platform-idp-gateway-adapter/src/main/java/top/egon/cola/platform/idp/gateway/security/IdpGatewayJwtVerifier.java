package top.egon.cola.platform.idp.gateway.security;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;

import java.util.Objects;

/**
 * Adapts the shared IdP verifier to the non-servlet Gateway security SPI.
 */
public final class IdpGatewayJwtVerifier
        implements IdpIdentityAuthenticationProvider.TokenVerifier {

    private final IdpJwtVerifier delegate;

    public IdpGatewayJwtVerifier(IdpJwtVerifier delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public IdentityPrincipal verify(String token) {
        return delegate.verify(token);
    }
}
