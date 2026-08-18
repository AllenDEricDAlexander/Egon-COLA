package top.egon.cola.platform.rbac3.starter.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Reads the current RBAC3 USER details from the request-bound SecurityContext.
 */
public final class CurrentRbac3User {

    public Optional<Rbac3UserDetails> current() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof Rbac3UserDetails details
                ? Optional.of(details)
                : Optional.empty();
    }

    public Rbac3UserDetails require() {
        return current().orElseThrow(() ->
                new AuthenticationCredentialsNotFoundException(
                        "RBAC3 USER details are required"));
    }
}
