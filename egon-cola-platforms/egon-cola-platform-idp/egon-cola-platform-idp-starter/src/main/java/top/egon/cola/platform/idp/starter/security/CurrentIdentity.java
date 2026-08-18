package top.egon.cola.platform.idp.starter.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.Optional;

/**
 * Reads the current verified USER identity from the request-bound SecurityContext.
 *
 * <p>This accessor deliberately does not cache or synthesize an identity. SERVICE principals and
 * anonymous requests are not accepted as USER identities.</p>
 */
public final class CurrentIdentity {

    /**
     * Returns the current verified USER identity when the SecurityContext contains one.
     *
     * @return current USER identity, or empty for anonymous/SERVICE requests
     */
    public Optional<IdentityPrincipal> current() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof IdentityPrincipal user
                ? Optional.of(user)
                : Optional.empty();
    }

    /**
     * Requires a verified USER identity for the current request.
     *
     * @return current USER identity
     * @throws AuthenticationCredentialsNotFoundException when no USER identity exists
     */
    public IdentityPrincipal require() {
        return current().orElseThrow(() ->
                new AuthenticationCredentialsNotFoundException(
                        "verified USER identity is required"));
    }
}
